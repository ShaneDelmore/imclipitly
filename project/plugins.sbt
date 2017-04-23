addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
// looks for keys in ~/.gnupg and ~/.sbt/gpg

addSbtPlugin("com.eed3si9n" % "sbt-doge" % "0.1.5")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")

// for sbt plugin tests
libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
