package imclipitly

import org.json4s.native.JsonMethods._
import com.softwaremill.clippy._
import scala.meta._

import java.nio.file.Files
import java.nio.file.Paths

import caseapp._
import org.scalameta.logger

case class CodegenOptions(
    in: String,
    out: String,
    mirrorSourcepath: String,
    mirrorClasspath: String
)
case class Enrichment(
    path: String,
    implicitClass: Type.Name,
    name: Term.Name,
    from: Type.Arg,
    to: Option[Type]
)

object Codegen extends CaseApp[CodegenOptions] {
  override def exit(code: Int): Unit =
    if (code != 0) {
      throw new IllegalArgumentException(s"Non-zero exit status: $code ")
    }

  def run(options: CodegenOptions, ignored: RemainingArgs): Unit = {
    logger.elem(options.mirrorSourcepath, options.mirrorClasspath)
    val mirror = Mirror(options.mirrorSourcepath, options.mirrorClasspath)
    logger.elem(mirror.database)
    val in = Paths.get(options.in)
    val out = Paths.get(options.out)
    val source = in.toFile.parse[Source].get
    val enrichments = extractEnrichments(source)
    val adviceOutput = enrichments.map(toAdvice)
    val json = pretty(render(Clippy("0.1", adviceOutput).toJson))

    if (enrichments.nonEmpty) {
      out.toFile.getParentFile.mkdirs()
      org.scalameta.logger.elem(out)
      Files.write(out, json.getBytes())
    }
  }

  def extractEnrichments(tree: Tree): List[Enrichment] =
    tree.collect {
      case ref: Defn.Class if ref.mods.exists(_.syntax == "implicit") =>
        val fromTypes = ref.ctor.paramss.flatten.map(_.decltpe).flatten //Grab the first param only
        ref.templ.collect {
          case d: Defn.Def =>
            Enrichment(
              path = path(d),
              implicitClass = ref.name,
              name = d.name,
              from = fromTypes.headOption.get,
              to = d.decltpe
            )
        }
    }.flatten

  def enumerateParents(acc: List[Tree], cur: Tree): List[Tree] =
    cur.parent match {
      case None => acc
      case Some(parent) => enumerateParents(parent +: acc, parent)
    }

  def path(tree: Tree): String = {
    enumerateParents(Nil, tree)
      .collect {
        case pkg: Pkg =>
          pkg.syntax
            .replace("package ", "")
            .trim
            .split("\\s")
            .head //split on whitespace, get just the package name
        case obj: Defn.Object => obj.name
      }
      .mkString(".")
  }

  def toAdvice(enrichment: Enrichment): Advice =
    Advice(
      NotAMemberError(
        what = RegexT(s"value ${enrichment.name.syntax}"),
        notAMemberOf = RegexT(s".*${enrichment.from.syntax}.*")
      ),
      s"You may need to import ${enrichment.path}._",
      Library("lib", "package", "version")
    )

  //Use these to test and help clippy
  import java.io.File
  import scala.collection.JavaConverters._

  def listFilesForFolder(folder: File): List[File] = {
    folder.listFiles.flatMap { fileEntry =>
      if (fileEntry.isDirectory)
        listFilesForFolder(fileEntry)
      else List(fileEntry)
    }.toList
  }

  val files = getClass.getClassLoader
    .getResources("clippy")
    .asScala
    .toList
    .flatMap { entry =>
      val file = new File(entry.getFile)
      listFilesForFolder(file)
    }

}
