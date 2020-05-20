package doobiedocs._06checking

import scala.util.chaining._

import hutil.stringformat._

import cats._
import cats.data._
import cats.effect._
import cats.implicits._

import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts

object ParameterizedQueries extends hutil.App {

  // We need a ContextShift[IO] before we can construct a Transactor[IO]. The passed ExecutionContext
  // is where nonblocking operations will be executed. For testing here we're using a synchronous EC.
  implicit val cs = IO.contextShift(ExecutionContexts.synchronous)

  // A transactor that gets connections from java.sql.DriverManager and executes blocking operations
  // on an our synchronous EC. See the chapter on connection handling for more info.
  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",                                    // driver classname
    "jdbc:postgresql:world",                                    // connect URL (driver-specific)
    "postgres",                                                 // user
    "",                                                         // password
    Blocker.liftExecutionContext(ExecutionContexts.synchronous) // just for testing
  )

  val y = xa.yolo
  import y._

  s"$dash10 Checking a Query (check prints errors) $dash10".magenta.println

  case class Country(code: String, name: String, pop: Int, gnp: Option[Double])

  def biggerThan(minPop: Short) =
    sql"""|
          |select code, name, population, gnp, indepyear
          |from country
          |where population > $minPop
          |""".stripMargin.query[Country]

  // Now let’s try the check method provided by YOLO and see what happens.

  biggerThan(0).check.unsafeRunSync

  s"$dash10 Checking a Query (check for column 4 OK) $dash10".magenta.println

  case class Country2(code: String, name: String, pop: Int, gnp: Option[BigDecimal])

  def biggerThan2(minPop: Short) =
    sql"""|
          |select code, name, population, gnp
          |from country
          |where population > $minPop
          |""".stripMargin.query[Country2]

  // Now let’s try the check method provided by YOLO and see what happens.

  biggerThan2(0).check.unsafeRunSync

  s"$dash10 Working Around Bad Metadata (using checkOutput) $dash10".magenta.println

  biggerThan2(0).checkOutput.unsafeRunSync
}
