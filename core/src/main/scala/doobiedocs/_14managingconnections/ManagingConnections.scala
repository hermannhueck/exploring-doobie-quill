// See 'book of doobie', chapter 14:
// https://tpolecat.github.io/doobie/docs/14-Managing-Connections.html
//
package doobiedocs._14managingconnections

import hutil.stringformat._

import cats.effect.{Blocker, ContextShift, IO, Resource}

import doobie._
import doobie.implicits._

object ManagingConnections extends hutil.App {

  s"$dash10 Using the JDBC DriverManager $dash10".magenta.println

  object TransactorFromDriverManager {

    import doobie.util.ExecutionContexts

    // We need a ContextShift[IO] before we can construct a Transactor[IO]. The passed ExecutionContext
    // is where nonblocking operations will be executed. For testing here we're using a synchronous EC.
    implicit val cs = IO.contextShift(ExecutionContexts.synchronous)

    // A transactor that gets connections from java.sql.DriverManager and executes blocking operations
    // on an our synchronous EC. See the chapter on connection handling for more info.
    val xa = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",                                    // driver classname
      "jdbc:postgresql:world",                                    // connect URL (driver-specific)
      "postgres",                                                 // user
      "",                                                         // password
      Blocker.liftExecutionContext(ExecutionContexts.synchronous) // just for testing
    )
  }

  s"$dash10 Using a HikariCP Connection Pool $dash10".magenta.println

  object TransactorFromHikariConnectionPool {

    import doobie.hikari._

    implicit val cs = IO.contextShift(ExecutionContexts.synchronous)

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
  }

  s"$dash10 Using an existing DataSource $dash10".magenta.println

  object TransactorFromDataSource {

    import javax.sql.DataSource

    //implicit val cs = IO.contextShift(ExecutionContexts.synchronous)

    // Resource yielding a DataSourceTransactor[IO] wrapping the given `DataSource`
    // @scala.annotation.nowarn("cat=unused-params")
    def transactor(ds: DataSource)(implicit ev: ContextShift[IO]): Resource[IO, DataSourceTransactor[IO]] =
      for {
        ce <- ExecutionContexts.fixedThreadPool[IO](32) // our connect EC
        be <- Blocker[IO]                               // our blocking EC
      } yield Transactor.fromDataSource[IO](ds, ce, be)
  }

  s"$dash10 Using an Existing JDBC Connection $dash10".magenta.println

  object TransactorFromJdbcConnection {

    import java.sql.Connection

    // Resource yielding a Transactor[IO] wrapping the given `Connection`
    // @scala.annotation.nowarn("cat=unused-params")
    def transactor(c: Connection)(implicit ev: ContextShift[IO]): Resource[IO, Transactor[IO]] =
      Blocker[IO].map { b => Transactor.fromConnection[IO](c, b) }
  }

  s"$dash10 Customizing Transactors $dash10".magenta.println

  object CustomizingTransactors {

    val xa = TransactorFromDriverManager.xa

    val testXa = Transactor.after.set(xa, HC.rollback)

    import doobie.util.transactor.Strategy
    import doobie.free.connection.unit

    val hiveXa = Transactor.strategy.set(xa, Strategy.default.copy(after = unit, oops = unit))
  }
}
