package genericSearch.model

import genericSearch.common.{FieldName, FieldValue}
import io.circe.Json
import io.circe.syntax._


class Data(private val data: Map[FieldName, FieldValue]) extends AnyVal {

  def get(f: FieldName): Option[FieldValue] = data.get(f)

  def contains(f: FieldName): Boolean = data.contains(f)

  def fields: Iterable[FieldName] = data.keys
  def fieldsAndVals: Iterable[(FieldName, FieldValue)] = data

  def asJson: Json = Json.fromFields(data.map{case (k,v) => (k.asString, v.asJson)})

  override def toString: String = data.map{ case (k, v) => k.asString -> v.asJson}.asJson.spaces2
}
