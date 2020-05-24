package doobiedocs._12custommappings

import java.awt.Point

import scala.util.chaining._

import hutil.stringformat._

import cats.Show
import cats.data.NonEmptyList
import cats.syntax.show._
import cats.syntax.either._

import doobie._
import doobie.implicits._

import io.circe._
import io.circe.jawn._
import io.circe.syntax._
import org.postgresql.util.PGobject

object CustomMappings extends hutil.App {

  s"$dash10 When do I need a custom type mapping? $dash10".magenta.println

  // The first case appears when you try to use an unmapped type as a statement parameter.

  // def nope(msg: String, ex: Exception): ConnectionIO[Int] =
  //   sql"INSERT INTO log (message, detail) VALUES ($msg, $ex)".update.run

  // error:
  // Cannot construct a parameter vector of the following type:
  //
  //   String :: Exception :: shapeless.HNil
  //
  // Because one or more types therein (disregarding HNil) does not have a Put
  // instance in scope. Try them one by one in the REPL or in your code:
  //
  //   scala> Put[Foo]
  //
  // and find the one that has no instance, then construct one as needed. Refer to
  // Chapter 12 of the book of doobie for more information.
  //
  //   sql"INSERT INTO log (message, detail) VALUES ($msg, $ex)".update.run
  //   ^^^

  // error in 2.13.2 and doobie 0.9.0:
  // [error]      type mismatch;
  // [error]       found   : Exception
  // [error]       required: doobie.syntax.SqlInterpolator.SingleFragment
  // [error]      L38:     sql"INSERT INTO log (message, detail) VALUES ($msg, $ex)".update.run
  // [error]                                                                    ^

  // The second common case is when we try to read rows into a data type that includes an unmapped member type, such as this one.

  case class LogEntry(msg: String, ex: Exception)

  // When we attept to define a Query0[LogEntry] we get a type error similar to the one above.

  // sql"SELECT message, detail FROM log".query[LogEntry]
  // error:
  // Cannot find or construct a Read instance for type:
  //
  //   repl.Session.App.LogEntry
  //
  // This can happen for a few reasons, but the most common case is that a data
  // member somewhere within this type doesn't have a Get instance in scope. Here are
  // some debugging hints:
  //
  // - For Option types, ensure that a Read instance is in scope for the non-Option
  //   version.
  // - For types you expect to map to a single column ensure that a Get instance is
  //   in scope.
  // - For case classes, HLists, and shapeless records ensure that each element
  //   has a Read instance in scope.
  // - Lather, rinse, repeat, recursively until you find the problematic bit.
  //
  // You can check that an instance exists for Read in the REPL or in your code:
  //
  //   scala> Read[Foo]
  //
  // and similarly with Get:
  //
  //   scala> Get[Foo]
  //
  // And find the missing instance and construct it as needed. Refer to Chapter 12
  // of the book of doobie for more information.
  //
  // sql"SELECT message, detail FROM log".query[LogEntry]
  // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

  s"$dash10 Single-Column Type Mappings $dash10".magenta.println

  s"$dash5 Deriving Get and Put from Existing Instances $dash5".yellow.println

  object NatModule {

    sealed trait Nat
    case object Zero        extends Nat
    case class Succ(n: Nat) extends Nat

    def toInt(n: Nat): Int = {
      def go(n: Nat, acc: Int): Int =
        n match {
          case Zero    => acc
          case Succ(n) => go(n, acc + 1)
        }
      go(n, 0)
    }

    def fromInt(n: Int): Nat = {
      def go(n: Int, acc: Nat): Nat =
        if (n <= 0) acc else go(n - 1, Succ(acc))
      go(n, Zero)
    }

  }

  import NatModule._

  // There is no direct schema mapping for Nat, but there is a schema mapping for Int
  // that we get out of the box, and we can use it to define our mapping for Nat.

  // Bidirectional schema mapping for Nat, in terms of Int
  implicit val natGet: Get[Nat] = Get[Int].map(fromInt)
  implicit val natPut: Put[Nat] = Put[Int].contramap(toInt)

  // Prefer .tmap and .tcontramap when possible.
  implicit val natGet2: Get[Nat] = Get[Int].tmap(fromInt)
  implicit val natPut2: Put[Nat] = Put[Int].tcontramap(toInt)

  s"$dash5 Deriving Get and Put from Meta $dash5".yellow.println

  // Bidirectional schema mapping for Nat, in terms of Int
  implicit val natMeta: Meta[Nat] = Meta[Int].imap(fromInt)(toInt)

  // Prefer .timap when possible.
  implicit val natMeta2: Meta[Nat] = Meta[Int].timap(fromInt)(toInt)

  s"$dash5 Defining Get and Put for Exotic Types $dash5".yellow.println

  // In this example we will create a mapping for PostgreSQL’s json type,
  // which is not part of the JDBC specification. On the Scala side we will use
  // the Json type from Circe. The PostgreSQL JDBC driver transfers json values
  // via the JDBC type OTHER, with an uwrapped payload type PGobject. The only way
  // to know this is by experimentation. You can expect to get this kind of mapping wrong
  // a few times before it starts working. In any case the OTHER type is commonly used
  // for nonstandard types and doobie provides a way to construct such mappings.

  implicit val showPGobject: Show[PGobject] = Show.show(_.getValue.take(250))

  implicit val jsonGet: Get[Json] =
    Get
      .Advanced
      .other[PGobject](NonEmptyList.of("json"))
      .temap[Json] { o => parse(o.getValue).leftMap(_.show) }

  implicit val jsonPut: Put[Json] =
    Put
      .Advanced
      .other[PGobject](NonEmptyList.of("json"))
      .tcontramap[Json] { j =>
        val pgo = new PGobject
        pgo.setType("json")
        pgo.setValue(j.noSpaces)
        pgo
      }

  // As above, with bidirectional mappings it’s usually more convenient to use Meta,
  // which provides an other constructor allowing the operations above to be combined.

  val pgObject2Json: PGobject => Json = a =>
    parse(a.getValue)
      .leftMap[Json](e => throw e) // scalafix:ok DisableSyntax.throw
      .merge

  val json2PGObject: Json => PGobject = a => {
    val o = new PGobject
    o.setType("json")
    o.setValue(a.noSpaces)
    o
  }

  implicit val jsonMeta: Meta[Json] =
    Meta
      .Advanced
      .other[PGobject]("json")
      .timap[Json](pgObject2Json)(json2PGObject)

  s"$dash10 Column Vector Mappings $dash10".magenta.println

  s"$dash5 Deriving Read and Write from Existing Instances $dash5".yellow.println

  // Consider the Point class from Java AWT, which is logically a pair of Ints
  // but is not a case class and is thus not eligable for automatic derivation
  // of Read and Write instances. We can define these by hand by mapping to and from
  // the Scala type (Int, Int) which does have automatically-derived instances.

  implicit val pointRead: Read[Point] =
    Read[(Int, Int)].map { case (x, y) => new Point(x, y) }
  // pointRead: Read[Point] = doobie.util.Read@2e67456a

  implicit val pointWrite: Write[Point] =
    Write[(Int, Int)].contramap(p => (p.x, p.y))
  // pointWrite: Write[Point] = doobie.util.Write@7bb3c285

  // There is no equivalent to Meta for bidirectional column vector mappings.
}
