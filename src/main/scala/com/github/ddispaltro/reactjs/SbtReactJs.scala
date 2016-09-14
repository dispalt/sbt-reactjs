package com.github.ddispaltro.reactjs

import sbt._
import sbt.Keys._
import com.typesafe.jse._
import scala.collection.{JavaConversions, immutable}
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
import scala.util.control.Exception._
import com.typesafe.sbt.web.incremental.{OpInputHasher, OpInputHash, OpSuccess}
import spray.json._
import DefaultJsonProtocol._
import scala.collection.mutable.ListBuffer


import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.commonjs.module.Require
import org.mozilla.javascript.commonjs.module.RequireBuilder
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider
import com.typesafe.jse.Engine.JsExecutionResult


object Import {

  val reactJs = TaskKey[Seq[File]]("reactjs", "Perform ReactJS JSX compilation on the asset pipeline.")

  object ReactJsKeys {

    val timeout         = SettingKey[FiniteDuration]("reactjs-timeout", "How long before timing out JS runtime.")
    val harmony         = SettingKey[Boolean]("reactjs-harmony", "Support harmony features.")
    val es6module       = SettingKey[Boolean]("reactjs-es6module", "Support ES6 modules.")
    val stripTypes      = SettingKey[Boolean]("reactjs-strip-types", "Strips out type annotations.")
    val sourceMapInline = SettingKey[Boolean]("reactjs-source-map-inline", "Embed inline sourcemap in transformed source.")
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

  override def requires = SbtJsTask

  override def trigger = AllRequirements

  val reactJsScriptUnscopedSettings = Seq(

    includeFilter := "*.jsx",

    jsOptions := JsObject(
      "harmony" -> JsBoolean(harmony.value),
      "es6module" → JsBoolean(es6module.value),
      "stripTypes" -> JsBoolean(stripTypes.value),
      "sourceMap" -> JsBoolean(sourceMapInline.value)
    ).toString()
  )

  override def projectSettings = Seq(
    harmony := false,
    es6module := false,
    stripTypes := false,
    sourceMapInline := false
  ) ++ inTask(reactJs)(
    SbtJsTask.jsTaskSpecificUnscopedProjectSettings ++
      inConfig(Assets)(reactJsScriptUnscopedSettings) ++
      inConfig(TestAssets)(reactJsScriptUnscopedSettings) ++
      Seq(
        moduleName := "reactjs",
        shellFile := getClass.getClassLoader.getResource("jsx.js"),

        taskMessage in Assets := "ReactJS compiling",
        taskMessage in TestAssets := "ReactJS test compiling"
      )
  ) ++ SbtJsTask.addJsSourceFileTasks(reactJs) ++ Seq(
    reactJs in Assets := (reactJs in Assets).dependsOn(webModules in Assets).value,
    reactJs in TestAssets := (reactJs in TestAssets).dependsOn(webModules in TestAssets).value
  )



}
