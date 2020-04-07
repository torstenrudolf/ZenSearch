package genericSearch

import genericSearch.common.FieldName
import genericSearch.common.Extensions._
import genericSearch.dataLoader.DataLoader
import genericSearch.model.{Entity, EntitySelector, Relation}
import genericSearch.search.Search


sealed abstract class TestModel(pkName: FieldName, relations: List[Relation[TestModel]], searchableFields: List[FieldName])
  extends Entity[TestModel](pkName, relations, searchableFields)

object TestModel {

  // tests circular reference of entities
  case object Org
    extends TestModel(
      pkName = "id".asFieldName,
      relations = List(Relation("admin".asFieldName, "admin_id".asFieldName, User)),
      searchableFields = List("name".asFieldName))

  case object User
    extends TestModel(
      pkName = "_id".asFieldName,
      relations = List(Relation("organization".asFieldName, "organization_id".asFieldName, Org)),
      searchableFields = List("name".asFieldName))

  implicit val es: EntitySelector[TestModel] = new EntitySelector[TestModel] {
    override val entities: List[TestModel] = List(Org, User)

    override def nameForEntity(e: TestModel): String = e match {
      case Org => "Organization"
      case User => "User"
    }

    override def entityForName(name: String): Option[TestModel] = name match {
      case "Organization" => Some(Org)
      case "User" => Some(User)
      case _ => None
    }
  }


  val search = {
    val cache = DataLoader.load[TestModel](List((Org, "orgs.json"), (User, "users.json")))
    new Search(cache.right.get)
  }


}