import bintray.Keys._

sbtPlugin := true

name := "sbt-reactjs"

organization := "com.github.ddispaltro"

version := "0.3.0"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies ++= Seq(
  "org.webjars" % "react" % "0.11.0",
  "org.webjars" % "jstransform" % "6.0.1",
  "org.webjars" % "esprima" % "4001.1.0-dev-harmony-fb",
  "org.webjars" % "base62js" % "707ebd9e05",
  "org.webjars" % "source-map" % "0.1.31-2",
  "org.webjars" % "mkdirp" % "0.3.5"
)

resolvers += "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.0.1")

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
