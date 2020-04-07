package genericSearch.cache

import genericSearch.model.Entity
import genericSearch.common.{FieldName, FieldValue, Index}
import genericSearch.common.Extensions._
import gstlib.GeneralizedSuffixTree
import io.circe.Json

import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, Map => MMap}


/**
 * separate pk lookups from other fields, because they are unique
 * for each fieldname we hold a lookup table from all values to the index of the associated objects in the collection
 *
 */
private[cache] class FieldCache[T <: Entity[T]](entity: T,
                                                data: Map[FieldName, Map[FieldValue, Set[Index]]],
                                                byPk: Map[FieldValue, Index],
                                                suffixTrees: Map[FieldName, (Array[Index], GeneralizedSuffixTree[Char, String])]) {

  def get(name: FieldName, value: FieldValue): List[Index] = {
    if (name == entity.pkName)
      getByPk(value).toList
    else
      for {
        lookup <- data.get(name).toList
        entityId <- lookup.get(value).toList.flatten
      } yield entityId
  }


  def getByPk(pk: FieldValue): Option[Index] =
    byPk.get(pk)

  def find(name: FieldName, pattern: String): List[Index] =
    if (pattern.nonEmpty) {
      // st.find() fails on empty pattern
      suffixTrees.get(name).toList.flatMap { case (indices, st) =>
        st.find(pattern.toLowerCase).map { case (i, _) => indices(i) }
      }
    }
    else
      Nil

  def allFieldNames: List[FieldName] =
    entity.pkName +: data.keys.toList
}


private[cache] object FieldCache {


  def build[T <: Entity[T]](entity: T, collection: Collection): Either[DuplicatedPk[T], FieldCache[T]] = {

    // for performance use mutable collections within this function scope

    val fields = MMap[FieldName, MMap[FieldValue, ListBuffer[Index]]]()
    val byPk = MMap[FieldValue, Index]()
    val suffixTreeBuilders: Map[FieldName, (mutable.Builder[Index, Array[Index]], mutable.Builder[String, GeneralizedSuffixTree[Char, String]])] =
      entity.searchableFields.map((_, (Array.newBuilder[Index], GeneralizedSuffixTree.newBuilder[Char, String]))).toMap

    def updateMMaps(fieldName: FieldName, fieldValue: FieldValue, i: Index): Unit = {
      val fieldValueMap = fields.getOrElse(fieldName, MMap[FieldValue, ListBuffer[Index]]())
      val entityIndices = fieldValueMap.getOrElse(fieldValue, ListBuffer[Index]())
      fieldValueMap.update(fieldValue, entityIndices.append(i))
      fields.update(fieldName, fieldValueMap)
    }

    for {
      (e, i) <- collection.zipWithIndex
      (fieldName, fieldValue) <- e.fieldsAndVals
    } {
      if (fieldName == entity.pkName) {
        // verify pks are unique
        if (byPk.contains(fieldValue)) {
          return Left(DuplicatedPk[T](entity, fieldValue))
        }
        byPk.update(fieldValue, i)
      }
      else {
        updateMMaps(fieldName, fieldValue, i)
        // if the field holds an array, also add each element to cache
        fieldValue.asJson.asArray.foreach(_.foreach(json => updateMMaps(fieldName, json.asFieldValue, i)))
      }

      for {
        s <- fieldValue.asJson.asString
        (indices, stb) <- suffixTreeBuilders.get(fieldName)
        if s.nonEmpty
      } {
        stb.addOne(s.toLowerCase)
        indices.addOne(i)
      }
    }

    val data = fields.map { case (k, v) => (k, v.map { case (k2, v2) => (k2, v2.toSet) }.toMap) }.toMap

    Right(new FieldCache[T](
      entity = entity,
      data = data,
      byPk = byPk.toMap,
      suffixTrees = suffixTreeBuilders.map { case (fn, (indices, b)) => (fn, (indices.result, b.result)) }
    ))
  }
}



