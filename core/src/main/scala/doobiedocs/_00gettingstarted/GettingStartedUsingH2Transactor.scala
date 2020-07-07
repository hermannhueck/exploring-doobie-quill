package doobiedocs._00gettingstarted

import scala.concurrent.ExecutionContext
import scala.util.chaining._

import cats.effect.{Blocker, IO, Resource}

import doobie._
import doobie.h2._
import doobie.implicits._

object GettingStartedUsingH2Transactor extends hutil.App {

  implicit val cs = IO.contextShift(ExecutionContext.global)

  val ec                                           = ExecutionContext.global
  val xaH2Resource: Resource[IO, H2Transactor[IO]] = H2Transactor.newH2Transactor[IO](
    "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", // connect URL
    "sa",                                 // username
    "",                                   // password
    ec,                                   // await connection here
    Blocker.liftExecutionContext(ec)      // execute JDBC operations here
  )

  case class Country(code: String, name: String, population: Long)

  val dropTable: ConnectionIO[Int] =
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

  xaH2Resource
    .use { xa =>
      (for {
        _      <- dropTable
        _      <- createTable
        _      <- insert(Country("FRA", "France", 67000000L))
        _      <- insert(Country("Ger", "Germany", 83000000L))
        result <- find("France")
        _      <- deleteAll
        _      <- dropTable
      } yield result).transact(xa)
    }
    .unsafeRunSync() pipe println
}
