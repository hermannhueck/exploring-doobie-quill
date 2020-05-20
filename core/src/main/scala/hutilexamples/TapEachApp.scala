package hutilexamples

import scala.util.chaining._

import hutil.stringformat._

object TapEachApp extends hutil.App {

  s"$dash10 Coll#tapEach".magenta pipe println

  val doubledAndSquared: List[Int] =
    List(1, 2, 3)
      .tapEach(x => println(s"value: $x"))
      .tap(_ => println)
      .map(x => x * 2)
      .tapEach(x => println(s"doubled: $x"))
      .tap(_ => println)
      .map(x => x * x)
      .tapEach(x => println(s"squared: $x"))
}
