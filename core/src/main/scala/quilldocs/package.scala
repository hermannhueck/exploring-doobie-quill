import scala.util.chaining._

package object quilldocs {

  def printAst(ast: Any, withEmptyLine: Boolean = true): Unit = {
    ast pipe println
    if (withEmptyLine) println else print("")
  }

  def printStatement(stmnt: String, withEmptyLine: Boolean = true): Unit = {
    stmnt pipe println
    if (withEmptyLine) println else print("")
  }

  def printAstAndStatement(ast: Any, stmnt: String, withEmptyLine: Boolean = true): Unit = {
    ast pipe println
    stmnt pipe println
    if (withEmptyLine) println else print("")
  }
}
