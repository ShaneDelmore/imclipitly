package imclipitly

import org.scalatest.{FunSpec, MustMatchers}
import scala.meta._
import Codegen._

class CodegenSpec extends FunSpec with MustMatchers {
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
      val source =
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
       """.stripMargin.parse[Source].get

      val extracted = extractEnrichments(source)
      extracted.map(_.name.syntax) mustBe List("increment", "double")
    }
  }
}
