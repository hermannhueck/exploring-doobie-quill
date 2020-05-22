package doobiedocs._00gettingstarted

import scala.concurrent.ExecutionContext
import scala.util.chaining._

import cats.effect.IO

import doobie._
import doobie.h2._
import doobie.implicits._

object GettingStartedUsingH2WithFragments extends hutil.App {

  implicit val cs = IO.contextShift(ExecutionContext.global)

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:world;DB_CLOSE_DELAY=-1",
    "sa",
    ""
  )

  case class Country(code: String, name: String, population: Long)

  object Country {
    val dummy  = Country("DMY", "Dummy", 0L)
    val prefix = dummy.productPrefix
    val columnNames: List[String] =
      dummy.productElementNames.toList
  }

  val tableFragment      = Fragment.const(Country.prefix)
  val colFragments       = Country.columnNames.map(Fragment.const(_))
  val codeFragment       = colFragments(0)
  val nameFragment       = colFragments(1)
  val populationFragment = colFragments(2)

  val dropTableIfExists: ConnectionIO[Int] =
    sql"drop table if exists country"
      .update
      .run

  val createTable: ConnectionIO[Int] =
    (fr"create table" ++ tableFragment ++
      fr"(" ++ codeFragment ++ fr"text," ++ nameFragment ++ fr"text," ++ populationFragment ++ fr"integer)")
      .update
      .run

  def insert(c: Country): ConnectionIO[Int] =
    (fr"insert into " ++ tableFragment ++
      fr" (" ++ codeFragment ++ fr"," ++ nameFragment ++ fr"," ++ populationFragment ++
      fr") values (${c.code}, ${c.name}, ${c.population})")
      .update
      .run

  def find(name: String): ConnectionIO[Option[Country]] =
    (fr"select" ++ codeFragment ++ fr"," ++ nameFragment ++ fr"," ++ populationFragment ++
      fr" from" ++ tableFragment ++ fr" where" ++ nameFragment ++ fr" = $name")
      .query[Country]
      .option

  val deleteAll: ConnectionIO[Int] =
    (fr"delete from" ++ tableFragment)
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
    .unsafeRunSync pipe println
}
