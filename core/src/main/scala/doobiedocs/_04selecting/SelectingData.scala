// See 'book of doobie', chapter 04:
// https://tpolecat.github.io/doobie/docs/04-Selecting.html
//
package doobiedocs._04selecting

import scala.concurrent.ExecutionContext

import hutil.stringformat._

import cats.effect.IO

import doobie._
import doobie.implicits._

object SelectingData extends hutil.App {

  import doobiedocs._ // imports Transactor xa + implicit ContextShift cs

  s"$dash10 Reading Rows into Collections $dash10".magenta.println

  sql"select name from country" // Fragment
    .query[String]              // Query0[String]
    .to[List]                   // ConnectionIO[List[String]]
    .transact(xa)               // IO[List[String]]
    .unsafeRunSync              // List[String]
    .take(5)                    // List[String]
    .foreach(println)           // Unit
  // Afghanistan
  // Netherlands
  // Netherlands Antilles
  // Albania
  // Algeria

  s"$dash10 Internal Streaming $dash10".magenta.println

  sql"select name from country" // Fragment
    .query[String]              // Query0[String]
    .stream                     // Stream[ConnectionIO, String]
    .take(5)                    // Stream[ConnectionIO, String]
    .compile                    // Stream.CompileOps[ConnectionIO, ConnectionIO, String]
    .toList                     // ConnectionIO[List[String]]
    .transact(xa)               // IO[List[String]]
    .unsafeRunSync              // List[String]
    .foreach(println)           // Unit
  // Afghanistan
  // Netherlands
  // Netherlands Antilles
  // Albania
  // Algeria

  s"$dash10 With server-side LIMIT $dash10".magenta.println

  sql"select name from country limit 5"
    .query[String]    // Query0[String]
    .to[List]         // ConnectionIO[List[String]]
    .transact(xa)     // IO[List[String]]
    .unsafeRunSync    // List[String]
    .foreach(println) // Unit
  // Afghanistan
  // Netherlands
  // Netherlands Antilles
  // Albania
  // Algeria

  s"$dash10 YOLO Mode $dash10".magenta.println

  val y = xa.yolo // a stable reference is required
  import y._

  sql"select name from country"
    .query[String] // Query0[String]
    .stream        // Stream[ConnectionIO, String]
    .take(5)       // Stream[ConnectionIO, String]
    .quick         // IO[Unit]
    .unsafeRunSync
  //   Afghanistan
  //   Netherlands
  //   Netherlands Antilles
  //   Albania
  //   Algeria

  s"$dash10 Multi-Column Queries $dash10".magenta.println

  sql"select code, name, population, gnp from country"
    .query[(String, String, Int, Option[Double])]
    .stream
    .take(5)
    .quick
    .unsafeRunSync
  //   (AFG,Afghanistan,22720000,Some(5976.0))
  //   (NLD,Netherlands,15864000,Some(371362.0))
  //   (ANT,Netherlands Antilles,217000,Some(1941.0))
  //   (ALB,Albania,3401200,Some(3205.0))
  //   (DZA,Algeria,31471000,Some(49982.0))

  s"$dash10 Shapeless HList Support $dash10".magenta.println

  import shapeless.{::, HList, HNil}

  type CountryHList = String :: String :: Int :: Option[Double] :: HNil

  sql"select code, name, population, gnp from country"
    .query[CountryHList]
    .stream
    .take(5)
    .quick
    .unsafeRunSync
  //   AFG :: Afghanistan :: 22720000 :: Some(5976.0) :: HNil
  //   NLD :: Netherlands :: 15864000 :: Some(371362.0) :: HNil
  //   ANT :: Netherlands Antilles :: 217000 :: Some(1941.0) :: HNil
  //   ALB :: Albania :: 3401200 :: Some(3205.0) :: HNil
  //   DZA :: Algeria :: 31471000 :: Some(49982.0) :: HNil

  s"$dash10 Shapeless Record Support $dash10".magenta.println

  import shapeless.record.Record

  type Rec =
    Record
      .`Symbol("code") -> String, Symbol("name") -> String, Symbol("pop") -> Int, Symbol("gnp") -> Option[Double]`.T

