package quilldocs._05queries

import hutil.stringformat._

import io.getquill._

object Queries extends hutil.App {

  import quilldocs._

  val ctx = new SqlMirrorContext(PostgresDialect, SnakeCase)
  import ctx._

  s"$dash10 for-comprehensions $dash10".magenta.println()

  // The overall abstraction of quill queries uses database tables as if they were in-memory collections.
  // Scala for-comprehensions provide syntactic sugar to deal with these kinds of monadic operations:

  case class Person(id: Int, name: String, age: Int)
  case class Contact(personId: Int, phone: String)

  val q = quote {
    for {
      p <- query[Person] if (p.id == 999)
      c <- query[Contact] if (c.personId == p.id)
    } yield {
      (p.name, c.phone)
    }
  }

  printAstAndStatement(q.ast, ctx.run(q).string)
  // SELECT p.name, c.phone FROM Person p, Contact c WHERE (p.id = 999) AND (c.personId = p.id)

  // Quill normalizes the quotation and translates the monadic joins to applicative joins,
  // generating a database-friendly query that avoids nested queries.
}
