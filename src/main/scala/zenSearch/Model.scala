package zenSearch

import genericSearch.common.Extensions._
import genericSearch.common.FieldName
import genericSearch.model.{Entity, EntitySelector, Relation}


object Model {

  sealed abstract class ZenEntity(pkName: FieldName, relations: List[Relation[ZenEntity]])
    extends Entity[ZenEntity](pkName, relations)

  case object Org
    extends ZenEntity("_id".asFieldName, relations = Nil)

  case object User
    extends ZenEntity(
      pkName = "_id".asFieldName,
      relations = List(Relation("organisation".asFieldName, "organization_id".asFieldName, Org)))


  case object Ticket
    extends ZenEntity(
      pkName = "_id".asFieldName,
      relations = List(
        Relation("submitter".asFieldName, "submitter_id".asFieldName, User),
        Relation("assignee".asFieldName, "assignee_id".asFieldName, User),
        Relation("organization".asFieldName, "organization_id".asFieldName, Org)
      ))


  val ZenEntitySelector: EntitySelector[ZenEntity] = new EntitySelector[ZenEntity] {
    override val entities: List[ZenEntity] = List(Org, User, Ticket)

    override def nameForEntity(e: ZenEntity): String = e match {
      case Org => "Organization"
      case User => "User"
      case Ticket => "Ticket"
    }

    override def entityForName(name: String): Option[ZenEntity] = name match {
      case "Organization" => Some(Org)
      case "User" => Some(User)
      case "Ticket" => Some(Ticket)
      case _ => None
    }
  }

}
