package imclipitly

import scala.meta.scalahost.sbt.ScalahostSbtPlugin

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

import sbt._
import Keys._
import sbt.plugins.JvmPlugin

// generic plugin for wrapping any command-line interface as an sbt plugin
object CliWrapperPlugin extends AutoPlugin {

  def createSyntheticProject(id: String, base: File): Project =
    Project(id, base).settings(publish := {},
                               publishLocal := {},
                               publishArtifact := false)
  class HasMain(reflectiveMain: Main) {
    def main(args: Array[String]): Unit = reflectiveMain.main(args)
  }
  type Main = {
    def main(args: Array[String]): Unit
  }
  object autoImport {
    val cliWrapperClasspath =
      taskKey[Classpath]("classpath to run code generation in")
    val cliWrapperMainClass =
      taskKey[String]("Fully qualified name of main class")
    val cliWrapperMain =
      taskKey[HasMain]("Classloaded instance of main")
    // Returns emitted output from (stdout, stderr) while evaluating thunk.
    def cliWrapperRun[T](thunk: => T): (String, String) = {
      val out: ByteArrayOutputStream = new ByteArrayOutputStream()
      val err: ByteArrayOutputStream = new ByteArrayOutputStream()
      scala.Console.withOut(new PrintStream(out)) {
        scala.Console.withErr(new PrintStream(err)) {
          thunk
        }
      }
      out.toString -> err.toString
    }
    // (optional) wrap cliWrapperRun in this function to skip running
    // cli on files that have not changed since last run.
    def cliWrapperIncremental(init: Set[File], cacheDir: File)(
        onChange: Set[File] => Set[File]): Seq[File] = {
      def handleUpdate(inReport: ChangeReport[File],
                       outReport: ChangeReport[File]) = {
        onChange(inReport.modified -- inReport.removed)
      }
      val x = FileFunction.cached(cacheDir)(
        FilesInfo.lastModified,
        FilesInfo.lastModified)(handleUpdate)(init)
      x.toSeq
    }
  }
  import autoImport._
  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    cliWrapperMain := {
      compileInputs.in(Compile, compile).value
      val cp = cliWrapperClasspath.value.map(_.data.toURI.toURL)
      val cl = new java.net.URLClassLoader(cp.toArray, null)
      val cls = cl.loadClass(cliWrapperMainClass.value)
      val constuctor = cls.getDeclaredConstructor()
      constuctor.setAccessible(true)
      val main = constuctor.newInstance().asInstanceOf[Main]
      new HasMain(main)
    }
  )
}

object ImclipitlyPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires: Plugins =
    ScalahostSbtPlugin && CliWrapperPlugin && JvmPlugin
  object autoImport {
    lazy val imclipitlyIn = taskKey[Seq[File]]("--in files")
    lazy val imclipitlyOut = settingKey[File]("--out directory")
  }
  import autoImport._
  import CliWrapperPlugin.autoImport._
  import ScalahostSbtPlugin.autoImport._
  lazy val imclipitlyCodegenProject = CliWrapperPlugin
    .createSyntheticProject(
      "imclipitly",
      file("project/imclipitly") // should be something like file("project/clippy-codgen")
    )
    .settings(
      scalaVersion := ImclipitlyBuildInfo.coreScalaVersion,
      description := "Automatically generate clippy advice on package.",
      libraryDependencies := Seq(
        "io.delmore" %% "imclipitly-core" % ImclipitlyBuildInfo.version
      )
    )

  override def extraProjects: Seq[Project] = Seq(imclipitlyCodegenProject)

  lazy val imclipitlySettings = Seq(
    imclipitlyOut := resourceManaged.in(Compile).value / "clippy",
    imclipitlyIn := sources.in(Compile).value.**("*.scala").get,
    // can be managedClasspath if imclipitly project has no main.
    cliWrapperClasspath := fullClasspath
      .in(imclipitlyCodegenProject, Compile)
      .value,
    cliWrapperMainClass := "imclipitly.Codegen$",
    resourceGenerators.in(Compile) += Def.task {
      val sourcepath = sourceDirectories
        .in(Compile)
        .value
        .map(_.getAbsolutePath)
        .mkString(File.pathSeparator)
      val classpath = fullClasspath
        .in(Compile)
        .value
        .map(_.data.getAbsolutePath)
        .mkString(File.pathSeparator)
      val log = streams.value.log
      val cache = target.in(Compile, compile).value / "clippy-codegen"
      val in = imclipitlyIn.value.toSet
      cliWrapperIncremental(in, cache)(_.map {
        in =>
          val relative =
            in.relativeTo(baseDirectory.value).getOrElse(in).toPath
          val out =
            imclipitlyOut.value.toPath
              .resolve(relative.toString + ".clippy")
          val args = Array(
            "--in",
            in.getAbsolutePath,
            "--out",
            out.toString,
            "--mirror-sourcepath",
            sourcepath,
            "--mirror-classpath",
            classpath
          )
          log.info(s"Running codegen for $in")
          cliWrapperRun(cliWrapperMain.value.main(args))
          out.toFile
      })
    }
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    imclipitlySettings
}
