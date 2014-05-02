addSbtPlugin("com.typesafe.sbt" % "sbt-reactjs" % sys.props("project.version"))

resolvers ++= Seq(
  Resolver.mavenLocal
)