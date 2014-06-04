# ReactJS Source Compiler

This plugin hooks your JSX files in to the Asset compilation phase.
It uses the autoPlugin feature with `0.13.5` to make the setup dead-simple.

To use this plugin use the addSbtPlugin command within your project's
plugins.sbt (or as a global setting) i.e.:

```scala
addSbtPlugin("com.github.ddispaltro" % "sbt-reactjs" % "0.2.1-SNAPSHOT")
```

Your project's build file also needs to enable sbt-web plugins. For example with build.sbt:

```scala
lazy val root = (project in file(".")).enablePlugins(SbtWeb)
```

# Options

Right now the only option is to use `harmony` similar to the jsx compiler.

The artifact is hosted as part of the [community plugins](http://www.scala-sbt.org/0.13.5/docs/Community/Bintray-For-Plugins.html)
via [bintray service](https://bintray.com/ddispaltro/sbt-plugins/sbt-reactjs/view). **Right now the hosting seems broken**


# License

This software is Apache 2 licensed.
