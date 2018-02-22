//import bintray.BintrayKeys._

//scalaVersion := "2.12.4"

sbtPlugin := true

name := "sbt-reactjs"

organization := "com.github.ddispaltro"

scalacOptions ++= Seq("-deprecation", "-feature")



libraryDependencies ++= Seq(
  "org.webjars.npm" % "react" % "16.2.0",
  "org.webjars.npm" % "react-tools" % "0.13.3",
  "org.webjars.npm" % "jstransform" % "11.0.3",
  "org.webjars" % "esprima" % "13001.1.0-dev-harmony-fb",
  "org.webjars" % "base62js" % "1.0.0",
  "org.webjars.npm" % "source-map" % "0.6.1",
  "org.webjars" % "mkdirp" % "0.5.0",
  "org.webjars.npm" % "minimatch" % "3.0.4"
)

//addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.2.2")
addSbtJsEngine("1.2.2")

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

pomIncludeRepository := { _ => false }

// Bintray settings
publishMavenStyle := false

//TODO: ADD again
//bintrayPublishSettings
//TODO: ADD again
//repository in bintray := "sbt-plugins"
//TODO: ADD again
//bintrayOrganization in bintray := None