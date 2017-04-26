sonatypeProfileName := "io.delmore"

lazy val allSettings = Seq(
  version := "0.0.3",
  scalaVersion := "2.12.2",
  organization := "io.delmore",
  description := "Make your project more clippity implicitly with imclipitly",
  pomIncludeRepository := { _ =>
    false
  },
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url("http://github.com/shanedelmore/imclipitly/")),
  scmInfo := Some(
    ScmInfo(url("https://github.com/shanedelmore/imclipitly"), "scm:git@github.com:shanedelmore/imclipitly.git")
  ),
  publishArtifact in Test := false,
  publishMavenStyle := true,
  sonatypeDefaultResolver := Opts.resolver.sonatypeStaging,
  useGpg := false,
  developers := List(
    Developer(
      id = "sdelmore",
      name = "Shane Delmore",
      email = "shane@delmore.io",
      url = url("http://github.com/ShaneDelmore/imclipitly/tree/master/readme.md")
    )
  ),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
)

lazy val `imclipitly-core` = project
  .settings(
    allSettings,
    scalaVersion := "2.12.2",
    libraryDependencies += "com.github.alexarchambault" %% "case-app" % "1.2.0-M3",
    libraryDependencies += "org.scalameta" %% "scalameta" % "1.7.0",
    libraryDependencies += "org.json4s" %% "json4s-native" % "3.5.0",
    libraryDependencies += "com.softwaremill.clippy" %% "plugin" % "0.5.2",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.3" % Test
  )

lazy val `imclipitly-sbt` = project
  .configs(IntegrationTest)
  .settings(
    moduleName := "sbt-imclipitly", // sbt plugin convention to use sbt-*
    allSettings,
    scriptedSettings,
    buildInfoSettings,
    Defaults.itSettings,
    // to run: imclipitly/it:test
    test.in(IntegrationTest) := {
      scripted
        .toTask("")
        .dependsOn(
          publishLocal.in(`imclipitly-core`)
        )
        .value
    },
    sbtPlugin := true,
    scalaVersion := "2.10.6"
  )
  .enablePlugins(BuildInfoPlugin) // used to inject version number

// boilerplate
lazy val buildInfoSettings: Seq[Def.Setting[_]] = Seq(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    "coreScalaVersion" -> scalaVersion.in(`imclipitly-core`).value
  ),
  buildInfoObject := "ImclipitlyBuildInfo",
  buildInfoPackage := "imclipitly"
)

lazy val scriptedSettings =
  ScriptedPlugin.scriptedSettings ++ Seq(
    scriptedLaunchOpts := Seq(
      "-Dplugin.version=" + version.value,
      "-Dscalafmt.scripted=true",
      "-XX:MaxPermSize=256m",
      "-Xmx2g",
      "-Xss2m"
    ),
    scriptedBufferLog := false
  )
lazy val noPublish = Seq(
  publish := {},
  publishArtifact := false,
  publishLocal := {}
)
noPublish

addCommandAlias("testAll", ";test ;it:test")
