package imclipitly

import org.scalatest.{ FunSpec, MustMatchers }
import org.json4s.native.JsonMethods._
import scala.meta._
import Codegen._
import com.softwaremill.clippy.Library

class CodegenSpec extends FunSpec with MustMatchers {
  import TestData._

  describe("Codegen") {
    it("can construct path when using java style package syntax") {
      val source = """
          |package level1.level2
          |object target
        """.stripMargin.parse[Source].get

      val target = source.collect { case obj: Defn.Object => obj }.head
      path(target) mustBe "level1.level2"
    }

    it("can construct path when using scala style package syntax") {
      val source = """
          |package level1
          |package level2
          |object target
        """.stripMargin.parse[Source].get

      val target = source.collect { case obj: Defn.Object => obj }.head
      path(target) mustBe "level1.level2"
    }

    it("extracts enrichments from source code") {
      val extracted = extractEnrichments(testCodeSource.parse[Source].get)
      extracted.map(_.name.syntax) mustBe List("increment", "double")
    }

    it("identifies advice by appname and relative file path") {
      val lib = createLibrary(testCodeOptions)
      lib.artifactId mustBe "demoApp:src/main/scala/testProject/TestCode.scala"
    }

    describe("Incremental compilation") {
      it("removes existing advice from the file being reprocessed") {
        updateAdvice(moreTestCodeLib, sampleExistingAdvice, "") must not be empty
      }

      it("leaves existing advice from other files") {
        updateAdvice(testCodeLib, sampleExistingAdvice, "") mustBe empty
      }

      it("merges in new advice") {
        updateAdvice(testCodeLib, sampleExistingAdvice, moreTestCodeSource) must not be empty
      }
    }
  }
}

object TestData {
  val testCodeOptions = CodegenOptions(
    in = "/anyOldPath/app/app/src/main/scala/testProject/TestCode.scala",
    out = "/anyOldPath/app/app/target/scala-2.10/resource_managed/clippy.json",
    org = "io.delmore",
    appname = "demoApp",
    appversion = "1.0.0"
  )

  val moreTestCodeOptions = CodegenOptions(
    in = "/anyOldPath/app/app/src/main/scala/testProject/MoreTestCode.scala",
    out = "/anyOldPath/app/app/target/scala-2.10/resource_managed/clippy.json",
    org = "app",
    appname = "app",
    appversion = "0.1-SNAPSHOT"
  )

  val testCodeLib = createLibrary(testCodeOptions)

  val moreTestCodeLib = createLibrary(moreTestCodeOptions)

  val testCodeSource =
    """
      |package demo
      |
      |object Enrichments {
      |  implicit class IntImprovements(num: Int) {
      |    def increment = num + 1
      |
      |    def double: Int = num * 2
      |  }
      |}
    """.stripMargin

  val moreTestCodeSource =
    """
      |package demo
      |
      |object OtherEnrichments {
      |  implicit class OptionImprovements[A](opt: Option[A]) {
      |    def valueOr(default: A) = opt.getOrElse(default)
      |  }
      |}
    """.stripMargin

  val sampleExistingAdvice =
    """
      |{
      |   "version":"0.1",
      |   "advices":[{
      |     "error":{
      |       "type":"notAMember",
      |       "what":"value valueOr",
      |       "notAMemberOf":".*Option.*"
      |     },
      |     "text":"You may need to import demo.OtherEnrichments._",
      |     "library":{
      |       "groupId":"app",
      |       "artifactId":"app:src/main/scala/testProject/MoreTestCode.scala",
      |       "version":"0.1-SNAPSHOT"
      |     }
      |   }]
      | }
      |
    """.stripMargin
}
