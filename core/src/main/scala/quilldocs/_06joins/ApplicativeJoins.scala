package quilldocs._06joins

import scala.util.chaining._

import hutil.stringformat._

import io.getquill._

object ApplicativeJoins extends hutil.App {

  import quilldocs._
  import model._

  val ctx = new SqlMirrorContext(PostgresDialect, SnakeCase)
  import ctx._

  // Applicative joins are useful for joining two tables together, they are straightforward to understand,
  // and typically look good on one line. Quill supports inner, left-outer, right-outer, and full-outer
  // (i.e. cross) applicative joins.

  // Inner Join
  val q1 = quote {
    query[Person].join(query[Address]).on(_.id == _.fk)
  }

  printAstAndStatement(q1.ast, ctx.run(q1).string) //: List[(Person, Address)]
  // SELECT x1.id, x1.name, x2.street, x2.zip, x2.fk
  // FROM Person x1 INNER JOIN Address x2 ON x1.id = x2.fk

  // Left (Outer) Join
  val q2 = quote {
    query[Person].leftJoin(query[Address]).on((p, a) => p.id == a.fk)
  }

  printAstAndStatement(q2.ast, ctx.run(q2).string) //: List[(Person, Option[Address])]
  // Note that when you use named-variables in your comprehension, Quill does its best to honor them in the query.
  // SELECT p.id, p.name, a.street, a.zip, a.fk
  // FROM Person p LEFT JOIN Address a ON p.id = a.fk

  // Right (Outer) Join
  val q3 = quote {
    query[Person].rightJoin(query[Address]).on((p, a) => p.id == a.fk)
  }

  printAstAndStatement(q3.ast, ctx.run(q3).string) //: List[(Option[Person], Address)]
  // SELECT p.id, p.name, a.street, a.zip, a.fk
  // FROM Person p RIGHT JOIN Address a ON p.id = a.fk

  // Full (Outer) Join
  val q4 = quote {
    query[Person].fullJoin(query[Address]).on((p, a) => p.id == a.fk)
  }

  printAstAndStatement(q4.ast, ctx.run(q4).string) //: List[(Option[Person], Option[Address])]
  // SELECT p.id, p.name, a.street, a.zip, a.fk
  // FROM Person p FULL JOIN Address a ON p.id = a.fk

  // What about joining more than two tables with the applicative syntax? Here’s how to do that:

  // All is well for two tables but for three or more, the nesting mess begins:
  val q5 = quote {
    query[Person]
      .join(query[Address])
      .on({ case (p, a) => p.id == a.fk }) // Let's use `case` here to stay consistent
      .join(query[Company])
      .on({ case ((_, a), c) => a.zip == c.zip })
  }

  printAstAndStatement(q5.ast, ctx.run(q5).string) //: List[((Person, Address), Company)]
  // (Unfortunately when you use `case` statements, Quill can't help you with the variables names either!)
  // SELECT x01.id, x01.name, x11.street, x11.zip, x11.fk, x12.name, x12.zip
  // FROM Person x01 INNER JOIN Address x11 ON x01.id = x11.fk INNER JOIN Company x12 ON x11.zip = x12.zip
}
