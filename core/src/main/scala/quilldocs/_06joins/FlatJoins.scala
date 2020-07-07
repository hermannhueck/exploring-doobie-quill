package quilldocs._06joins

import io.getquill._

object FlatJoins extends hutil.App {

  import quilldocs._
  import model._

  val ctx = new SqlMirrorContext(PostgresDialect, SnakeCase)
  import ctx._

  // Flat Joins give you the best of both worlds! In the monadic syntax, you can use both inner joins,
  // and left-outer joins together without any of that pesky nesting.

  val q1 = quote {
    for {
      p <- query[Person]
      a <- query[Address].join(a => a.fk == p.id)
    } yield (p, a)
  }

  printAstAndStatement(q1.ast, ctx.run(q1).string) //: List[(Person, Address)]
  // SELECT p.id, p.name, a.street, a.zip, a.fk, c.name, c.zip
  // FROM Person p, Address a, Company c WHERE p.id = a.fk AND c.zip = a.zip

  // Now, this is great because you can keep adding more and more joins without having to do any pesky nesting.

// Left (Outer) Join
  val q2 = quote {
    for {
      p <- query[Person]
      a <- query[Address].leftJoin(a => a.fk == p.id)
    } yield (p, a)
  }

  printAstAndStatement(q2.ast, ctx.run(q2).string) //: List[(Person, Option[Address])]
  // SELECT p.id, p.name, a.street, a.zip, a.fk
  // FROM Person p LEFT JOIN Address a ON a.fk = p.id

  // Now you can keep adding both right and left joins without nesting!

  val q3 = quote {
    for {
      p <- query[Person]
      a <- query[Address].join(a => a.fk == p.id)
      c <- query[Company].leftJoin(c => c.zip == a.zip)
    } yield (p, a, c)
  }

  printAstAndStatement(q3.ast, ctx.run(q3).string) //: List[(Person, Address, Option[Company])]
  // SELECT p.id, p.name, a.street, a.zip, a.fk, c.name, c.zip
  // FROM Person p
  // INNER JOIN Address a ON a.fk = p.id
  // LEFT JOIN Company c ON c.zip = a.zip

  // Can’t figure out what kind of join you want to use? Who says you have to choose?
  // With Quill the following multi-join queries are equivalent, use them according to preference:

  val qFlat = quote {
    for {
      (p, e) <- query[Person].join(query[Employer]).on(_.id == _.personId)
      c      <- query[Contact].leftJoin(_.personId == p.id)
    } yield (p, e, c)
  }

  printAstAndStatement(qFlat.ast, ctx.run(qFlat).string)
  // SELECT p.id, p.name, p.age, e.id, e.personId, e.name, c.id, c.phone
  // FROM Person p INNER JOIN Employer e ON p.id = e.personId LEFT JOIN Contact c ON c.personId = p.id

  val qNested = quote {
    for {
      ((p, e), c) <- query[Person]
                       .join(query[Employer])
                       .on(_.id == _.personId)
                       .leftJoin(query[Contact])
                       .on(
                         _._1.id == _.personId
                       )
    } yield (p, e, c)
  }

  printAstAndStatement(qNested.ast, ctx.run(qNested).string)
  // SELECT p.id, p.name, p.age, e.id, e.personId, e.name, c.id, c.phone
  // FROM Person p INNER JOIN Employer e ON p.id = e.personId LEFT JOIN Contact c ON c.personId = p.id

  // Note that in some cases implicit and flat joins cannot be used together,
  // for example, the following query will fail.

  @annotation.nowarn("cat=unused-params")
  val q = quote {
    for {
      p  <- query[Person]
      p1 <- query[Person] if (p1.name == p.name)
      c  <- query[Contact].leftJoin(_.personId == p.id)
    } yield (p, c)
  }

  // ctx.run(q)
  // java.lang.IllegalArgumentException: requirement failed: Found an `ON` table reference of a table that is
  // not available: Set(p). The `ON` condition can only use tables defined through explicit joins.

  // This happens because an explicit join typically cannot be done after an implicit join in the same query.

  // A good guideline is in any query or subquery, choose one of the following:
  // Use flat-joins + applicative joins or Use implicit joins

  // Also, note that not all Option operations are available on outer-joined tables (i.e. tables wrapped
  // in an Option object), only a specific subset. This is mostly due to the inherent limitations of SQL itself.
  // For more information, see the ‘Optional Tables’ section.

}
