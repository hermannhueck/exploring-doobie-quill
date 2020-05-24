// See 'book of doobie', chapter 11:
// https://tpolecat.github.io/doobie/docs/11-Arrays.html
//
package doobiedocs._11arrays

import hutil.stringformat._

import doobie._
import doobie.implicits._

object SQLArrays extends hutil.App {

  import doobiedocs._ // imports Transactor xa + implicit ContextShift cs

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
