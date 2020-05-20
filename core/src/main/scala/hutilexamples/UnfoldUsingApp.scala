package hutilexamples

import java.io.{BufferedReader, FileReader}

import scala.util.Using
import scala.util.chaining._

import hutil.stringformat._

object UnfoldUsingApp extends hutil.App {

  val unfoldFunction: Int => Option[(Int, Int)] = {
    case 0 => None
    case s => Some(((s * s), (s - 1)))
  }

  s"$dash10 List.unfold".magenta.println

  List.unfold(10)(unfoldFunction) pipe println

  s"$dash10 Iterator.unfold + Using".magenta.println

  def bufferedReader(fileName: String): BufferedReader =
    new BufferedReader(new FileReader(fileName))

  def readLines(reader: BufferedReader): Seq[String] =
    Iterator
      .unfold(()) { _ => Option { reader.readLine() }.map(x => (x, ())) }
      .toList

  val lines: Seq[String] =
    Using.resource(bufferedReader("README.md"))(readLines)

  lines foreach println
}
