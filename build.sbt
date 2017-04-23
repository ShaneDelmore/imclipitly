scalaVersion in ThisBuild := "2.12.2"

inThisBuild(
  Seq(
  organization := "io.delmore",
  description in ThisBuild := "implicitly make your project more clippity with imclipitly",
  version := "0.1-SNAPSHOT",
  pomIncludeRepository := { _ => false },
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url("http://github.com/shanedelmore/imclipitly/")),
  scmInfo := Some(ScmInfo(url("https://github.com/shanedelmore/imclipitly"), "scm:git@github.com:shanedelmore/imclipitly.git")),
  publishArtifact in Test := false,
  bintrayReleaseOnPublish := false,
  bintrayOrganization := None,
  bintrayRepository := "sbt-plugins",
  bintrayPackage := "sbt-imclipitly"
  )
)

lazy val imclipitly = project.in(file("."))
//  .aggregate(clippyCodegen)
  .enablePlugins(CrossPerProjectPlugin)
  .disablePlugins(BintrayPlugin)
  .settings(
    publishArtifact := false,
    publish := {},
    publishLocal := {},
    PgpKeys.publishSigned := {},
    PgpKeys.publishLocalSigned := {}
  )

lazy val app = project
  .enablePlugins(
    ClippyCodegenPlugin
  )

commands += Command.command("cleanrun") { state =>
  "app/clean" ::
    "app/run" ::
    state
}
