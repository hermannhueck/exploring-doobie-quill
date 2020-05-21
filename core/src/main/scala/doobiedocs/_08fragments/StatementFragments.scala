package doobiedocs._08fragments

import scala.util.chaining._

import hutil.stringformat._

import cats._
import cats.data._
import cats.effect._
import cats.implicits._

import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts

object StatementFragments extends hutil.App {

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

  s"$dash10 Composing SQL literals $dash10".magenta.println

  val a = fr"select name from country"
  // a: Fragment = Fragment("select name from country ")
  val b = fr"where code = 'USA'"
  // b: Fragment = Fragment("where code = 'USA' ")
  val c = a ++ b // concatenation by ++
  // c: Fragment = Fragment("select name from country where code = 'USA' ") // concatenation by ++
  c.query[String].unique.quick.unsafeRunSync
  //   United States

  s"$dash10 Fragments can capture arguments of any type $dash10".magenta.println

  def whereCode(s: String) = fr"where code = $s"

  val fra = whereCode("FRA")
  // fra: Fragment = Fragment("where code = ? ")

  (fr"select name from country" ++ fra).query[String].quick.unsafeRunSync
  //   France

  s"$dash10 Lift an arbitrary string value via Fragment.const $dash10".magenta.println

  def count(table: String) =
    (fr"select count(*) from" ++ Fragment.const(table)).query[Int].unique

  count("city").quick.unsafeRunSync
  //   4079

  // Note that Fragment.const performs no escaping of passed strings.
  // Passing user-supplied data is an injection risk.

  s"$dash10 Whitespace handling $dash10".magenta.println

  fr"IN (" ++ List(1, 2, 3).map(n => fr"$n").intercalate(fr",") ++ fr")" pipe println
  // res3: Fragment = Fragment("IN ( ? , ? , ? ) ")

  fr0"IN (" ++ List(1, 2, 3).map(n => fr0"$n").intercalate(fr",") ++ fr")" pipe println
  // res4: Fragment = Fragment("IN (?, ?, ?) ")

  // Note that the sql interpolator is simply an alias for fr0.

  s"$dash10 The Fragments Module $dash10".magenta.println

  // Import some convenience combinators.
  import Fragments.{in, whereAndOpt}

  // Country Info
  case class Info(name: String, code: String, population: Int)

  // Construct a Query0 with some optional filter conditions and a configurable LIMIT.
  def select(name: Option[String], pop: Option[Int], codes: List[String], limit: Long): Query0[Info] = {

    // Three Option[Fragment] filter conditions.
    val f1 = name.map(s => fr"name LIKE $s")
    val f2 = pop.map(n => fr"population > $n")
    val f3 = codes.toNel.map(cs => in(fr"code", cs))

    // Our final query
    val q: Fragment =
      fr"SELECT name, code, population FROM country" ++
        whereAndOpt(f1, f2, f3) ++
        fr"LIMIT $limit"

    // Construct a Query0
    q.query[Info]
  }

  println
  select(None, None, Nil, 10) // no filters
    .check
    .unsafeRunSync
  //   Query0[Session.App.Info] defined at 08-Fragments.md:116
  //   SELECT name, code, population FROM country LIMIT ?
  //   ✓ SQL Compiles and TypeChecks
  //   ✓ P01 Long  →  BIGINT (int8)
  //   ✓ C01 name       VARCHAR (varchar) NOT NULL  →  String
  //   ✓ C02 code       CHAR    (bpchar)  NOT NULL  →  String
  //   ✓ C03 population INTEGER (int4)    NOT NULL  →  Int // no filters

  println
  select(Some("U%"), None, Nil, 10) // one filter
    .check
    .unsafeRunSync
  //   Query0[Session.App.Info] defined at 08-Fragments.md:116
  //   SELECT name, code, population FROM country WHERE (name LIKE ? )
  //   LIMIT ?
  //   ✓ SQL Compiles and TypeChecks
  //   ✓ P01 String  →  VARCHAR (text)
  //   ✓ P02 Long    →  BIGINT  (int8)
  //   ✓ C01 name       VARCHAR (varchar) NOT NULL  →  String
  //   ✓ C02 code       CHAR    (bpchar)  NOT NULL  →  String
  //   ✓ C03 population INTEGER (int4)    NOT NULL  →  Int // one filter

  println
  select(Some("U%"), Some(12345), List("FRA", "GBR"), 10) // three filters
    .check
    .unsafeRunSync
  //   Query0[Session.App.Info] defined at 08-Fragments.md:116
  //   SELECT name, code, population FROM country WHERE (name LIKE ? ) AND
  //   (population > ? ) AND (code IN (?, ?) ) LIMIT ?
  //   ✓ SQL Compiles and TypeChecks
  //   ✓ P01 String  →  VARCHAR (text)
  //   ✓ P02 Int     →  INTEGER (int4)
  //   ✓ P03 String  →  CHAR    (bpchar)
  //   ✓ P04 String  →  CHAR    (bpchar)
  //   ✓ P05 Long    →  BIGINT  (int8)
  //   ✓ C01 name       VARCHAR (varchar) NOT NULL  →  String
  //   ✓ C02 code       CHAR    (bpchar)  NOT NULL  →  String
  //   ✓ C03 population INTEGER (int4)    NOT NULL  →  Int
}
