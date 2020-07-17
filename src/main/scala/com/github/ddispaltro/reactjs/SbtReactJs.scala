package com.github.ddispaltro.reactjs

import sbt._
import sbt.Keys._
import com.typesafe.jse._

import scala.collection.{JavaConversions, immutable}
import com.typesafe.npm.NpmLoader
import com.typesafe.sbt.web.pipeline.Pipeline
import akka.util.Timeout

import scala.concurrent.{Await, ExecutionContext, Future}
import com.typesafe.sbt.web.{CompileProblems, SbtWeb, incremental}

import scala.concurrent.duration._
import com.typesafe.sbt.jse.{SbtJsEngine, SbtJsTask}
import com.typesafe.jse.Engine.JsExecutionResult
import akka.actor.ActorRef
import akka.pattern.ask

import scala.util.control.Exception._
import com.typesafe.sbt.web.incremental.{OpInputHash, OpInputHasher, OpSuccess}
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
import com.typesafe.sbt.web.Import.WebKeys


object Import {



  object ReactJsKeys {

    val reactJs = TaskKey[Seq[File]]("reactjs", "Perform ReactJS JSX compilation on the asset pipeline.")

    val timeout         = SettingKey[FiniteDuration]("reactjs-timeout", "How long before timing out JS runtime.")
    val harmony         = SettingKey[Boolean]("reactjs-harmony", "Support harmony features.")
    val es6module       = SettingKey[Boolean]("reactjs-es6module", "Support ES6 modules.")
    val stripTypes      = SettingKey[Boolean]("reactjs-strip-types", "Strips out type annotations.")
    val sourceMapInline = SettingKey[Boolean]("reactjs-source-map-inline", "Embed inline sourcemap in transformed source.")
  }

}

object SbtReactJs extends AutoPlugin {

  override def requires = SbtJsTask

  override def trigger = AllRequirements

  val autoImport = Import

  import SbtWeb.autoImport._
  import WebKeys._
  import SbtJsTask.autoImport.JsTaskKeys._
  import autoImport.ReactJsKeys._



  val reactJsScriptUnscopedSettings = Seq(

    includeFilter := "*.jsx",

    jsOptions := JsObject(
      "harmony" -> JsBoolean(harmony.value),
      "es6module" → JsBoolean(es6module.value),
      "stripTypes" -> JsBoolean(stripTypes.value),
      "sourceMap" -> JsBoolean(sourceMapInline.value)
    ).toString()
  )

  override def buildSettings = inTask(reactJs)(
    SbtJsTask.jsTaskSpecificUnscopedBuildSettings ++ Seq(
      moduleName := "reactjs",
      shellFile := getClass.getClassLoader.getResource("jsx.js")
    )
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
        taskMessage in Assets := "ReactJS compiling",
        taskMessage in TestAssets := "ReactJS test compiling"
      )
  ) ++ SbtJsTask.addJsSourceFileTasks(reactJs) ++ Seq(
    reactJs in Assets := (reactJs in Assets).dependsOn(webModules in Assets, nodeModules in Assets).value,
    reactJs in TestAssets := (reactJs in TestAssets).dependsOn(webModules in TestAssets, nodeModules in TestAssets).value
  )



}
