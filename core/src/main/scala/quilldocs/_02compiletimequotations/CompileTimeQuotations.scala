package quilldocs._02compiletimequotations

import hutil.stringformat._

import io.getquill._

object CompileTimeQuotations extends hutil.App {

  import quilldocs._

  val ctx = new SqlMirrorContext(PostgresDialect, SnakeCase)
  import ctx._

  case class Circle(radius: Float)

  s"$dash10 Avoid type widening / type ascription $dash10".magenta.println()

  // Avoid type widening (Quoted[Query[Circle]]), or else the quotation will be dynamic.
  val q1: Quoted[Query[Circle]] = quote {
    query[Circle].filter(c => c.radius > 10)
  }
  printAstAndStatement(q1.ast, ctx.run(q1).string) // Dynamic query q1 due to return type ascription

  // Avoid type widening (Quoted[Query[Circle]]), or else the quotation will be dynamic.
  val q2 = quote {
    query[Circle].filter(c => c.radius > 10)
  }
  printAstAndStatement(q2.ast, ctx.run(q2).string) // Static query q2 (without type ascription)

  s"$dash10 Inline Queries $dash10".magenta.println()

  printStatement(ctx.run(query[Circle].map(_.radius)).string)
  // SELECT r.radius FROM Circle r
}
