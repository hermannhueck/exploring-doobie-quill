package doobiedocs._13unittesting

import org.specs2.mutable.Specification

class AnalysisTestWithSpecs2 extends Specification with doobie.specs2.IOChecker {

  override val transactor = doobiedocs.xa

  import UnitTesting._

  check(trivial)
  check(biggerThan(0))
  check(update("", ""))
}
