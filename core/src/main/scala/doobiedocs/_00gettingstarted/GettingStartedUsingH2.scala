package doobiedocs._00gettingstarted

import scala.concurrent.ExecutionContext
import scala.util.chaining._

import cats.effect.IO

import doobie._
import doobie.implicits._

object GettingStartedUsingH2 extends hutil.App {

  implicit val cs = IO.contextShift(ExecutionContext.global)

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:world;DB_CLOSE_DELAY=-1",
    "sa",
    ""
  )

  case class Country(code: String, name: String, population: Long)

  val dropTableIfExists: ConnectionIO[Int] =
    sql"drop table if exists country"
      .update
      .run

  val createTable: ConnectionIO[Int] =
    sql"create table country (code text, name text, population integer)"
      .update
      .run

  def insert(c: Country): ConnectionIO[Int] =
    sql"insert into country (code, name, population) values (${c.code}, ${c.name}, ${c.population})"
      .update
      .run

  def find(name: String): ConnectionIO[Option[Country]] =
    sql"select code, name, population from country where name = $name"
      .query[Country]
      .option

  val deleteAll: ConnectionIO[Int] =
    sql"delete from country"
      .update
      .run

  val program: ConnectionIO[Option[Country]] =
    for {
      _      <- dropTableIfExists
      _      <- createTable
      _      <- insert(Country("FRA", "France", 67000000L))
      _      <- insert(Country("Ger", "Germany", 83000000L))
      result <- find("France")
      _      <- deleteAll
      _      <- dropTableIfExists
    } yield result

  program
    .transact(xa)
    .unsafeRunSync() pipe println
}
