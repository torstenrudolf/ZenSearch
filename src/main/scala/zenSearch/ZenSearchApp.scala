package zenSearch

import genericSearch.cache.DuplicatedPk
import genericSearch.cli.CLI
import genericSearch.dataLoader.DataLoader
import genericSearch.dataLoader.DataLoader.LoadingError
import genericSearch.search.Search
import zenSearch.Model._


object ZenSearchApp extends App {
  val search =
    DataLoader
      .load[ZenEntity](List((User, "users.json"), (Ticket, "tickets.json"), (Org, "organizations.json"))) match {
      case Left(err) =>
        err match {
          case Left(loadingError) =>
            println(
              s"""
                 |Data loading failed.
                 |Error:
                 |${loadingError.message}

                 |""".stripMargin)

          case Right(cacheBuildError) =>
            cacheBuildError match {
              case DuplicatedPk(e, v) =>
                println(s"Found duplicated primary keys for entity ${ZenEntitySelector.nameForEntity(e)} with value '${v}''")
            }
        }
        sys.exit()

      case Right(cache) =>
        new Search[ZenEntity](cache)
    }


  new ZenSearchApp(search).cli.run()

}


class ZenSearchApp(search: Search[ZenEntity]) {

  def cli: CLI[ZenEntity] =
    new CLI[ZenEntity](search, graffiti + "\n\n" + welcome)(ZenEntitySelector)

  private val graffiti =
    """
      | ______           _____                     _
      ||___  /          /  ___|                   | |
      |   / /  ___ _ __ \ `--.  ___  __ _ _ __ ___| |__
      |  / /  / _ \ '_ \ `--. \/ _ \/ _` | '__/ __| '_ \
      |./ /__|  __/ | | /\__/ /  __/ (_| | | | (__| | | |
      |\_____/\___|_| |_\____/ \___|\__,_|_|  \___|_| |_|
      |
      |""".stripMargin

  private val welcome: String = "Welcome to ZenSearch!"


}
