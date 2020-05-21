package doobiedocs._11arrays

import scala.util.chaining._

import hutil.stringformat._

import cats._
import cats.data._
import cats.effect._
import cats.implicits._

import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts

object SQLArrays extends hutil.App {

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

  val drop = sql"DROP TABLE IF EXISTS person".update.quick

  val create =
    sql"""
    CREATE TABLE person (
      id   SERIAL,
      name VARCHAR   NOT NULL UNIQUE,
      pets VARCHAR[] NOT NULL
    )
  """.update.quick

  (drop *> create).unsafeRunSync
  //   0 row(s) updated
  //   0 row(s) updated

  s"$dash10 Reading and Writing Arrays $dash10".magenta.println

  case class Person(id: Long, name: String, pets: List[String])

  // vendor specific import required to map Postgres SQLArray to Array, List or Vector
  import doobie.postgres.implicits._

  def insert(name: String, pets: List[String]): ConnectionIO[Person] = {
    sql"insert into person (name, pets) values ($name, $pets)"
      .update
      .withUniqueGeneratedKeys("id", "name", "pets")
  }

  insert("Bob", List("Nixon", "Slappy")).quick.unsafeRunSync
  //   Person(1,Bob,List(Nixon, Slappy))

  insert("Alice", Nil).quick.unsafeRunSync
  //   Person(2,Alice,List())

  s"$dash10 Lamentations of NULL $dash10".magenta.println

  sql"select array['foo','bar','baz']".query[List[String]].quick.unsafeRunSync
  //   List(foo, bar, baz)

  // sql"select array['foo',NULL,'baz']".query[List[String]].quick.unsafeRunSync
  // throws:
  // [error] (run-main-50) doobie.util.invariant$NullableCellRead$:
  // SQL `NULL` appears in an array cell that was asserted to be non-null.

  sql"select array['foo','bar','baz']".query[Option[List[String]]].quick.unsafeRunSync
  //   Some(List(foo, bar, baz))

  // sql"select array['foo',NULL,'baz']".query[Option[List[String]]].quick.unsafeRunSync
  // throws:
  // [error] (run-main-50) doobie.util.invariant$NullableCellRead$:
  // SQL `NULL` appears in an array cell that was asserted to be non-null.

  sql"select array['foo',NULL,'baz']".query[List[Option[String]]].quick.unsafeRunSync
  //   List(Some(foo), None, Some(baz))

  sql"select array['foo',NULL,'baz']".query[Option[List[Option[String]]]].quick.unsafeRunSync
  //   Some(List(Some(foo), None, Some(baz)))
}
