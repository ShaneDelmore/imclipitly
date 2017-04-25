addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")

//To publish to sonatype
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.1")

//To cross-build
addSbtPlugin("com.eed3si9n" % "sbt-doge" % "0.1.5")

// for sbt plugin tests
libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
