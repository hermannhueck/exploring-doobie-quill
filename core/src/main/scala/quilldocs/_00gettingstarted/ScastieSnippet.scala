package quilldocs._00gettingstarted

import io.getquill._

object ScastieSnippet extends hutil.App {

  val ctx = new SqlMirrorContext(PostgresDialect, SnakeCase)

  import ctx._

  case class Person(name: String, age: Int)

  val m = ctx.run(query[Person].filter(p => p.name == "John"))

  println(m.string)
}
