package doobiedoc.gettingstarted

import cats.effect.IO
import doobie._
import doobie.implicits._
import scala.concurrent.ExecutionContext
import scala.util.chaining._

object GettingStarted extends hutil.App {

  implicit val cs = IO.contextShift(ExecutionContext.global)

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres",
    ""
  )

  case class Country(code: String, name: String, population: Long)

  def find(name: String): ConnectionIO[Option[Country]] =
    sql"select code, name, population from country where name = $name"
      .query[Country]
      .option

  find("France")
    .transact(xa)
    .unsafeRunSync pipe println
}
