import bintray.Keys._

sbtPlugin := true

name := "sbt-reactjs"

organization := "com.github.ddispaltro"

version := "0.5.1"

scalaVersion := "2.10.5"

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies ++= Seq(
  "org.webjars" % "react" % "0.13.3",
  "org.webjars" % "jstransform" % "10.1.0",
  "org.webjars" % "esprima" % "13001.1.0-dev-harmony-fb",
  "org.webjars" % "base62js" % "1.0.0",
  "org.webjars" % "source-map" % "0.1.40-1",
  "org.webjars" % "mkdirp" % "0.5.0"
)

resolvers += "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.0.2")

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

// Bintray settings
publishMavenStyle := false

bintrayPublishSettings

repository in bintray := "sbt-plugins"

bintrayOrganization in bintray := None

// Scripted testing config
scriptedSettings

scriptedBufferLog := false

scriptedLaunchOpts += s"-Dproject.version=${version.value}"
