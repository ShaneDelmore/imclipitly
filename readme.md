[![Gitter](https://img.shields.io/gitter/room/imclipitly/Lobby.svg)](https://gitter.im/imclipitly/Lobby)

### Imclipitly: Now you can replace easy-to-forget imports with an easy-to-misspell plugin.

Imclipitly will generate Scala-Clippy advice for implicit enrichments in your source code.

This plugin is currently published for Scala 2.11 and 2.12.

### To add Imclipitly to your project:

* in plugins.sbt: `addSbtPlugin("io.delmore" %% "sbt-imclipitly" % "0.0.3")`

* in build.sbt: `enablePlugins(ImclipitlyPlugin)`

* and most importantly, make sure you have [SoftwareMill](https://softwaremill.com/)'s [Scala-Clippy](https://github.com/softwaremill/scala-clippy) plugin enabled. 

On run or package, Imclipitly will scan your source code using Scalameta and then generate Scala-Clippy advice in your managed-resources directory.

### To run tests:

* `sbt test`

### To run integration tests:

* `sbt it:test`

_Project contributors: Shane Delmore, Ólafur Páll Geirsson_ 
