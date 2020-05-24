package doobiedocs._15extensionspostgresql

import scala.util.chaining._

import hutil.stringformat._

import cats._
import cats.data._
import cats.implicits._

import doobie._
import doobie.implicits._

object ExtensionsForPostgreSQL extends hutil.App {

  import doobiedocs._ // imports Transactor xa + implicit ContextShift cs

  import doobie.postgres._
  import doobie.postgres.implicits._

  s"$dash10 Array Types $dash10".magenta.println

  s"$dash10 Enum Types $dash10".magenta.println

  s"$dash10 Geometric Types $dash10".magenta.println

  s"$dash10 PostGIS Types $dash10".magenta.println

  s"$dash10 Other Nonstandard Types $dash10".magenta.println

  s"$dash10 Extended Error Handling $dash10".magenta.println

  """val p = sql"oops".query[String].unique // this won't work""".yellow.println
  val p = sql"oops".query[String].unique // this won't work
  println

  """p.attempt""".yellow.println
  p.attempt
    .transact(xa)
    .unsafeRunSync // attempt is provided by ApplicativeError instance
    .pipe(println)
  // res2: Either[Throwable, String] = Left(
  //   org.postgresql.util.PSQLException: ERROR: syntax error at or near "oops"
  //   Position: 1
  // ) // attempt is provided by ApplicativeError instance

  """p.attemptSqlState""".yellow.println
  p.attemptSqlState
    .transact(xa)
    .unsafeRunSync // this catches only SQL exceptions
    .pipe(println)
  // res3: Either[SqlState, String] = Left(SqlState("42601")) // this catches only SQL exceptions

  """p.attemptSomeSqlState { case SqlState("42601") => "caught!" }""".yellow.println
  p.attemptSomeSqlState { case SqlState("42601") => "caught!" }
    .transact(xa)
    .unsafeRunSync // catch it
    .pipe(println)
  // res4: Either[String, String] = Left("caught!") // catch it

  """p.attemptSomeSqlState { case sqlstate.class42.SYNTAX_ERROR => "caught!" }""".yellow.println
  p.attemptSomeSqlState { case sqlstate.class42.SYNTAX_ERROR => "caught!" }
    .transact(xa)
    .unsafeRunSync // same, w/constant
    .pipe(println)
  // res5: Either[String, String] = Left("caught!") // same, w/constant

  """p.exceptSomeSqlState { case sqlstate.class42.SYNTAX_ERROR => "caught!".pure[ConnectionIO] }""".yellow.println
  p.exceptSomeSqlState { case sqlstate.class42.SYNTAX_ERROR => "caught!".pure[ConnectionIO] }
    .transact(xa)
    .unsafeRunSync // recover
    .pipe(println)
  // res6: String = "caught!" // recover

  """p.onSyntaxError("caught!".pure[ConnectionIO])""".yellow.println
  p.onSyntaxError("caught!".pure[ConnectionIO])
    .transact(xa)
    .unsafeRunSync // using recovery combinator
    .pipe(println)
  // res7: String = "caught!"

  s"$dash10 Server-Side Statements $dash10".magenta.println

  s"$dash10 LISTEN and NOTIFY $dash10".magenta.println

  s"$dash10 Large Objects $dash10".magenta.println

  s"$dash10 Copy Manager $dash10".magenta.println

  s"$dash10 Fastpath $dash10".magenta.println

  s"$dash10 EXPLAIN/EXPLAIN ANALYZE $dash10".magenta.println
}
