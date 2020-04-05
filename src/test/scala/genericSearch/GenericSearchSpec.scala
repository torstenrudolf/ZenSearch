package genericSearch

import genericSearch.cache.DuplicatedPk
import genericSearch.common.Extensions._
import genericSearch.dataLoader.DataLoader
import genericSearch.dataLoader.DataLoader.{JsonLoadingError, NoPkFound}
import io.circe.Json
import utest._





object GenericSearchSpec extends TestSuite {
  import TestModel._

  val tests = Tests {

    test("All fields are loaded") {
      val fields = search.availableFields.map(x => (x._1, x._2.map(_.asString).toSet))
      assert(fields == Map(
        Org -> Set("id", "number", "name", "shared_tickets", "tags", "admin_id"),
        User -> Set("_id", "name", "alias", "organization_id")
      ))
    }

    test("Search with relation lookup works") {

      val orgSearch = search.search(Org)("name".asFieldName, Json.fromString("TinyCorp").asFieldValue)
      assert(orgSearch.size == 1)
      val searchRes = orgSearch.head.asJson
      assert(searchRes == Json.fromFields(Map(
        "object" -> io.circe.parser.parse(
          """
            |{
            |    "id": 102,
            |    "name": "TinyCorp",
            |    "shared_tickets": false,
            |    "tags": [
            |      "Cherry",
            |      "Collier",
            |      "West"
            |    ],
            |    "admin_id": 5
            |  }
            |""".stripMargin
        ).right.get,
        "resolvedRelations" -> Json.fromFields(Map(
          "admin" -> io.circe.parser.parse(
            """
              |{
              |    "_id": 5,
              |    "name": "Loraine Pittman",
              |    "alias": "Mr Ola",
              |    "organization_id": 102
              |  }
              |""".stripMargin
          ).right.get
        ))
      )))
    }

    test("Search copes if relation reference cannot be resolved") {

      val orgSearch = search.search(Org)("id".asFieldName, Json.fromInt(104).asFieldValue)
      assert(orgSearch.size == 1)
      val searchRes = orgSearch.head.asJson
      assert(searchRes == Json.fromFields(Map(
        "object" -> io.circe.parser.parse(
          """
            |{
            |    "id": 104,
            |    "admin_id": 123
            |  }
            |""".stripMargin
        ).right.get,
        "resolvedRelations" -> Json.fromFields(List())
      )))
    }

    test("Lookup on array fields") {
      val orgSearch = search.search(Org)("tags".asFieldName,Json.fromValues(List("Fulton", "West").map(Json.fromString)).asFieldValue)
      assert(orgSearch.size == 1)
      val id = orgSearch.head.data.get("id".asFieldName).get
      assert(id == Json.fromInt(101).asFieldValue)
    }

    test("Lookup on array fields matching on single value") {
      val orgSearch = search.search(Org)("tags".asFieldName,Json.fromString("West").asFieldValue)
      assert(orgSearch.size == 2)
      val ids = orgSearch.map(_.data.get("id".asFieldName).get).toSet
      assert(ids == Set(101, 102).map(Json.fromInt).map(_.asFieldValue))
    }

    test("Can search for empty fields") {
      val userSearch = search.search(User)("name".asFieldName, Json.fromString("").asFieldValue)
      assert(userSearch.size == 1)
      val searchRes = userSearch.head.asJson
      assert(searchRes == Json.fromFields(Map(
        "object" -> io.circe.parser.parse(
          """
            |{
            |        "_id": 2,
            |        "name": "",
            |        "alias": "Miss Joni"
            |      }
            |""".stripMargin
        ).right.get,
        "resolvedRelations" -> Json.fromFields(List())
      )))
    }

    test("Search is case-insensitive") {
        val orgSearch = search.search(Org)("tags".asFieldName,Json.fromString("WEST").asFieldValue)
        assert(orgSearch.size == 2)
        val ids = orgSearch.map(_.data.get("id".asFieldName).get).toSet
        assert(ids == Set(101, 102).map(Json.fromInt).map(_.asFieldValue))
      }

    test("Dataloading fails if ids not unique") {
      val cacheE = DataLoader.load[TestModel](List((Org, "orgs_duplicated_ids.json")))
      assert(cacheE == Left(Right(DuplicatedPk[TestModel](Org, Json.fromInt(101).asFieldValue))))
    }

    test("Dataloading fails with correct type if json not parseable") {
      val cacheE = DataLoader.load[TestModel](List((Org, "orgs_faulty_json.json")))
      assert(cacheE.left.get.left.get.isInstanceOf[JsonLoadingError])
    }

    test("Dataloading fails with correct type if no pk given for an object") {
      val cacheE = DataLoader.load[TestModel](List((Org, "orgs_missing_pk.json")))
      assert(cacheE == Left(Left(NoPkFound("id".asFieldName, io.circe.parser.parse(
        """
          |{
          |    "_id": 102,
          |    "name": "TinyCorp"
          |  }
          |""".stripMargin
      ).right.get))))
    }


  }


}

