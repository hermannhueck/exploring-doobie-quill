// See 'book of doobie', chapter 13:
// https://tpolecat.github.io/doobie/docs/13-Unit-Testing.html
//
package doobiedocs._13unittesting

import hutil.stringformat._

import doobie._
import doobie.implicits._

object UnitTesting extends hutil.App {

  import doobiedocs._ // imports Transactor xa + implicit ContextShift cs

  val y = xa.yolo
  import y._

  case class Country(code: Int, name: String, pop: Int, gnp: Double)

  lazy val trivial =
    sql"""|
          |select 42, 'foo'::varchar
          |"""
      .stripMargin
      .query[(Int, String)]

  def biggerThan(minPop: Short) =
    sql"""|
          |select code, name, population, gnp, indepyear
          |from country
          |where population > $minPop
          |"""
      .stripMargin
      .query[Country]

  def update(oldName: String, newName: String) =
    sql"""|
          |update country set name = $newName where name = $oldName
          |""".stripMargin.update

  s"$dash10 The Specs2 Package $dash10".magenta.println

  // The doobie-specs2 add-on provides a mix-in trait that we can add to a Specification
  // to allow for typechecking of queries, interpreted as a set of specifications.

  // Our unit test needs to extend AnalysisSpec and must define a Transactor[IO].
  // To construct a testcase for a query, pass it to the check method.
  // Note that query arguments are never used, so they can be any values that typecheck.

  import org.specs2.mutable.Specification

  class AnalysisTestWithSpecs2 extends Specification with doobie.specs2.IOChecker {

    val transactor = doobiedocs.xa

    check(trivial)
    check(biggerThan(0))
    check(update("", ""))
  }

  import _root_.specs2.{run => runTest}
  import _root_.org.specs2.main.{Arguments, Report}

  // Run a test programmatically. Usually you would do this from sbt, bloop, etc.
  runTest(new AnalysisTestWithSpecs2)(Arguments(report = Report(_color = Some(false))))

  s"$dash10 The ScalaTest Package $dash10".magenta.println

  // The doobie-scalatest add-on provides a mix-in trait that we can add to any Assertions implementation
  // (like FunSuite) much like the Specs2 package above.

  import org.scalatest._

  class AnalysisTestWithScalaTest
      extends funsuite.AnyFunSuite
      with matchers.should.Matchers
      with doobie.scalatest.IOChecker {

    // override val colors = doobie.util.Colors.None // just for docs

    val transactor = doobiedocs.xa

    test("trivial") {
      check(trivial)
    }
    test("biggerThan") {
      check(biggerThan(0))
    }
    test("update") {
      check(update("", ""))
    }
  }

  (new AnalysisTestWithScalaTest).execute(color = false)
}
