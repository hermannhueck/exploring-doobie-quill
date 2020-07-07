package doobiedocs._00gettingstarted

import scala.concurrent.ExecutionContext
import scala.util.chaining._

import cats.effect.IO

import doobie._
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
    val dummy                     = Country("DMY", "Dummy", 0L)
    val prefix                    = dummy.productPrefix
    val columnNames: List[String] =
      dummy.productElementNames.toList
  }

  val tableFragment      = Fragment.const(Country.prefix)
  val tableFragment0     = Fragment.const0(Country.prefix)
  val columnFragments    = Country.columnNames.map(Fragment.const(_))
  val codeFragment       = columnFragments(0)
  val nameFragment       = columnFragments(1)
  val populationFragment = columnFragments(2)

  val dropTableIfExists: ConnectionIO[Int] =
    (fr"drop table if exists" ++ tableFragment0)
      .update
      .run

  val createTable: ConnectionIO[Int] =
    (fr"create table" ++ tableFragment ++
      fr"(" ++ codeFragment ++ fr"text," ++ nameFragment ++ fr"text," ++ populationFragment ++ fr0"integer)")
      .update
      .run

  def insert(c: Country): ConnectionIO[Int] =
    (fr"insert into " ++ tableFragment ++
      fr" (" ++ codeFragment ++ fr"," ++ nameFragment ++ fr"," ++ populationFragment ++
      fr0") values (${c.code}, ${c.name}, ${c.population})")
      .update
      .run

  def find(name: String): ConnectionIO[Option[Country]] =
    (fr"select" ++ codeFragment ++ fr"," ++ nameFragment ++ fr"," ++ populationFragment ++
      fr" from" ++ tableFragment ++ fr" where" ++ nameFragment ++ fr0" = $name")
      .query[Country]
      .option

  val deleteAll: ConnectionIO[Int] =
    (fr"delete from" ++ tableFragment0)
      .update
      .run

  val program: ConnectionIO[Option[Country]] =
    for {
      _     <- dropTableIfExists
      _     <- createTable
      _     <- insert(Country("FRA", "France", 67000000L))
      _     <- insert(Country("Ger", "Germany", 83000000L))
      found <- find("France")
      _     <- deleteAll
      _     <- dropTableIfExists
    } yield found

  program
    .transact(xa)
    .unsafeRunSync() pipe println
}
