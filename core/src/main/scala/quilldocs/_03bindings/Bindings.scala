package quilldocs._03bindings

import scala.util.chaining._

import hutil.stringformat._

import io.getquill._

object Bindings extends hutil.App {

  import quilldocs._

  val ctx = new SqlMirrorContext(PostgresDialect, SnakeCase)
  import ctx._

  case class Circle(radius: Float)

  s"$dash10 lifted values $dash10".magenta.println()

  // A runtime value can be lifted to a quotation through the method lift:

  def biggerThan(i: Float) =
    quote {
      query[Circle].filter(r => r.radius > lift(i))
    }
  printStatement(ctx.run(biggerThan(10)).string)
  // SELECT r.radius FROM Circle r WHERE r.radius > ?

  s"$dash10 lifted queries: $dash10".magenta.println()

  // A Iterable instance can be lifted as a Query.
  // There are two main usages for lifted queries:

  s"$dash5 1. contains $dash5".green.println()

  def find(radiusList: List[Float]) =
    quote {
      query[Circle].filter(r => liftQuery(radiusList).contains(r.radius))
    }
  printStatement(ctx.run(find(List(1.1f, 1.2f))).string)
  // SELECT r.radius FROM Circle r WHERE r.radius IN (?)

  s"$dash5 2. batch action $dash5".green.println()

  def insert(circles: List[Circle]) =
    quote {
      liftQuery(circles).foreach(c => query[Circle].insert(c))
    }
  ctx
    .run(insert(List(Circle(1.1f), Circle(1.2f)))) pipe println
  // INSERT INTO Circle (radius) VALUES (?)
  println()
}
