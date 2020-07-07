// See 'book of doobie', chapter 07:
// https://tpolecat.github.io/doobie/docs/07-Updating.html
//
package doobiedocs._07updating

import scala.util.chaining._

import hutil.stringformat._

import cats.instances.list._
import cats.syntax.apply._

import doobie._
import doobie.implicits._

object DDLInsertingAndUpdating extends hutil.App {

  import doobiedocs._ // imports Transactor xa + implicit ContextShift cs

  val y = xa.yolo
  import y._

  s"$dash10 Checking a Query (check prints errors) $dash10".magenta.println()

  val drop =
    sql"""
    DROP TABLE IF EXISTS person
  """.update.run

  val create =
    sql"""
    CREATE TABLE person (
      id   SERIAL,
      name VARCHAR NOT NULL UNIQUE,
      age  SMALLINT
    )
  """.update.run

  (drop, create)
    .mapN(_ + _)
    .transact(xa)
    .unsafeRunSync()
    .pipe(count => println(s"Table PERSON dropped and recreated. Rows updated: $count"))
  // res0: Int = 0

  s"$dash10 Inserting $dash10".magenta.println()

  def insert1(name: String, age: Option[Short]): Update0 =
    sql"insert into person (name, age) values ($name, $age)".update

  insert1("Alice", Some(12))
    .run
    .transact(xa)
    .unsafeRunSync()
    .pipe(count => println(s"Rows updated: $count"))
  // res1: Int = 1
  insert1("Bob", None).quick.unsafeRunSync() // switch to YOLO mode
  //   1 row(s) update

  println()

  case class Person(id: Long, name: String, age: Option[Short])

  sql"select id, name, age from person"
    .query[Person]
    .quick
    .unsafeRunSync()
  //   Person(1,Alice,Some(12))
  //   Person(2,Bob,None)

  s"$dash10 Updating $dash10".magenta.println()

  sql"update person set age = 15 where name = 'Alice'"
    .update
    .quick
    .unsafeRunSync()
  //   1 row(s) updated

  println()

  sql"select id, name, age from person"
    .query[Person]
    .quick
    .unsafeRunSync()
  //   Person(2,Bob,None)
  //   Person(1,Alice,Some(15))

  s"$dash10 Retrieving Results $dash10".magenta.println()

  def insert2(name: String, age: Option[Short]): ConnectionIO[Person] =
    for {
      _  <- sql"insert into person (name, age) values ($name, $age)".update.run
      id <- sql"select lastval()".query[Long].unique
      p  <- sql"select id, name, age from person where id = $id".query[Person].unique
    } yield p

  insert2("Jimmy", Some(42))
    .quick
    .unsafeRunSync()
  //   Person(3,Jimmy,Some(42))

  s"$dash10 withUniqueGeneratedKeys (H2 allows to return only the inserted id) $dash10".magenta.println()

  // Some database (like H2) allow you to return [only] the inserted id, allowing the above operation
  // to be reduced to two statements (see below for an explanation of withUniqueGeneratedKeys).

  def insert2_H2(name: String, age: Option[Short]): ConnectionIO[Person] =
    for {
      id <- sql"insert into person (name, age) values ($name, $age)"
              .update
              .withUniqueGeneratedKeys[Int]("id")
      p  <- sql"select id, name, age from person where id = $id"
              .query[Person]
              .unique
    } yield p

  insert2_H2("Ramone", Some(42)).quick.unsafeRunSync()
  //   Person(4,Ramone,Some(42))

  s"$dash10 withUniqueGeneratedKeys (PostgreSQL - and other DBMS - return the specified columns in one shot) $dash10"
    .magenta
    .println()

  // Other databases (including PostgreSQL) provide a way to do this in one shot
  // by returning multiple specified columns from the inserted row.

  def insert3(name: String, age: Option[Short]): ConnectionIO[Person] = {
    sql"insert into person (name, age) values ($name, $age)"
      .update
      .withUniqueGeneratedKeys("id", "name", "age")
  }

  insert3("Elvis", None)
    .quick
    .unsafeRunSync()
  //   Person(5,Elvis,None)

  s"$dash10 withUniqueGeneratedKeys with updates $dash10".magenta.println()

  val up = {
    sql"update person set age = age + 1 where age is not null"
      .update
      .withGeneratedKeys[Person]("id", "name", "age")
  }

  // Running this process updates all rows with a non-NULL age and returns them.

  up.quick.unsafeRunSync()
  //   Person(1,Alice,Some(16))
  //   Person(3,Jimmy,Some(43))
  //   Person(4,Ramone,Some(43))
  println()
  up.quick.unsafeRunSync() // and again!
  //   Person(1,Alice,Some(17))
  //   Person(3,Jimmy,Some(44))
  //   Person(4,Ramone,Some(44))

  s"$dash10 Batch Updates $dash10".magenta.println()

  // Given some values ...
  val a = 1; val b = "foo"

  // this expression ...
  sql"... $a $b ..."

  // is syntactic sugar for this one, which is an Update applied to (a, b)
  Update[(Int, String)]("... ? ? ...").run((a, b))

  type PersonInfo = (String, Option[Short])

  def insertMany(ps: List[PersonInfo]): ConnectionIO[Int] = {
    val sql = "insert into person (name, age) values (?, ?)"
    Update[PersonInfo](sql).updateMany(ps)
  }

  // Some rows to insert
  val data = List[PersonInfo](("Frank", Some(12)), ("Daddy", None))

  insertMany(data)
    .quick
    .unsafeRunSync()
  //   2

  import fs2.Stream

  def insertMany2(ps: List[PersonInfo]): Stream[ConnectionIO, Person] = {
    val sql = "insert into person (name, age) values (?, ?)"
    Update[PersonInfo](sql).updateManyWithGeneratedKeys[Person]("id", "name", "age")(ps)
  }

  s"$dash10 Batch Updates (using updateManyWithGeneratedKeys) $dash10".magenta.println()

  // Some rows to insert
  val data2 = List[PersonInfo](("Banjo", Some(39)), ("Skeeter", None), ("Jim-Bob", Some(12)))

  insertMany2(data2)
    .quick
    .unsafeRunSync()
  //   Person(8,Banjo,Some(39))
  //   Person(9,Skeeter,None)
  //   Person(10,Jim-Bob,Some(12))

  s"$dash10 Deleting $dash10".magenta.println()

  sql"delete from person"
    .update
    .run
    .transact(xa)
    .unsafeRunSync()
    .pipe(count => println(s"Rows deleted: $count"))

  sql"delete from person" // with yolo
    .update
    .quick
    .unsafeRunSync()
}
