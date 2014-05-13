package com.typesafe.sbt.reactjs


import sbt._
import sbt.Keys._
import com.typesafe.jse._
import scala.collection.immutable
import com.typesafe.npm.NpmLoader
import com.typesafe.sbt.web.pipeline.Pipeline
import akka.util.Timeout
import scala.concurrent.{Future, ExecutionContext, Await}
import com.typesafe.sbt.web.{CompileProblems, incremental, SbtWeb}
import scala.concurrent.duration._
import com.typesafe.sbt.jse.{SbtJsEngine, SbtJsTask}
import com.typesafe.jse.Engine.JsExecutionResult
import akka.actor.ActorRef
import akka.pattern.ask
import play.core.jscompile.JavascriptCompiler.CompilationException
import scala.util.control.Exception._
import com.typesafe.sbt.web.incremental.OpSuccess
import spray.json._
import DefaultJsonProtocol._


object Import {

  val reactJs = TaskKey[Seq[File]]("reactjs", "Perform ReactJS JSX compilation on the asset pipeline.")

  object ReactJsKeys {

    val sourceMap = SettingKey[Boolean]("reactjs-source-map", "Outputs a v3 sourcemap.")
    val version = SettingKey[String]("reactjs-version", "The version of react to fetch")
    val options = SettingKey[Seq[String]]("reactjs-options")
    val timeout = SettingKey[FiniteDuration]("reactjs-timeout", "How long before timing out JS runtime.")
    val tools = TaskKey[File]("reactjs-tools", "Install the ReactJS jsx compiler")
    val extension = SettingKey[String]("reactjs-extension", "The reactjs extension")
  }

}

object SbtReactJs extends AutoPlugin {

  val autoImport = Import

  import autoImport.ReactJsKeys._
  import SbtWeb.autoImport._
  import WebKeys._
  import SbtJsEngine.autoImport.JsEngineKeys._
  import SbtJsTask.autoImport.JsTaskKeys._
  import autoImport._

  final val NODE_MODULES = "node_modules"
  final val JSX = NODE_MODULES + "/.bin/jsx"


  override def requires = SbtJsTask

  override def trigger = AllRequirements

  private def invokeJS(engine: ActorRef, npmFile: File, args: Seq[String])
                      (implicit timeout: Timeout): Future[JsExecutionResult] = {
    (engine ? Engine.ExecuteJs(npmFile, args.to[immutable.Seq], timeout.duration)).mapTo[JsExecutionResult]
  }

  private def addUnscopedJsSourceFileTasks(sourceFileTask: TaskKey[Seq[File]]): Seq[Setting[_]] = {
    Seq(
      resourceGenerators <+= sourceFileTask,
      managedResourceDirectories += (resourceManaged in sourceFileTask).value
    )
  }

  override def projectSettings = Seq(
    sourceMap := true,
    version := "0.10.0",
    extension := "jsx",
    options := Seq.empty[String],
    timeout := 2.minutes,
    excludeFilter in reactJs := HiddenFileFilter,
    includeFilter in reactJs := GlobFilter("*.jsx"),
    tools := toolsInstaller.value,
    taskMessage in Assets := "ReactJS compiling",
    taskMessage in TestAssets := "ReactJS test compiling",

  // Ripoff of the js source task from sbtjsengine
    reactJs in Assets := runCompiler(reactJs, Assets).dependsOn(webModules in Assets).dependsOn(tools).value,
    reactJs in TestAssets := runCompiler(reactJs, TestAssets).dependsOn(webModules in TestAssets).dependsOn(tools).value,
    resourceManaged in reactJs in Assets := webTarget.value / reactJs.key.label / "main",
    resourceManaged in reactJs in TestAssets := webTarget.value / reactJs.key.label / "test",
    reactJs := (reactJs in Assets).value
  ) ++
    inConfig(Assets)(addUnscopedJsSourceFileTasks(reactJs)) ++
    inConfig(TestAssets)(addUnscopedJsSourceFileTasks(reactJs))

