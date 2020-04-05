package genericSearch.common

import io.circe.optics.JsonOptics._
import monocle.function.Plated
import io.circe.Json

class FieldName(private val value: String) extends AnyVal {
  def asString: String = value

  override def toString: String = s"FieldName($value)"
}

class FieldValue (value: Json) {
  def asJson: Json = value

  private[FieldValue] def asLowerCase: Json = Helper.lowerCaseAllStrings(value)

  // make matching case insensitive
  override def hashCode(): Int = asLowerCase.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case fv: FieldValue => this.asLowerCase.equals(fv.asLowerCase)
    case _ => false
  }

  override def toString: String = s"FieldValue(${value.noSpaces})"
}


object Helper {
  private[common] def lowerCaseAllStrings(json: Json): Json =
    // plated.transform only applies to children
    json.asString match {
      case Some(s) =>
        Json.fromString(s.toLowerCase)
      case _ => Plated.transform[Json] { j =>
        j.asString match {
          case Some(s) => Json.fromString(s.toLowerCase)
          case None => j
        }
      }(json)
    }
}

/**
 * the index in the genericSearch.cache.Collection
 */
private[genericSearch] class Index(val value: Int) extends AnyVal {
  override def toString: String = s"Index($value)"
}


object Extensions {

  implicit class StringExt(s: String) {
    def asFieldName: FieldName = new FieldName(s)
  }

  implicit class JsonExt(json: Json) {
    def asFieldValue: FieldValue = new FieldValue(json)
  }

}
