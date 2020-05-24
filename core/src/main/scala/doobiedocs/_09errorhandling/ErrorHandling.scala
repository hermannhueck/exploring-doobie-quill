// See 'book of doobie', chapter 09:
// https://tpolecat.github.io/doobie/docs/09-Error-Handling.html
//
package doobiedocs._09errorhandling

import scala.util.chaining._

import hutil.stringformat._

import cats.syntax.applicative._
import cats.syntax.applicativeError._

import doobie._
import doobie.implicits._

object ErrorHandling extends hutil.App {

  import doobiedocs._ // imports Transactor xa + implicit ContextShift cs

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

  import cats.syntax.traverse._
  import cats.instances.all._
  import cats.effect.IO

  val io: IO[List[Unit]] = List(
    sql"""DROP TABLE IF EXISTS person""",
    sql"""CREATE TABLE person (
          id    SERIAL,
          name  VARCHAR NOT NULL UNIQUE
        )"""
  ).traverse(frag => frag.update.quick)
  io.void.unsafeRunSync
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
