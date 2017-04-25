Imclipitly: Because I thought you might want to replace easy to forget imports with an easy to misspell plugin.

Imclipitly will generate scala-clippy advice for implicit enrichments in your source code.

Imclipitely is only published for Scala 2.12 currently.  I will cross publish to 2.11 shortly.

To add Imclipitly to your project:

in plugins.sbt: `addSbtPlugin("io.delmore" %% "sbt-imclipitly" % "0.0.2")`

in build.sbt: `enablePlugins(ImclipitlyPlugin)`

On run or package Imclipitly will scan your source code using Scalameta and ScalaFix and then generate Scala-Clippy advice in your managed-resources directory.

To run tests:

`sbt test`

To run integration tests:

`sbt it:test`

Project contributors: Shane Delmore, Ólafur Páll Geirsson 
