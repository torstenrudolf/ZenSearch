package genericSearch.cli

import genericSearch.common.{FieldName, FieldValue}
import genericSearch.common.Extensions._

import scala.util.parsing.combinator.RegexParsers

case class SearchTerm(entity: String, fieldName: FieldName, fieldValue: FieldValue)


/**
 * Parse a search input of the form: "<entity>.<fieldName> = <fieldValue>"
 */
object SearchTermParser {

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

    def eqls: Parser[String] = raw"\s*=\s*".r ^^ {
      _.toString
    }

    def searchTerm: Parser[SearchTerm] = entity ~ dot ~ fieldName ~ eqls ~ fieldValue ^^ { case e ~ _ ~ fn ~ _ ~ fv =>
      // try to parse as json
      val json = io.circe.parser.parse(fv) match {
        case Left(_) =>
          // treat value as string
          io.circe.Json.fromString(fv)
        case Right(json) =>
          json
      }
      SearchTerm(e, fn.asFieldName, json.asFieldValue)
    }

    def parse(s: String): Option[SearchTerm] =
      parse(searchTerm, s) match {
        case Success(matched,_) => Some(matched)
        case _ => None
      }
  }

  def parse(s: String): Option[SearchTerm] = Parser.parse(s)
}
