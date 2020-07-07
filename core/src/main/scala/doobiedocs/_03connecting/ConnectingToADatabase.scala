// See 'book of doobie', chapter 03:
// https://tpolecat.github.io/doobie/docs/03-Connecting.html
//
package doobiedocs._03connecting

import scala.util.chaining._

import hutil.stringformat._

import cats.effect.IO
import cats.syntax.applicative._

import doobie._
import doobie.implicits._

object ConnectingToADatabase extends hutil.App {

  import doobiedocs._ // imports Transactor xa + implicit ContextShift cs

  s"$dash10 Our First Program $dash10".magenta.println()

  val program1 = 42.pure[ConnectionIO]
  // program1: ConnectionIO[Int] = Pure(42)

  val io = program1.transact(xa)
  // io: IO[Int] = Async(
  //   cats.effect.internals.IOBracket$$$Lambda$8180/248688506@6db044f4,
  //   false
  // )
  io.unsafeRunSync() pipe println
  // res0: Int = 42

  s"$dash10 Our Second Program $dash10".magenta.println()

  val program2 = sql"select 42".query[Int].unique
  // program2: ConnectionIO[Int] = Suspend(
  //   BracketCase(
  //     Suspend(PrepareStatement("select 42")),
  //     doobie.hi.connection$$$Lambda$8172/902966097@4b8e53c9,
  //     cats.effect.Bracket$$Lambda$8174/1752249240@7f379812
  //   )
  // )
  val io2      = program2.transact(xa)
  // io2: IO[Int] = Async(
  //   cats.effect.internals.IOBracket$$$Lambda$8180/248688506@713156d6,
  //   false
  // )
  io2.unsafeRunSync() pipe println
  // res1: Int = 42

  s"$dash10 Our Third Program (monadic) $dash10".magenta.println()

  val program3: ConnectionIO[(Int, Double)] =
    for {
      a <- sql"select 42".query[Int].unique
      b <- sql"select random()".query[Double].unique
    } yield (a, b)

  program3.transact(xa).unsafeRunSync() pipe println
  // res2: (Int, Double) = (42, 0.011002501472830772)

  s"$dash10 Our Program 3a (applicative) $dash10".magenta.println()

  import cats.syntax.apply._

  val program3a = {
    val a: ConnectionIO[Int]    = sql"select 42".query[Int].unique
    val b: ConnectionIO[Double] = sql"select random()".query[Double].unique
    (a, b).tupled
  }

  program3a.transact(xa).unsafeRunSync() pipe println
  // res3: (Int, Double) = (42, 0.7195786754600704)

  s"$dash10 Our Program 3b (compose more) $dash10".magenta.println()

  val valuesList = program3a.replicateA(5)
  // valuesList: ConnectionIO[List[(Int, Double)]] = FlatMapped(
  //   FlatMapped(
  //     FlatMapped(
  //       Suspend(
  //         BracketCase(
  //           Suspend(PrepareStatement("select 42")),
  //           doobie.hi.connection$$$Lambda$8172/902966097@5dd987f7,
  //           cats.effect.Bracket$$Lambda$8174/1752249240@1e6d7356
  //         )
  //       ),
  //       cats.FlatMap$$Lambda$8597/1341541765@651111ab
  //     ),
  //     cats.Monad$$Lambda$8144/773217938@584306a
  //   ),
  //   cats.FlatMap$$Lambda$8343/722281023@14f3f912
  // )
  val result     = valuesList.transact(xa)
  // result: IO[List[(Int, Double)]] = Async(
  //   cats.effect.internals.IOBracket$$$Lambda$8180/248688506@6171cf19,
  //   false
  // )
  result.unsafeRunSync().foreach(println)
  // (42,0.19134460762143135)
  // (42,0.6406009765341878)
  // (42,0.22629678901284933)
  // (42,0.932811641599983)
  // (42,0.7562076565809548)

  s"$dash10 Diving Deeper $dash10".magenta.println()

  import cats.syntax.flatMap._
  import cats.effect.Blocker

  val interpreter =
    KleisliInterpreter[IO](Blocker.liftExecutionContext(ExecutionContexts.synchronous)).ConnectionInterpreter
  // interpreter: ConnectionOp ~> Kleisli[IO, Connection, γ$9$] = doobie.free.KleisliInterpreter$$anon$11@c8016d5

  val kleisli = program1.foldMap(interpreter)
  // kleisli: Kleisli[IO, Connection, Int] = Kleisli(
  //   cats.data.KleisliFlatMap$$Lambda$8207/142389822@2823704f
  // )

  val io3 = IO(null: java.sql.Connection) >>= kleisli.run // scalafix:ok DisableSyntax.null
  // io3: IO[Int] = Bind(
  //   Delay(<function0>),
  //   cats.data.KleisliFlatMap$$Lambda$8207/142389822@2823704f
  // )

  io3.unsafeRunSync() pipe println // sneaky; program1 never looks at the connection
  // res5: Int = 42

  s"$dash10 Using Your Own Target Monad (monix.eval.Task instead of cats.effect.IO) $dash10".magenta.println()

  import monix.eval.Task
  import monix.execution.Scheduler.Implicits.global

  val mxa = Transactor.fromDriverManager[Task](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres",
    ""
  )

  sql"select 42"
    .query[Int]
    .unique
    .transact(mxa)
    .runSyncUnsafe()
    .pipe(println)
}
