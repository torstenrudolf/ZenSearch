package genericSearch.cli

import genericSearch.cli.SearchType.{ExactMatch, Search}
import genericSearch.common.{FieldName, FieldValue}
import genericSearch.common.Extensions._

import scala.util.parsing.combinator.RegexParsers

sealed trait SearchType

object SearchType {

  case object ExactMatch extends SearchType

  case object Search extends SearchType

}

private[cli] case class SearchTerm(entity: String, fieldName: FieldName, fieldValues: List[FieldValue], searchType: SearchType)


/**
 * Parse a search input of the form: "<entity>.<fieldName> = <fieldValue>"
 */
private[cli] object SearchTermParser {

  private object Parser extends RegexParsers {
    def entity: Parser[String] =
      """[A-Za-z]+""".r ^^ {
        _.toString
      }

    def fieldName: Parser[String] =
      """[A-Za-z_0-9]+""".r ^^ {
        _.toString
      }

    def fieldValue: Parser[String] =
      """.*""".r ^^ {
        _.toString
      }

    def dot: Parser[String] = ".".r ^^ {
      _.toString
    }

    def exactMatch: Parser[ExactMatch.type] = raw"\s*=\s*".r ^^ {
      _.toString
    } ^^ (_ => ExactMatch)

    def search: Parser[Search.type] = raw"\s*contains\s*".r ^^ {
      _.toString
    } ^^ (_ => Search)

    def searchType: Parser[SearchType] = (exactMatch | search)

    def searchTerm: Parser[SearchTerm] = entity ~ dot ~ fieldName ~ searchType ~ fieldValue ^^ { case e ~ _ ~ fn ~ st ~ fv =>
      // try to parse as json
      val fieldVals: List[FieldValue] = io.circe.parser.parse(fv) match {
        case Left(_) =>
          // treat value as string
          fv.split(" or ").map { s =>
            io.circe.parser.parse(s) match {
              case Left(_) => io.circe.Json.fromString(s).asFieldValue
              case Right(j) => j.asFieldValue
            }
          }.toList
        case Right(json) =>
          List(json.asFieldValue)
      }
      SearchTerm(e, fn.asFieldName, fieldVals, st)
    }

    def parse(s: String): Option[SearchTerm] =
      parse(searchTerm, s) match {
        case Success(matched, _) => Some(matched)
        case _ => None
      }
  }

  def parse(s: String): Option[SearchTerm] = Parser.parse(s)
}
