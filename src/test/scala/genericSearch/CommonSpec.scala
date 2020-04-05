package genericSearch.common

import genericSearch.common.Extensions._
import io.circe.Json
import io.circe.parser.parse
import utest._


object CommonSpec extends TestSuite {


  val tests = Tests {

    test("json lowercase transform of nested objects") {
      val json = parse(
        """
          |{
          |"k": ["ASD", {"k2": "BSD"}]
          |}
          |""".stripMargin).right.get
      val lcJson = Helper.lowerCaseAllStrings(json)
      assert(lcJson == parse("""{"k": ["asd", {"k2": "bsd"}]}""").right.get)
    }

    test("json lowercase transform of strings") {
      val json = Json.fromString("TEST string ZX")
      val lcJson = Helper.lowerCaseAllStrings(json)
      assert(lcJson == Json.fromString("test string zx"))
    }

    test("FieldValues match case insensitive") {
      val fv1 = Json.fromString("value1").asFieldValue
      val fv2 = Json.fromString("VALUE1").asFieldValue

      assert(fv1 == fv2)

      val map = Map(fv1 -> "YEP")
      assert(map(fv2) == map(fv1))
    }

  }

}