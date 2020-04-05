package genericSearch.model

import genericSearch.common.{FieldName, FieldValue}

/**
 * An entity is a type of objects in the search space.
 *
 * An entity can have relations to other entities.
 */
abstract class Entity[T <: Entity[T]](val pkName: FieldName, val relations: List[Relation[T]]) {
  def pkVal(data: Data): FieldValue = data.get(pkName).get
}


case class Relation[T <: Entity[T]](name: FieldName, foreignKey: FieldName, entity: T)


/**
 * A type class describing the available entities in the search space
 */
trait EntitySelector[T <: Entity[T]] {
  val entities: List[T]

  def nameForEntity(e: T): String

  def entityForName(name: String): Option[T]
}
