// See 'book of doobie', chapter 14:
// https://tpolecat.github.io/doobie/docs/14-Managing-Connections.html
//
package doobiedocs._14managingconnections

import cats.effect.{Blocker, ExitCode, IO, Resource}

import doobie._
import doobie.hikari._
import doobie.implicits._

object HikariApp extends hutil.IOApp {

  // Resource yielding a transactor configured with a bounded connect EC and an unbounded
  // transaction EC. Everything will be closed and shut down cleanly after use.
  val transactor: Resource[IO, HikariTransactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32) // our connect EC
      be <- Blocker[IO]                               // our blocking EC
      xa <- HikariTransactor.newHikariTransactor[IO](
             "org.h2.Driver",                      // driver classname
             "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", // connect URL
             "sa",                                 // username
             "",                                   // password
             ce,                                   // await connection here
             be                                    // execute JDBC operations here
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
