package quilldocs._02compiletimequotations

import scala.util.chaining._

import hutil.stringformat._

import io.getquill._

object CompileTimeQuotations extends hutil.App {

  val ctx = new SqlMirrorContext(PostgresDialect, SnakeCase)

  import ctx._

  case class Circle(radius: Float)

  s"$dash10 Avoid type widening / type ascription $dash10".magenta.println

  // Avoid type widening (Quoted[Query[Circle]]), or else the quotation will be dynamic.
  val q1: Quoted[Query[Circle]] = quote {
    query[Circle].filter(c => c.radius > 10)
  }

  ctx
    .run(q1) // Dynamic query q1
    .pipe(println)

  // Avoid type widening (Quoted[Query[Circle]]), or else the quotation will be dynamic.
  val q2 = quote {
    query[Circle].filter(c => c.radius > 10)
  }

  ctx
    .run(q2) // Static query q2 (without type ascription)
    .pipe(println)

  s"$dash10 Inline Queries $dash10".magenta.println

  ctx.run(query[Circle].map(_.radius))
  // SELECT r.radius FROM Circle r
}
