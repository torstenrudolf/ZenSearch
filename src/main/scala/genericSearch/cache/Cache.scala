package genericSearch.cache

import genericSearch.common.{FieldName, FieldValue}
import genericSearch.model.{Data, Entity}


/**
 * The Cache holds all the data for each entity in collections and
 * also has fieldCaches per entity to lookup the entities by fieldValues
 */
class Cache[T <: Entity[T]](cache: Map[T, (Collection, FieldCache[T])]) {

  def availableFields: Map[T, List[FieldName]] =
    (for {
      (e, (_, fc)) <- cache.toList
    } yield (e, fc.allFieldNames)).toMap


  def dataForField(entity: T, fieldName: FieldName, fieldValue: FieldValue): List[Data] = {
    val (collection, fieldCache) = cache(entity)
    for {
      index <- fieldCache.get(fieldName, fieldValue)
      data <- collection.get(index)
    } yield data
  }

  def dataForPk(entity: T, pk: FieldValue): Option[Data] = {
    val (collection, fieldCache) = cache(entity)
    for {
      index <- fieldCache.getByPk(pk)
      data <- collection.get(index)
    } yield data
  }

}


object Cache {
  def build[T <: Entity[T]](collections: Iterable[(T, Collection)]): Either[CacheBuildError[T], Cache[T]] = {
    import cats.implicits._

    collections
      .map { case (e, coll) => (e, (coll, FieldCache.build(e, coll))) }
      .toList
      .traverse { case (e, (coll, fce)) => fce.map(fc => (e, (coll, fc))) }
      .map(entityCaches => new Cache[T](entityCaches.toMap))
  }

}
