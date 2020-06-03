package quilldocs._01introduction

import scala.util.chaining._

import io.getquill._

object Introduction extends hutil.App {

  val ctx = new SqlMirrorContext(PostgresDialect, SnakeCase)

  import ctx._

  // val pi: Quoted[Double]
  val pi =
    quote(3.14159)
  pi.ast pipe println
  ctx.run(pi).string pipe println
  println

  case class Circle(radius: Float)

  // val areas1: Quoted[EntityQuery[Double]]
  val areas1 = quote {
    query[Circle].map(c => pi * c.radius * c.radius)
  }
  areas1.ast pipe println
  ctx.run(areas1).string pipe println
  println

  // val area: Quoted[Circle => Double]
  val area = quote { (c: Circle) =>
    {
      val r2 = c.radius * c.radius
      pi * r2
    }
  }
  area.ast pipe println
  println

  // val areas2: Quoted[EntityQuery[Double]]
  val areas2 = quote {
    query[Circle].map(c => area(c))
  }
  areas2.ast pipe println
  ctx.run(areas2).string pipe println
  println

  val areasNormalized = quote {
    query[Circle].map(c => 3.14159 * c.radius * c.radius)
  }
  areasNormalized.ast pipe println
  ctx.run(areasNormalized).string pipe println
  println

  // def existsAny[T]: Quoted[Query[T] => ((T => Boolean) => Boolean)]
  def existsAny[T] =
    quote { (q: Query[T]) => (p: T => Boolean) =>
      q.filter(p(_)).nonEmpty
    }
  existsAny.ast pipe println
  println

  // val q: Quoted[EntityQuery[Circle]]
  val q = quote {
    query[Circle].filter { c1 => existsAny(query[Circle])(c2 => c2.radius > c1.radius) }
  }
  q.ast pipe println
  ctx.run(q).string pipe println
  println
}
