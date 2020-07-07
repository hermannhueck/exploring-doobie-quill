package quilldocs._04schema

import hutil.stringformat._

import io.getquill._

object Ex01SchemaCustomization extends hutil.App {

  import quilldocs._

  val ctx = new SqlMirrorContext(PostgresDialect, SnakeCase)
  import ctx._

  case class Circle(radius: Float)
  case class Rectangle(length: Int, width: Int)

  s"$dash10 Schema $dash10".magenta.println()

  // The database schema is represented by case classes. By default, quill uses
  // the class and field names as the database identifiers:

  val q1 = quote {
    query[Circle].filter(c => c.radius > 1)
  }
  printAstAndStatement(q1.ast, ctx.run(q1).string)
  // SELECT c.radius FROM Circle c WHERE c.radius > 1

  s"$dash10 Schema customization $dash10".magenta.println()

  val circles = quote {
    querySchema[Circle]("circle_table", _.radius -> "radius_column")
  }

  val q2 = quote {
    circles.filter(c => c.radius > 1)
  }
  printAstAndStatement(q2.ast, ctx.run(q2).string)
  // SELECT c.radius_column FROM circle_table c WHERE c.radius_column > 1

  object schema {
    val circles    = quote {
      querySchema[Circle]("circle_table", _.radius -> "radius_column")
    }
    val rectangles = quote {
      querySchema[Rectangle]("rectangle_table", _.length -> "length_column", _.width -> "width_column")
    }
  }
}
