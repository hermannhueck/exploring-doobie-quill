package doobiedocs._09errorhandling

import scala.util.chaining._

import hutil.stringformat._

import cats._
import cats.data._
import cats.effect._
import cats.implicits._

import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts

object ErrorHandling extends hutil.App {

  // We need a ContextShift[IO] before we can construct a Transactor[IO]. The passed ExecutionContext
  // is where nonblocking operations will be executed. For testing here we're using a synchronous EC.
  implicit val cs = IO.contextShift(ExecutionContexts.synchronous)

  // A transactor that gets connections from java.sql.DriverManager and executes blocking operations
  // on an our synchronous EC. See the chapter on connection handling for more info.
  val xa: Transactor.Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",                                    // driver classname
    "jdbc:postgresql:world",                                    // connect URL (driver-specific)
    "postgres",                                                 // user
    "",                                                         // password
    Blocker.liftExecutionContext(ExecutionContexts.synchronous) // just for testing
  )

  val y = xa.yolo
  import y._

  s"$dash10 MonadError and Derived Combinators $dash10".magenta.println

  val p = 42.pure[ConnectionIO]
  // p: ConnectionIO[Int] = Pure(42)
  val res0: ConnectionIO[Either[Throwable, Int]] = p.attempt
  res0 pipe println
  // res0: ConnectionIO[Either[Throwable, Int]] = Suspend(
  //   HandleErrorWith(
  //     FlatMapped(Pure(42), cats.Monad$$Lambda$8144/773217938@2bab87cb),
  //     cats.ApplicativeError$$Lambda$8235/2046171693@2d546afa
  //   )
  // )

  s"$dash10 Example: Unique Constraint Violation $dash10".magenta.println

  List(
    sql"""DROP TABLE IF EXISTS person""",
    sql"""CREATE TABLE person (
          id    SERIAL,
          name  VARCHAR NOT NULL UNIQUE
        )"""
  ).traverse(_.update.quick).void.unsafeRunSync
  //   0 row(s) updated
  //   0 row(s) updated

  case class Person(id: Int, name: String)

  def insert(s: String): ConnectionIO[Person] =
    sql"insert into person (name) values ($s)"
      .update
      .withUniqueGeneratedKeys("id", "name")

  // The first insert will work.
  insert("bob").quick.unsafeRunSync
  //   Person(1,bob)

  // The second will fail with a unique constraint violation.
  try {
    insert("bob").quick.unsafeRunSync
  } catch {
    case e: java.sql.SQLException =>
      println(e.getMessage)
      println(e.getSQLState)
  }
  // ERROR: duplicate key value violates unique constraint "person_name_key"
  //   Detail: Key (name)=(bob) already exists.
  // 23505

  import doobie.postgres._

  def safeInsert(s: String): ConnectionIO[Either[String, Person]] =
    insert(s).attemptSomeSqlState {
      case sqlstate.class23.UNIQUE_VIOLATION => "Oops!"
    }

  // Given this definition we can safely attempt to insert duplicate records
  // and get a helpful error message rather than an exception.

  safeInsert("bob").quick.unsafeRunSync
  //   Left(Oops!)

  safeInsert("steve").quick.unsafeRunSync
  //   Right(Person(4,steve))
}
