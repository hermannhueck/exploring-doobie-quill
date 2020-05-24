package doobiedocs._17quill

import scala.util.chaining._

import hutil.stringformat._

import doobie._
import doobie.implicits._

object QuillIntegration extends hutil.App {

  import io.getquill.{idiom => _, _}
  import doobie.quill.DoobieContext

  // We can now construct a DoobieContext for our back-end database and import its members,
  // as we would with a traditional Quill context. The options are H2, MySQL, Oracle, Postgres,
  // SQLite, and SQLServer.

  val dc = new DoobieContext.Postgres(Literal) // Literal naming scheme
  import dc._

  case class Country(code: String, name: String, population: Int)

  import doobiedocs._

  s"$dash10 Examples: Query $dash10".magenta.println

  val q1 = quote { query[Country].filter(_.code == "GBR") }
  // q1: AnyRef with Quoted[EntityQuery[Country]]{def quoted: io.getquill.ast.Filter;def ast: io.getquill.ast.Filter;def id50738446(): Unit;val liftings: Object} = querySchema("Country").filter(x2 => x2.code == "GBR")

  // Select all at once
  val res0: ConnectionIO[List[Country]] = run(q1)
  // res0: doobie.package.ConnectionIO[List[Country]] = Suspend(
  //   BracketCase(
  //     Suspend(
  //       PrepareStatement(
  //         "SELECT x2.code, x2.name, x2.population FROM Country x2 WHERE x2.code = 'GBR'"
  //       )
  //     ),
  //     doobie.hi.connection$$$Lambda$8172/902966097@1117263f,
  //     cats.effect.Bracket$$Lambda$8174/1752249240@6fd4d6c3
  //   )
  // )
  run(q1)
    .transact(xa)
    .unsafeRunSync pipe println

  // Stream in chunks of 16
  val res1: fs2.Stream[ConnectionIO, Country] = stream(q1, 16)
  // res1: fs2.Stream[doobie.package.ConnectionIO, Country] = Stream(..)
  stream(q1, 16)
    .compile
    .toList
    .transact(xa)
    .unsafeRunSync pipe println

  s"$dash10 Examples: Update $dash10".magenta.println

  val u1 = quote { query[Country].filter(_.name like "U%").update(_.name -> "foo") }
  // u1: AnyRef with Quoted[Update[Country]]{def quoted: io.getquill.ast.Update;def ast: io.getquill.ast.Update;def id1396135128(): Unit;val liftings: Object} = querySchema("Country").filter(x3 => infix"${x3.name} like ${"U%"}").update(x4 => x4.name -> "foo")

  // Update yielding count of affected rows
  val res2: ConnectionIO[Long] = run(u1)
  // res2: doobie.package.ConnectionIO[Long] = Suspend(
  //   BracketCase(
  //     Suspend(
  //       PrepareStatement("UPDATE Country SET name = 'foo' WHERE name like 'U%'")
  //     ),
  //     doobie.hi.connection$$$Lambda$8172/902966097@1713a04c,
  //     cats.effect.Bracket$$Lambda$8174/1752249240@8502c3a
  //   )
  // )

  s"$dash10 Examples: Batch Update $dash10".magenta.println

  val u2 = quote {
    liftQuery(List("U%", "A%")).foreach { pat => query[Country].filter(_.name like pat).update(_.name -> "foo") }
  }
  // u2: AnyRef with Quoted[BatchAction[Update[Country]]]{def quoted: io.getquill.ast.Foreach;def ast: io.getquill.ast.Foreach;def id519296784(): Unit;val liftings: AnyRef{val scala.collection.immutable.List.apply[String]("U%", "A%"): io.getquill.quotation.ScalarValueLifting[List[String],String]}} = ?.foreach((pat) => querySchema("Country").filter(x5 => infix"${x5.name} like $pat").update(x6 => x6.name -> "foo"))

