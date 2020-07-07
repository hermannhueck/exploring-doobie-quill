package quilldocs._04schema

import hutil.stringformat._

import io.getquill._

object Ex04EmbeddedCaseClasses extends hutil.App {

  import quilldocs._

  val ctx = new SqlMirrorContext(PostgresDialect, SnakeCase)
  import ctx._

  s"$dash10 Embedded case classes $dash10".magenta.println()

  case class Contact(phone: String, address: String) extends Embedded
  case class Person(id: Int, name: String, contact: Contact)
  case class Person2(id: Int, name: String, homeContact: Contact, workContact: Option[Contact])

  printStatement(ctx.run(query[Person]).string)
  // SELECT x.id, x.name, x.phone, x.address FROM Person x

  // Note that default naming behavior uses the name of the nested case class properties.
  // It’s possible to override this default behavior using a custom schema:

  val q = quote {
    querySchema[Person2](
      "Person2",
      _.homeContact.phone          -> "homePhone",
      _.homeContact.address        -> "homeAddress",
      _.workContact.map(_.phone)   -> "workPhone",
      _.workContact.map(_.address) -> "workAddress"
    )
  }

  printStatement(ctx.run(q).string)
}
