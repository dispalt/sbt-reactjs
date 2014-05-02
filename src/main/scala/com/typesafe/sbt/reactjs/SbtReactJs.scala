package com.typesafe.sbt.reactjs


import sbt._
import sbt.Keys._
import com.typesafe.jse._
import scala.collection.immutable
import com.typesafe.npm.{NpmLoader, Npm}
import akka.util.Timeout
import scala.concurrent.{Future, ExecutionContext, Await}
import com.typesafe.jse.Node
import com.typesafe.sbt.web.SbtWeb
import scala.concurrent.duration._
import scala.util.Try
import com.typesafe.sbt.jse.{SbtJsEngine, SbtJsTask}
import spray.json._
import com.typesafe.sbt.jse.JsTaskImport.JsTaskKeys
import com.typesafe.jse.Engine.JsExecutionResult
import scala.collection.mutable.ListBuffer
import akka.actor.ActorRef
import akka.pattern.ask
import play.core.jscompile.JavascriptCompiler.CompilationException
import scala.util.control.Exception._


object Import {

  object ReactJsKeys {
    val reactJs = TaskKey[Unit]("reactjs", "Invoke the reactjs compiler.")
    val sourceMap = SettingKey[Boolean]("reactjs-source-map", "Outputs a v3 sourcemap.")
    val jsxPath = SettingKey[File]("reactjs-jsx-path", "The path to the jsx compiler")
    val reactVersion = SettingKey[String]("reactjs-version", "The version of react to fetch")
    val entryPoints = SettingKey[PathFinder]("reactjs-entry-points")
    val options = SettingKey[Seq[String]]("reactjs-options")
    val reactTimeout = SettingKey[FiniteDuration]("reactjs-timeout", "How long before timing out JS runtime.")
  }

}

object SbtReactJs extends AutoPlugin {

  val autoImport = Import

  import SbtWeb.autoImport._
  import WebKeys._
  import SbtJsTask.autoImport.JsTaskKeys._
  import autoImport.ReactJsKeys._
  import SbtJsEngine.autoImport._
  import JsEngineKeys._
  import autoImport._
  import JsTaskKeys._

  final val JSX = "node_modules/react-tools/bin/jsx"

  override def requires = SbtJsTask

  override def trigger = AllRequirements

  private def invokeJS(engine: ActorRef, npmFile: File, args: Seq[String])
                       (implicit timeout: Timeout): Future[JsExecutionResult] = {
    (engine ? Engine.ExecuteJs(npmFile, args.to[immutable.Seq], timeout.duration)).mapTo[JsExecutionResult]
  }

  val naming: (String, Boolean) => String = (name, min) => name.replace(".jsx", if (min) ".min.js" else ".js")

  def compile(file: File, state: State, path: File, timeout: FiniteDuration, options: Seq[String]): (String, Option[String], Seq[File]) = {
    implicit val timeo = Timeout(timeout)
    val pendingExitValue = SbtWeb.withActorRefFactory(state, this.getClass.getName) {
      arf =>
        val props = Trireme.props()
        val engine = arf.actorOf(props)
        import ExecutionContext.Implicits.global
        for (
          result <- invokeJS(engine, new File(JSX), Seq(file.getCanonicalPath))
        ) yield {
          // TODO: We need to stream the output and error channels. The js engine needs to change in this regard so that the
          // stdio sink and sources can be exposed through the NPM library and then adopted here.
          //val logger = streams.log
          //new String(result.output.toArray, "UTF-8").split("\n").foreach(s => logger.info(s))
          //new String(result.error.toArray, "UTF-8").split("\n").foreach(s => if (result.exitValue == 0) logger.info(s) else logger.error(s))
          if (result.exitValue == 0) {
            val jsSource = new String(result.output.toArray, "UTF-8")
            val minified = catching(classOf[CompilationException]).opt(play.core.jscompile.JavascriptCompiler.minify(jsSource, Some(file.getName())))
            (jsSource, minified, Seq(file))
          } else {
            throw new Exception(new String(result.error.toArray, "UTF-8"))
          }
        }
    }
    Await.result(pendingExitValue, timeo.duration)
  }

