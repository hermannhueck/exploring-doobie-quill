package doobiedocs._13unittesting

import org.scalatest._

class AnalysisTestWithScalaTest
    extends funsuite.AnyFunSuite
    with matchers.should.Matchers
    with doobie.scalatest.IOChecker {

  // override val colors = doobie.util.Colors.None // just for docs

  val transactor = doobiedocs.xa

  import UnitTesting._

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
