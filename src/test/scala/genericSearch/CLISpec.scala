package genericSearch.cli


import java.io.{StringReader, StringWriter}

import genericSearch.TestModel.search
import genericSearch.common.Extensions.{JsonExt, StringExt}
import io.circe.Json
import utest._



object CLISpec extends TestSuite {


  val tests = Tests {

    test("basic cli functionality") {
      val myReader = new StringReader("help\nUser._id=3\nquit\n")
      val myWriter = new StringWriter()

      val myCli = new CLI(search, "welcome", writer= myWriter, reader=myReader)
      myCli.run()

      val out: String = myWriter.toString

      assert(out contains "help")
      assert(out contains "To exact match, write a query like: \"<entity>.<fieldName> = <searchTerm>\"")
      assert(out contains "Searching for Users with _id = 3...")
      assert(out contains "Found 1 result:")
      assert(out contains "\"name\" : \"Ingrid Wagner\"")
    }


    test("SearchTermParser parses string as value2") {
      assert(SearchTermParser.parse("User._id contains test string") ==
        Some(SearchTerm("User", "_id".asFieldName, Json.fromString("test string").asFieldValue, SearchType.Search)))
    }


    test("SearchTermParser parses object as value") {
      assert(SearchTermParser.parse("""User._id = {"tags": ["tag1", "tag2"]}""") ==
        Some(SearchTerm(
          "User",
          "_id".asFieldName,
          Json.fromFields(List(("tags", Json.fromValues(List(Json.fromString("tag1"), Json.fromString("tag2")))))).asFieldValue,
          SearchType.ExactMatch)))
    }


  }

}
