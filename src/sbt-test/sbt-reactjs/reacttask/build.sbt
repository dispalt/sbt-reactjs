lazy val root = (project in file(".")).enablePlugins(SbtWeb)

ReactJsKeys.harmony := true

ReactJsKeys.sourceMapInline := true
