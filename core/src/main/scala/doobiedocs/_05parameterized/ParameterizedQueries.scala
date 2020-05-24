package doobiedocs._05parameterized

import hutil.stringformat._

import cats.data.NonEmptyList

import doobie._
import doobie.implicits._

object ParameterizedQueries extends hutil.App {

  import doobiedocs._ // imports Transactor xa + implicit ContextShift cs

  val y = xa.yolo
  import y._

  s"$dash10 Mapping result to a case class as in chapter 04 $dash10".magenta.println

  case class Country(code: String, name: String, pop: Int, gnp: Option[Double])

  sql"select code, name, population, gnp from country"
    .query[Country]
    .stream
    .take(5)
    .quick
    .unsafeRunSync
  //   Country(AFG,Afghanistan,22720000,Some(5976.0))
  //   Country(NLD,Netherlands,15864000,Some(371362.0))
  //   Country(ANT,Netherlands Antilles,217000,Some(1941.0))
  //   Country(ALB,Albania,3401200,Some(3205.0))
  //   Country(DZA,Algeria,31471000,Some(49982.0))

  s"$dash10 Adding a Parameter $dash10".magenta.println

  def biggerThan(minPop: Int) =
    sql"""|
          |select code, name, population, gnp
          |from country
          |where population > $minPop
          |""".stripMargin.query[Country]

  biggerThan(150000000)
    .quick
    .unsafeRunSync
  //   Country(BRA,Brazil,170115000,Some(776739.0))
  //   Country(IDN,Indonesia,212107000,Some(84982.0))
  //   Country(IND,India,1013662000,Some(447114.0))
  //   Country(CHN,China,1277558000,Some(982268.0))
  //   Country(PAK,Pakistan,156483000,Some(61289.0))
  //   Country(USA,United States,278357000,Some(8510700.0))

  s"$dash10 Multiple Parameters $dash10".magenta.println

  def populationIn(range: Range) =
    sql"""|
          |select code, name, population, gnp
          |from country
          |where population between ${range.min} and ${range.max}
          |""".stripMargin.query[Country]

  populationIn(150000000 to 200000000).quick.unsafeRunSync
  //   Country(BRA,Brazil,170115000,Some(776739.0))
  //   Country(PAK,Pakistan,156483000,Some(61289.0))

  s"$dash10 Dealing with IN Clauses (using Fragments) $dash10".magenta.println

  def populationIn(range: Range, codes: NonEmptyList[String]) = {
    val q =
      fr"""|
           |select code, name, population, gnp
           |from country
           |where population > ${range.min}
           |and   population < ${range.max}
           |and   """.stripMargin ++ Fragments.in(fr"code", codes) // code IN (...)
    q.query[Country]
  }

  populationIn(100000000 to 300000000, NonEmptyList.of("USA", "BRA", "PAK", "GBR")).quick.unsafeRunSync
  //   Country(BRA,Brazil,170115000,Some(776739.0))
  //   Country(PAK,Pakistan,156483000,Some(61289.0))
  //   Country(USA,United States,278357000,Some(8510700.0))

  s"$dash10 Diving Deeper $dash10".magenta.println

  import fs2.Stream

  val q =
    """|
       |select code, name, population, gnp
       |from country
       |where population > ?
       |and   population < ?
       |""".stripMargin

  def prog(range: Range): Stream[ConnectionIO, Country] =
    HC.stream[Country](q, HPS.set((range.min, range.max)), 512)

  prog(150000000 to 200000000).quick.unsafeRunSync
  //   Country(BRA,Brazil,170115000,Some(776739.0))
  //   Country(PAK,Pakistan,156483000,Some(61289.0))
}
