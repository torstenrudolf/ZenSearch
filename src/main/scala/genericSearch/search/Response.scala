package genericSearch.search

import genericSearch.common.FieldName
import genericSearch.model.Data
import io.circe.Json


case class Response(data: Data, relations: List[(FieldName, Data)]) {
  def asJson: Json = Json.fromFields(
    Map(
      "object" -> data.asJson,
      "resolvedRelations" -> Json.fromFields(relations.map { case (k, d) => (k.asString, d.asJson) })
    )
  )
}
