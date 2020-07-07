package quilldocs._04schema

import hutil.stringformat._

import io.getquill._

object Ex03Customization extends hutil.App {

  import quilldocs._

  val ctx = new SqlMirrorContext(PostgresDialect, SnakeCase)
  import ctx._

  case class Product(id: Int, description: String, sku: Long)

  s"$dash10 Customization $dash10".magenta.println()

  s"$dash5 Postgres $dash5".green.println()

  // The returning and returningGenerated methods also support arithmetic operations,
  // SQL UDFs and even entire queries. These are inserted directly into the SQL RETURNING clause.

  // Assuming this basic query:

  val q9 = quote {
    query[Product].insert(_.description -> "My Product", _.sku -> 1011L)
  }
  printAst(q9.ast)

  // Add 100 to the value of id:
  val run9a = ctx.run(q9.returning(r => r.id + 100)) //: List[Int]
  printStatement(run9a.string)
  // INSERT INTO Product (description, sku) VALUES (?, ?) RETURNING id + 100

  // Pass the value of id into a UDF:
  val udf   = quote { (i: Int) => infix"myUdf($i)".as[Int] }
  val run9b = ctx.run(q9.returning(r => udf(r.id))) //: List[Int]
  printStatement(run9b.string)
  // INSERT INTO Product (description, sku) VALUES (?, ?) RETURNING myUdf(id)

  // Use the return value of sku to issue a query:
  case class Supplier(id: Int, clientSku: Long)
  val run9c = ctx.run {
    q9.returning(r =>
      query[Supplier]
        .filter(s => s.clientSku == r.sku)
        .map(_.id)
        .max
    )
  } //: List[Option[Long]]
  printStatement(run9c.string)
  // INSERT INTO Product (description,sku) VALUES ('My Product', 1011) RETURNING (SELECT MAX(s.id) FROM Supplier s WHERE s.sku = clientSku)

  // As is typically the case with Quill, you can use all of these features together.

  val run9d = ctx.run {
    q9.returning(r =>
      (
        r.id + 100,
        udf(r.id),
        query[Supplier]
          .filter(s => s.clientSku == r.sku)
          .map(_.id)
          .max
      )
    )
  } // List[(Int, Int, Option[Long])]
  printStatement(run9d.string)
  // INSERT INTO Product (description,sku) VALUES ('My Product', 1011)
  // RETURNING id + 100, myUdf(id), (SELECT MAX(s.id) FROM Supplier s WHERE s.sku = sku)

  s"$dash5 SQL Server $dash5".green.println()

  {
    val ctx = new SqlMirrorContext(SQLServerDialect, SnakeCase)
    import ctx._

    val q1 = quote {
      query[Product]
        .insert(_.description -> "My Product", _.sku -> 1011L)
    }
    printAst(q1.ast, false)

    val run1 = ctx.run(q1.returning(r => r.id + 100)) //: List[Int]
    printStatement(run1.string)
    // INSERT INTO Product (description, sku) OUTPUT INSERTED.id + 100 VALUES (?, ?)

    val q2 = quote {
      query[Product]
        .update(_.description -> "Updated Product", _.sku -> 2022L)
        .returning(r => (r.id, r.description))
    }
    printAst(q2.ast, false)

    val updated = ctx.run(q2)
    printStatement(updated.string)
    // UPDATE Product SET description = 'Updated Product', sku = 2022 OUTPUT INSERTED.id, INSERTED.description
  }
}
