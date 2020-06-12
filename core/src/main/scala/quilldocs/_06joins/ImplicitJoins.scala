package quilldocs._06joins

import scala.util.chaining._

import hutil.stringformat._

import io.getquill._

object ImplicitJoins extends hutil.App {

  import quilldocs._
  import model._

  val ctx = new SqlMirrorContext(PostgresDialect, SnakeCase)
  import ctx._

  // Quill’s implicit joins use a monadic syntax making them pleasant to use for joining
  // many tables together. They look a lot like Scala collections when used in for-comprehensions
  // making them familiar to a typical Scala developer. What’s the catch? They can only do inner-joins.

  val q1 = quote {
    for {
      p <- query[Person]
      a <- query[Address] if (p.id == a.fk)
    } yield (p, a)
  }

  printAstAndStatement(q1.ast, ctx.run(q1).string) //: List[(Person, Address)]
  // SELECT p.id, p.name, a.street, a.zip, a.fk
  // FROM Person p, Address a WHERE p.id = a.fk

  // Now, this is great because you can keep adding more and more joins without having to do any pesky nesting.

  val q2 = quote {
    for {
      p <- query[Person]
      a <- query[Address] if (p.id == a.fk)
      c <- query[Address] if (c.zip == a.zip)
    } yield (p, a, c)
  }

  printAstAndStatement(q2.ast, ctx.run(q2).string) //: List[(Person, Address, Company)]
  // SELECT p.id, p.name, a.street, a.zip, a.fk, c.name, c.zip
  // FROM Person p, Address a, Company c WHERE p.id = a.fk AND c.zip = a.zip

  // Well that looks nice but wait! What If I need to inner, and outer join lots of tables nicely?
  // No worries, flat-joins are here to help!
}