  sql"select code, name, population, gnp from country"
    .query[Rec]
    .stream
    .take(5)
    .quick
    .unsafeRunSync
  //   AFG :: Afghanistan :: 22720000 :: Some(5976.0) :: HNil
  //   NLD :: Netherlands :: 15864000 :: Some(371362.0) :: HNil
  //   ANT :: Netherlands Antilles :: 217000 :: Some(1941.0) :: HNil
  //   ALB :: Albania :: 3401200 :: Some(3205.0) :: HNil
  //   DZA :: Algeria :: 31471000 :: Some(49982.0) :: HNil

  s"$dash10 Mapping rows to a case class $dash10".magenta.println

  case class Country(code: String, name: String, pop: Int, gnp: Option[Double])

  sql"select code, name, population, gnp from country"
    .query[Country]
    .stream
    .take(5)
    .quick
    .unsafeRunSync
  //   Country(AFG,Afghanistan,22720000,Some(5976.0))
  //   Country(NLD,Netherlands,15864000,Some(371362.0))
  //   Country(ANT,Netherlands Antilles,217000,Some(1941.0))
  //   Country(ALB,Albania,3401200,Some(3205.0))
  //   Country(DZA,Algeria,31471000,Some(49982.0))

  s"$dash10 Nested case classes and Tuples (HLists, Records) $dash10".magenta.println

  case class Code(code: String)
  case class Country2(name: String, pop: Int, gnp: Option[Double])

  sql"select code, name, population, gnp from country"
    .query[(Code, Country2)]
    .stream
    .take(5)
    .quick
    .unsafeRunSync
  //   (Code(AFG),Country2(Afghanistan,22720000,Some(5976.0)))
  //   (Code(NLD),Country2(Netherlands,15864000,Some(371362.0)))
  //   (Code(ANT),Country2(Netherlands Antilles,217000,Some(1941.0)))
  //   (Code(ALB),Country2(Albania,3401200,Some(3205.0)))
  //   (Code(DZA),Country2(Algeria,31471000,Some(49982.0)))

  s"$dash10 Creating a Map from a List of Pairs $dash10".magenta.println

  sql"select code, name, population, gnp from country"
    .query[(Code, Country2)]
    .stream
    .take(5)
    .compile
    .toList
    .map(_.toMap)
    .quick
    .unsafeRunSync
  //   HashMap(Code(ANT) -> Country2(Netherlands Antilles,217000,Some(1941.0)), Code(ALB) -> Country2(Albania,3401200,Some(3205.0)), Code(DZA) -> Country2(Algeria,31471000,Some(49982.0)), Code(NLD) -> Country2(Netherlands,15864000,Some(371362.0)), Code(AFG) -> Country2(Afghanistan,22720000,Some(5976.0)))
  import fs2.Stream

  s"$dash10 Final Streaming $dash10".magenta.println

  val s: Stream[IO, Country2] =
    sql"select name, population, gnp from country"
      .query[Country2] // Query0[Country2]
      .stream          // Stream[ConnectionIO, Country2]
      .transact(xa)    // Stream[IO, Country2]
  // s: Stream[IO, Country2] = Stream(..)

  s.take(5)
    .compile
    .toVector
    .unsafeRunSync
    .foreach(println)
  // Country2(Afghanistan,22720000,Some(5976.0))
  // Country2(Netherlands,15864000,Some(371362.0))
  // Country2(Netherlands Antilles,217000,Some(1941.0))
  // Country2(Albania,3401200,Some(3205.0))
  // Country2(Algeria,31471000,Some(49982.0))

  s"$dash10 Diving Deeper $dash10".magenta.println

  import cats.syntax.applicative._ // for pure

  val proc = HC.stream[(Code, Country2)](
    "select code, name, population, gnp from country", // statement
    ().pure[PreparedStatementIO],                      // prep (none)
    512                                                // chunk size
  )
  // proc: Stream[ConnectionIO, (Code, Country2)] = Stream(..)

  proc
    .take(5) // Stream[ConnectionIO, (Code, Country2)]
    .compile
    .toList       // ConnectionIO[List[(Code, Country2)]]
    .map(_.toMap) // ConnectionIO[Map[Code, Country2]]
    .quick
    .unsafeRunSync
  //   HashMap(Code(ANT) -> Country2(Netherlands Antilles,217000,Some(1941.0)), Code(ALB) -> Country2(Albania,3401200,Some(3205.0)), Code(DZA) -> Country2(Algeria,31471000,Some(49982.0)), Code(NLD) -> Country2(Netherlands,15864000,Some(371362.0)), Code(AFG) -> Country2(Afghanistan,22720000,Some(5976.0)))
}
