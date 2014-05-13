sbtPlugin := true

name := "sbt-reactjs"

organization := "com.typesafe.sbt"

version := "0.1.1-SNAPSHOT"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies ++= Seq(
  "org.webjars" % "react" % "0.10.0",
  "com.novocode" % "junit-interface" % "0.10" % "test",
  "org.scalatest" %% "scalatest" % "2.1.5" % "test"
)

resolvers ++= Seq(
  "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.url("sbt snapshot plugins", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns),
  Resolver.sonatypeRepo("snapshots"),
  "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/",
  Resolver.mavenLocal
)

addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.0.0-RC1")

addSbtPlugin("com.typesafe.sbt" % "sbt-webdriver" % "1.0.0-RC1")

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.0.0-RC1")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.0-RC1")

scriptedSettings

scriptedBufferLog := false

scriptedLaunchOpts <+= version apply { v => s"-Dproject.version=$v" }
