package genericSearch.cli

import java.io.{InputStreamReader, OutputStreamWriter, Writer}

import ammonite.terminal.filters._
import ammonite.terminal.{Filter, Terminal}
import genericSearch.model.{Entity, EntitySelector}
import genericSearch.search.Search

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer


/**
 * CLI providing the functionality to search from the command-line.
 */
class CLI[E <: Entity[E]](search: Search[E],
                          welcomeMsg: String,
                          exitMessage: String = "Exiting application",
                          reader: java.io.Reader = new InputStreamReader(System.in),
                          writer: java.io.Writer = new OutputStreamWriter(System.out))
                         (implicit es: EntitySelector[E]) {

  private val HELP = "help"
  private val QUIT = "quit"

  private val help: String =
    s"""
       | 1) To exact match, write a query like: "<entity>.<fieldName> = <searchTerm>"
       |    eg: "User.name = Shelly Clements"
       |    Note:
       |    * Matching is case-insensitive
       |    * If the field value is an array, you also can search against an item of that array
       |      eg: "Organization.tags = Vega"
       |
       | 2) On some fields, full search is available.
       |    Write a query like: "<entity>.<fieldName> contains <searchTerm>"
       |    eg: "Ticket.subject contains problem"
       |    Note: type "$HELP" to see the list of fields where full search is available
       |
       | 3) Type "$HELP" to see a list of all available entities and fields to search for.
       |
       | 4) Type "$QUIT" or press ctrl+d to exit
       |""".stripMargin

  private def showHelp(writer: Writer): Unit = {

    writer.write(
      help + "\n\n" +
        search.availableFields.map { case (e, fields) =>
          s"""
             |Available search fields for ${es.nameForEntity(e)}:
             |${e.searchableFields.map(_.asString).mkString(", ")}
             |
             |Available fields for exact match for ${es.nameForEntity(e)}:
             |${fields.map(_.asString).mkString(", ")}
             |""".stripMargin
        }.mkString("\n\n") ++
        "\n")
    writer.flush()

  }


  private def parseInput(s: String, writer: Writer): Unit = {
    val msg: String = SearchTermParser.parse(s) match {
      case Some(searchTerm) =>
        es.entityForName(searchTerm.entity) match {
          case Some(e) =>
            if (search.availableFields.getOrElse(e, Nil).contains(searchTerm.fieldName)) {
              writer.write(
                s"""
                   |Searching for ${es.nameForEntity(e)}s with ${searchTerm.fieldName.asString} = ${searchTerm.fieldValue.asJson.noSpaces}...\n
                   |""".stripMargin
              )
              writer.flush()
              val searchResult = searchTerm.searchType match {
                case SearchType.ExactMatch => search.exactMatchSearch(e)(searchTerm.fieldName, searchTerm.fieldValue)
                case SearchType.Search => search.find(e)(searchTerm.fieldName, searchTerm.fieldValue.asJson.asString.getOrElse(""))
              }
              searchResult match {
                case Nil =>
                  "No results found."
                case responses =>
                  s"""
                     |Found ${responses.length} result${if (responses.length > 1) "s" else ""}:
                     |
                     |${responses.map(_.asJson.spaces2).mkString("\n\n")}
                     |
                     |End of ${responses.length} result${if (responses.length > 1) "s" else ""}
                   """.stripMargin
              }
            }
            else {
              s"""
                 |${Console.RED}${searchTerm.fieldName.asString} is not a valid field for ${es.nameForEntity(e)}${Console.RESET}
                 |
                 |Available fields for ${es.nameForEntity(e)}:
                 |${search.availableFields.getOrElse(e, Nil).map(_.asString).mkString("\n")}
                 |""".stripMargin
            }

          case None =>
            s"""
               |${Console.RED}Could not resolve entity '${searchTerm.entity}'${Console.RESET}
               |
               |Available entities: ${es.entities.map(es.nameForEntity).mkString(", ")}
               |""".stripMargin
        }
      case None =>
        s"""${Console.RED}Couldn't parse this input. Type "$HELP" for help.${Console.RESET}"""
    }
    writer.write(msg + "\n")
    writer.flush()
  }


  private val history = ListBuffer.empty[String]

  private val terminalFilters = Filter.merge(
    ReadlineFilters.CutPasteFilter(),
    new HistoryFilter(() => history.toVector, fansi.Color.Blue),
    BasicFilters.all
  )

  private def sayBye(): Unit = {
    writer.write(s"$exitMessage\n")
    writer.flush()
  }

  @tailrec protected final def waitForInput(showWelcome: Boolean = false): Unit = {
    Terminal.readLine(
      prompt = (if (showWelcome) Console.BOLD + welcomeMsg + Console.RESET else "") + "\n\n" + help + "\n\n",
      reader = reader,
      writer = writer,
      filters = terminalFilters
    ) match {
      case None =>
        sayBye()
      case Some(s) =>
        history.prepend(s)
        s match {
          case QUIT =>
            sayBye()
          case HELP =>
            showHelp(writer)
            waitForInput()
          case s =>
            parseInput(s, writer)
            waitForInput()
        }
    }
  }

  def run(): Unit =
    waitForInput(showWelcome = true)

}
