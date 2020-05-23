package doobiedocs._16extensionsh2

import cats.effect._
import cats.implicits._

import doobie._
import doobie.h2._
import doobie.implicits._

object H2App extends hutil.IOApp {

  // Resource yielding a transactor configured with a bounded connect EC and an unbounded
  // transaction EC. Everything will be closed and shut down cleanly after use.
  val transactor: Resource[IO, H2Transactor[IO]] =
    for {
      ec      <- ExecutionContexts.fixedThreadPool[IO](32) // our connect EC
      blocker <- Blocker[IO]                               // our blocking EC
      xa <- H2Transactor.newH2Transactor[IO](
             "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", // connect URL
             "sa",                                 // username
             "",                                   // password
             ec,                                   // await connection here
             blocker                               // execute JDBC operations here
           )
    } yield xa

  def ioRun(args: List[String]): IO[ExitCode] =
    transactor.use { xa =>
      // Construct and run your server here!
      for {
        n <- sql"select 42".query[Int].unique.transact(xa)
        _ <- IO(println(n))
      } yield ExitCode.Success
    }
}
