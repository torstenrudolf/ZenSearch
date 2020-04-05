package genericSearch.dataLoader

import genericSearch.cache.{Cache, CacheBuildError, Collection}
import genericSearch.common.FieldName
import genericSearch.common.Extensions._
import genericSearch.model.{Data, Entity}
import io.circe.parser.parse
import io.circe.{DecodingFailure, Json, JsonObject}
import cats.implicits._
import scala.io.Source

object DataLoader {

  sealed trait LoadingError {
    def message: String
  }

  final case class JsonLoadingError(error: io.circe.Error) extends LoadingError {
    val message = s"Failed to load json file. Circe error: `${error.getMessage}`"
  }

  final case class NoPkFound(pkName: FieldName, json: Json) extends LoadingError {
    val message = s"Could not find pk `${pkName}` in data: ${json}"
  }

  private def loadCollection[T <: Entity[T]](entity: T)(fileName: String): Either[LoadingError, Collection] = {
    val fileContent: String = Source.fromResource(fileName).mkString

    def jsonObjectToData(json: JsonObject): Either[NoPkFound, Data] = {
      val d = new Data(json.toMap.map { case (k, v) => (k.asFieldName, v.asFieldValue) })
      d.get(entity.pkName) match {
        case Some(_) => Right(d)
        case None => Left(NoPkFound(entity.pkName, Json.fromFields(json.toIterable)))
      }
    }

    parse(fileContent) match {
      case Left(err) => Left(JsonLoadingError(err))
      case Right(json) if json.isArray =>
        val jObj = for {
          jArr <- json.asArray.toIterable
          obj <- jArr
          jObj <- obj.asObject
        } yield jObj
        jObj
          .toList
          .traverse(jsonObjectToData)
          .map(Collection(_))

      case Right(_) => Left(JsonLoadingError(DecodingFailure("json needs to be an array", Nil)))

    }
  }


  /**
   * @param sources : List of (Entity, resourceFilename.json)
   */
  def load[T <: Entity[T]](sources: Iterable[(T, String)]): Either[Either[LoadingError, CacheBuildError[T]], Cache[T]] = {

    sources.toList
      .traverse { case (e, f) => DataLoader.loadCollection(e)(f).map(c => (e, c)) }
      .map(Cache.build(_).leftMap(Right[LoadingError, CacheBuildError[T]]))
      .leftMap(Left[LoadingError, CacheBuildError[T]])
      .joinRight

  }
}
