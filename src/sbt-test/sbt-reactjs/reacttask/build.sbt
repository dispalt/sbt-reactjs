lazy val root = (project in file(".")).enablePlugins(SbtWeb)

ReactJsKeys.harmony := true

ReactJsKeys.stripTypes := true

ReactJsKeys.sourceMapInline := true
