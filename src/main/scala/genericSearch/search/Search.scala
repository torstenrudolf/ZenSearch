package genericSearch.search

import genericSearch.cache.Cache
import genericSearch.common.{FieldName, FieldValue}
import genericSearch.model.Entity


class Search[T <: Entity[T]](cache: Cache[T]) {

  def exactMatchSearch(entity: T)(fieldName: FieldName, fieldValues: List[FieldValue]): List[Response] = {
    for {
      fieldValue <- fieldValues
      data <- cache.dataForField(entity, fieldName, fieldValue)
      relations = for {
        r <- entity.relations
        fk <- data.get(r.foreignKey)
        d <- cache.dataForPk(r.entity, fk)
      } yield (r.name, d)
    } yield Response(data, relations)
  }

  def find(entity: T)(fieldName: FieldName, patterns: List[String]): List[Response] = {
    for {
      pattern <- patterns
      data <- cache.dataForSearch(entity, fieldName, pattern)
      relations = for {
        r <- entity.relations
        fk <- data.get(r.foreignKey)
        d <- cache.dataForPk(r.entity, fk)
      } yield (r.name, d)
    } yield Response(data, relations)
  }

  lazy val availableFields: Map[T, List[FieldName]]  = cache.availableFields

}
