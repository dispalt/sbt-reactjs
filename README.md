# ReactJS Source Compiler

This plugin will help you compile JSX files during the asset compilation phase.
It uses the autoPlugin feature with `0.13.5-RC3` of SBT to achieve that.

To use this plugin use the addSbtPlugin command within your project's
plugins.sbt (or as a global setting) i.e.:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-reactjs" % "0.2.0-SNAPSHOT")
```

Your project's build file also needs to enable sbt-web plugins. For example with build.sbt:

```scala
lazy val root = (project in file(".")).enablePlugins(SbtWeb)
```

Right now it uses the `unmanagedSourceDirectories` as the source for the files
to transform since the `jsx` compiler operates on directories.

# TODO
- [ ] Host the artifacts