  val CompileAssets =
    (state, reactTimeout, sourceDirectory in Compile, resourceManaged in Compile, cacheDirectory, reactJs, jsxPath, options, entryPoints) map { (state, timeout, src, resources, cache, react, jsxPath, options, files) => {
    val watch: (File => PathFinder) = _ ** "*.jsx"
    val currentInfos = watch(src).get.map(f => f -> FileInfo.lastModified(f)).toMap
    val (previousRelation, previousInfo) = Sync.readInfo(cache)(FileInfo.lastModified.format)

    if (previousInfo != currentInfos) {
      lazy val changedFiles: Seq[File] = currentInfos.filter(e => !previousInfo.get(e._1).isDefined || previousInfo(e._1).lastModified < e._2.lastModified).map(_._1).toSeq ++ previousInfo.filter(e => !currentInfos.get(e._1).isDefined).map(_._1).toSeq

      //erase dependencies that belong to changed files
      val dependencies = previousRelation.filter((original, compiled) => changedFiles.contains(original))._2s
      dependencies.foreach(IO.delete)

      val generated: Seq[(File, File)] = files.pair(relativeTo(Seq(src / "assets"))).flatMap {
        case (sourceFile, name) => {
          if (changedFiles.contains(sourceFile) || dependencies.contains(new File(resources, "public/" + naming(name, false)))) {
            val (debug, min, dependencies) = compile(sourceFile, state, jsxPath, timeout, options)
            val out = new File(resources, "public/" + naming(name, false))
            IO.write(out, debug)
            (dependencies ++ Seq(sourceFile)).toSet[File].map(_ -> out) ++ min.map {
              minified =>
                val outMin = new File(resources, "public/" + naming(name, true))
                IO.write(outMin, minified)
                (dependencies ++ Seq(sourceFile)).map(_ -> outMin)
            }.getOrElse(Nil)
          } else {
            previousRelation.filter((original, compiled) => original == sourceFile)._2s.map(sourceFile -> _)
          }
        }
      }
      //write object graph to cache file
      Sync.writeInfo(cache,
        Relation.empty[File, File] ++ generated,
        currentInfos)(FileInfo.lastModified.format)

      // Return new files
      generated.map(_._2).distinct.toList

    } else {
      // Return previously generated files
      previousRelation._2s.toSeq
    }
  }


  }

  override def projectSettings = inTask(reactJs)(SbtJsTask.jsTaskSpecificUnscopedSettings) ++ Seq(
    sourceMap := true,

    shellFile := JSX,

    jsxPath := baseDirectory.value / JSX,

    reactVersion := "0.10.0",

    entryPoints <<= (sourceDirectory in Compile)(base => base / "assets" ** "*.jsx"),

    options := Seq.empty[String],

    reactTimeout := 2.minutes,

    resourceGenerators in Compile <+= CompileAssets,

    reactJs := {
      val modules = Seq(baseDirectory.value / "node_modules").map(_.getCanonicalPath)

      // TODO: May need to remove this later, just trying to speed things up for now.
      if (!new File(JSX).exists()) {
        streams.value.log.info(s"Fetching react-tools@${reactVersion.value}")
        implicit val timeout = Timeout(reactTimeout.value)
        val pendingExitValue = SbtWeb.withActorRefFactory(state.value, this.getClass.getName) {
          arf =>
            val to = new File(new File("target"), "webjars")
            val cacheFile = new File(to, "extraction-cache")
            val props = Trireme.props(stdEnvironment = NodeEngine.nodePathEnv(modules.to[immutable.Seq]))
            val engine = arf.actorOf(props)
            import ExecutionContext.Implicits.global
            for (
              result <- invokeJS(engine, NpmLoader.load(to, cacheFile, getClass.getClassLoader), Seq("install", "react-tools@" + reactVersion.value))
            ) yield {
              streams.value.log.info(s"Successfully installed react-tools@${reactVersion.value} via NPM.")
              //new String(result.output.toArray, "UTF-8").split("\n").foreach(s => logger.info(s))
              //new String(result.error.toArray, "UTF-8").split("\n").foreach(s => if (result.exitValue == 0) logger.info(s) else logger.error(s))
            }
        }
        Await.result(pendingExitValue, timeout.duration)
      }
    }
  )

}