  // Update yielding list of counts of affected rows
  val res3: ConnectionIO[List[Long]] = run(u2)
  // res3: doobie.package.ConnectionIO[List[Long]] = FlatMapped(
  //   FlatMapped(
  //     Suspend(
  //       BracketCase(
  //         Suspend(
  //           PrepareStatement("UPDATE Country SET name = 'foo' WHERE name like ?")
  //         ),
  //         doobie.hi.connection$$$Lambda$8172/902966097@66e96a2,
  //         cats.effect.Bracket$$Lambda$8174/1752249240@48101194
  //       )
  //     ),
  //     cats.FlatMap$$Lambda$8343/722281023@428bd91a
  //   ),
  //   cats.Monad$$Lambda$8144/773217938@4bb31c8d
  // )

  s"$dash10 Examples: Update returning a single generated key $dash10".magenta.println

  // CREATE TABLE Foo (
  //   id    SERIAL,
  //   value VARCHAR(42)
  // )

  case class Foo(id: Int, value: String)

  val u3 = quote {
    query[Foo]
      .insert(lift(Foo(0, "Joe")))
      .returning(_.id)
  }
  // u3: AnyRef with Quoted[ActionReturning[Foo, Int]]{def quoted: io.getquill.ast.Returning;def ast: io.getquill.ast.Returning;def id1302982979(): Unit;val liftings: AnyRef{val App.this.Foo.apply(0, "Joe").id: io.getquill.quotation.ScalarValueLifting[Int,Int]; val App.this.Foo.apply(0, "Joe").value: io.getquill.quotation.ScalarValueLifting[String,String]}} = querySchema("Foo").insert(v => v.id -> ?, v => v.value -> ?).returning((x7) => x7.id)

  // Update yielding a single id
  val res4: ConnectionIO[Int] = run(u3)
  // res4: doobie.package.ConnectionIO[Int] = Suspend(
  //   BracketCase(
  //     Suspend(
  //       PrepareStatement3(
  //         "INSERT INTO Foo (id,value) VALUES (?, ?) RETURNING id",
  //         1
  //       )
  //     ),
  //     doobie.hi.connection$$$Lambda$9198/581532532@399a48df,
  //     cats.effect.Bracket$$Lambda$8174/1752249240@714e72c9
  //   )
  // )

  s"$dash10 Examples: Batch Update returning multiple generated keys $dash10".magenta.println

  val u4 = quote {
    liftQuery(List(Foo(0, "Joe"), Foo(0, "Bob")))
      .foreach { a =>
        query[Foo]
          .insert(a)
          .returning(_.id)
      }
  }
  // u4: AnyRef with Quoted[BatchAction[ActionReturning[Foo, Int]]]{def quoted: io.getquill.ast.Foreach;def ast: io.getquill.ast.Foreach;def id1086843774(): Unit;val liftings: AnyRef{val scala.collection.immutable.List.apply[repl.Session.App.Foo](App.this.Foo.apply(0, "Joe"), App.this.Foo.apply(0, "Bob")): io.getquill.quotation.CaseClassValueLifting[List[repl.Session.App.Foo]]}} = ?.foreach((a) => querySchema("Foo").insert(v => v.id -> a.id, v => v.value -> a.value).returning((x8) => x8.id))

  // Update yielding a list of ids
  val res5: ConnectionIO[List[Int]] = run(u4)
  // res5: doobie.package.ConnectionIO[List[Int]] = FlatMapped(
  //   FlatMapped(
  //     Suspend(
  //       BracketCase(
  //         Suspend(
  //           PrepareStatement3(
  //             "INSERT INTO Foo (id,value) VALUES (?, ?) RETURNING id",
  //             1
  //           )
  //         ),
  //         doobie.hi.connection$$$Lambda$9198/581532532@6d1126f3,
  //         cats.effect.Bracket$$Lambda$8174/1752249240@3e9518a8
  //       )
  //     ),
  //     cats.FlatMap$$Lambda$8343/722281023@55227315
  //   ),
  //   cats.Monad$$Lambda$8144/773217938@57789b42
  // )
}