  private def toolsInstaller: Def.Initialize[Task[File]] = Def.task {
    val modules = Seq(new File(NODE_MODULES)).map(_.getCanonicalPath)
    val result = new File(JSX)

    // TODO: May need to remove this later, just trying to speed things up for now.
    if (!result.exists()) {
      streams.value.log.info(s"Fetching react-tools@${version.value}")
      implicit val valTimeout = Timeout(timeout.value)

      val pendingExitValue = SbtWeb.withActorRefFactory(state.value, this.getClass.getName) {
        arf =>
          val to = new File(new File("target"), "webjars")
          val cacheFile = new File(to, "extraction-cache")
          val npmFile = NpmLoader.load(to, cacheFile, getClass.getClassLoader)
          val engineProps = SbtJsEngine.engineTypeToProps(
            (engineType in reactJs).value,
            Some(npmFile),
            LocalEngine.nodePathEnv(modules.to[immutable.Seq])
          )
          val engine = arf.actorOf(engineProps)
          import ExecutionContext.Implicits.global
          for (
            result <- invokeJS(engine, npmFile, Seq("install", s"react-tools@${version.value}"))
          ) yield {
            streams.value.log.info(s"Successfully installed react-tools@${version.value} via NPM.")
            //new String(result.output.toArray, "UTF-8").split("\n").foreach(s => logger.info(s))
            //new String(result.error.toArray, "UTF-8").split("\n").foreach(s => if (result.exitValue == 0) logger.info(s) else logger.error(s))
          }
      }
      Await.result(pendingExitValue, valTimeout.duration)
    }
    result
  }

  private def runCompiler(task: TaskKey[Seq[File]],
                          config: Configuration): Def.Initialize[Task[Seq[File]]] = Def.task {
    val nodeModulePaths = (nodeModuleDirectories in Plugin).value.map(_.getCanonicalPath)

    val sources = ((unmanagedSources in config).value ** ((includeFilter in task in config).value -- (excludeFilter in task in config).value)).get

    val sortedUnManagedDirs = sources.filter(_.isDirectory).sortWith {
      case (lhs, rhs) => lhs.getCanonicalPath.size < rhs.getCanonicalPath.size
    }

    val engineProps = SbtJsEngine.engineTypeToProps(
      EngineType.Node, // TODO Work with other than node, not sure how to yet.
      None,
      LocalEngine.nodePathEnv(nodeModulePaths.to[immutable.Seq])
    )

    val unManagedDirs = sortedUnManagedDirs.foldLeft(Seq.empty[File]) {
      (files, currentFile) =>
        if (files.count {
          file =>
            currentFile.relativeTo(file).nonEmpty
        } == 0) {
          files ++ Seq(currentFile)
        } else {
          files
        }
    }
    val logger: Logger = state.value.log

    streams.value.log.info(s"${(taskMessage in task in config).value} on ${
      unManagedDirs.size
    } source directories.")

    implicit val valTimeout = Timeout(timeout.value)
    import ExecutionContext.Implicits.global

    val pendingExitValue = unManagedDirs.map {
      dir =>
        SbtWeb.withActorRefFactory(state.value, this.getClass.getName) {
          arf =>
            val engine = arf.actorOf(engineProps)
            for (
              result <- invokeJS(engine, tools.value, Seq("--extension", extension.value, dir.getCanonicalPath, (resourceManaged in task in config).value.getCanonicalPath))
            ) yield {
              if (result.exitValue != 0) {
                throw new RuntimeException(s"""Compilation failed: ${new String(result.error.toArray, "UTF-8")}.""")
              }
              new String(result.output.toArray, "UTF-8")
            }
        }
    }
    val result = Await.result(Future.sequence(pendingExitValue), valTimeout.duration)
    val filesChanged = result.map{
      str => str.parseJson.convertTo[List[String]]
    }.flatMap {
      str => str.map(f => (resourceManaged in task in config).value / (f + ".js"))
    }
    filesChanged
  }

}
