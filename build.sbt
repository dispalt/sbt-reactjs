

sbtPlugin := true

name := "sbt-reactjs"

organization := "com.github.ddispaltro"

scalacOptions ++= Seq("-deprecation", "-feature")



libraryDependencies ++= Seq(
  "org.webjars.npm" % "react" % "16.13.1",
  "org.webjars.npm" % "react-tools" % "0.13.3",
  "org.webjars.npm" % "js-tokens" % "4.0.0",
  "org.webjars.npm" % "jstransform" % "11.0.3",
  "org.webjars"     % "esprima" % "13001.1.0-dev-harmony-fb",
  "org.webjars"     % "base62js" % "1.0.0",
  "org.webjars.npm" % "source-map" % "0.7.3",
  "org.webjars"     % "mkdirp" % "0.5.0",
  "org.webjars.npm" % "minimatch" % "3.0.4"
)

addSbtJsEngine("1.2.3")

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

ThisBuild / crossScalaVersions := Seq("2.12.11", "2.10.7")
ThisBuild / pluginCrossBuild / sbtVersion := {
  scalaBinaryVersion.value match {
    case "2.10" => "0.13.18"
    case "2.12" => "1.2.8"
  }
}

pomIncludeRepository := { _ => false }


publishMavenStyle := false

// Bintray settings
bintrayRepository in bintray := "sbt-plugins"
bintrayOrganization in bintray := None
