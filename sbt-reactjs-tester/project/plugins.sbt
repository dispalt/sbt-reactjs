lazy val root = Project("plugins", file(".")).dependsOn(plugin)

lazy val plugin = ProjectRef(file("../").getCanonicalFile.toURI, "sbt-reactjs")