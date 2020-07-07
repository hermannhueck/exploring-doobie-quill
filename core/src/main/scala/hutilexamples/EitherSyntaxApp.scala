package hutilexamples

import scala.util.chaining._

import hutil.syntax.either._

object EitherSyntaxApp extends hutil.App {

  Right(42)
    .withLeft[String]
    .leftMap(_.toLowerCase)
    .ensuring(_ == Right(42)) pipe println

  Left("Error")
    .withRight[Int]
    .leftMap(_.toLowerCase)
    .ensuring(_ == Left("error")) pipe println

  println()

  Right(Right(42)).flatten pipe println
  Right(Left("error")).flatten pipe println
  Left(Right(42)).flatten pipe println
  Left(Left("error")).flatten pipe println
}
