package imclipitly

import org.json4s.native.JsonMethods._
import com.softwaremill.clippy._

import scala.collection.JavaConverters._
import scala.meta._
import java.nio.file.{ Files, Path, Paths }

import caseapp._

import scala.util.Try

case class CodegenOptions(in: String, out: String, org: String, appname: String, appversion: String)
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
    val lib = createLibrary(options)
    val in = Paths.get(options.in)
    val out = Paths.get(options.out)
    val clippyJsonContents = slurp(out)
    val fileContents = slurp(in)
    val output = updateAdvice(lib, clippyJsonContents, fileContents)

    output.foreach { outString =>
      out.toFile.getParentFile.mkdirs()
      org.scalameta.logger.elem(out)
      Files.write(out, outString.getBytes())
    }
  }

  def createLibrary(options: CodegenOptions): Library = {
    val newAppName = options.appname + ":" + withoutSharedPrefix(options.in, options.out)
    Library(options.org, newAppName, options.appversion)
  }

  def withoutSharedPrefix(in: String, out: String): String = {
    val commonPrefixLength = in
      .zip(out)
      .takeWhile(Function.tupled(_ == _))
      .length
    in.drop(commonPrefixLength)
  }

  def slurp(path: Path): String =
    Try(Files.readAllLines(path).asScala.mkString("\n")).getOrElse("")

  def updateAdvice(lib: Library, existing: String, input: String): Option[String] = {
    val existingAdvice: Set[Advice] = Try(parseAdvice(existing)).getOrElse(Set.empty)
    val toRemove = existingAdvice.filter(_.library == lib)
    val toAdviceWithLib: (Enrichment) => Advice = toAdvice(lib, _)
    val source = input.parse[Source].get
    val enrichments = extractEnrichments(source)
    val toAdd = enrichments.map(toAdviceWithLib).toSet

    if (toRemove.isEmpty && toAdd.isEmpty)
      None
    else {
      val updatedAdvice = (existingAdvice -- toRemove) ++ toAdd
      Some(pretty(render(Clippy("0.1", updatedAdvice.toList).toJson)))
    }
  }

  def parseAdvice(input: String): Set[Advice] = {
    import org.json4s.native.JsonMethods._
    Clippy
      .fromJson(parse(input))
      .getOrElse(throw new IllegalArgumentException("Cannot deserialize Clippy data"))
      .advices
      .toSet
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

  def path(tree: Tree): String =
    enumerateParents(Nil, tree)
      .collect {
        case pkg: Pkg => packageName(pkg)
        case obj: Defn.Object => obj.name
      }
      .mkString(".")

  def packageName(pkg: Pkg): String =
    pkg.syntax
      .replace("package ", "")
      .trim
      .split("\\s")
      .head //split on whitespace, get just the package name

  //A rough matcher for now, will come back and create a more accurate matcher another day.
  def regexForType(tpe: Type.Arg): String = {
    val name = tpe.syntax.takeWhile(_ != '[')
    s".*$name.*"
  }

  def toAdvice(lib: Library, enrichment: Enrichment): Advice =
    Advice(
      NotAMemberError(
        what = RegexT(s"value ${enrichment.name.syntax}"),
        notAMemberOf = RegexT.fromRegex(regexForType(enrichment.from))
      ),
      s"You may need to import ${enrichment.path}._",
      lib
    )
}
