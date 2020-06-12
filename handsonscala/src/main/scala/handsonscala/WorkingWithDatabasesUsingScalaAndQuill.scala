// See:
// https://www.lihaoyi.com/post/WorkingwithDatabasesusingScalaandQuill.html
//
package handsonscala

import scala.util.chaining._
import hutil.stringformat._

import io.getquill._

// import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

object WorkingWithDatabasesUsingScalaAndQuill extends hutil.App {

  val pgDataSource = new org.postgresql.ds.PGSimpleDataSource()
  // val pgDataSource1 = new org.postgresql.ds.PGConnectionPoolDataSource()
  // @annotation.nowarn("cat=deprecation")
  // val pgDataSource2 = new org.postgresql.ds.PGPoolingDataSource
  pgDataSource.setUser("postgres")
  pgDataSource.setDatabaseName("world")

  val config = new HikariConfig()
  config.setDataSource(pgDataSource)
  config.setMaximumPoolSize(10)

  sys.addShutdownHook {
    config.getDataSource().getConnection().close()
  }

  val ctx = new PostgresJdbcContext(LowerCase, new HikariDataSource(config))
  import ctx._

  import model._

  s"$dash10 Basic Queries $dash10".magenta.println

  // basic query on City
  val qCities            = quote {
    query[City].take(3)
  }
  printAstAndStatement(qCities.ast, ctx.translate(qCities))
  val cities: List[City] = ctx.run(qCities)
  cities foreach println
  println

  // basic query on Country
  val qCountries               = quote {
    query[Country].take(3)
  }
  printAstAndStatement(qCities.ast, ctx.translate(qCountries))
  val countries: List[Country] = ctx.run(qCountries)
  countries foreach println
  println

  // basic query on CountryLanguage
  val qLanguages                      = quote {
    query[CountryLanguage].take(3)
  }
  printAstAndStatement(qLanguages.ast, ctx.translate(qLanguages))
  val language: List[CountryLanguage] = ctx.run(qLanguages)
  language foreach println
  println

  s"$dash10 Filtering $dash10".magenta.println

  // basic query on City
  val qCities2            = quote {
    query[City].filter(_.name == "Singapore")
  }
  printAstAndStatement(qCities2.ast, ctx.translate(qCities2))
  val cities2: List[City] = ctx.run(qCities2)
  cities2 foreach println
  println

  val qCities3            = quote {
    query[City].filter(_.id == 3208)
  }
  printAstAndStatement(qCities3.ast, ctx.translate(qCities3))
  val cities3: List[City] = ctx.run(qCities3)
  cities3 foreach println
  println

  val qCities4            = quote {
    query[City].filter(_.population > 9000000)
  }
  printAstAndStatement(qCities4.ast, ctx.translate(qCities4))
  val cities4: List[City] = ctx.run(qCities4)
  cities4 foreach println
  println

  val qCities5            = quote {
    query[City].filter(c => c.population > 5000000 && c.countryCode == "CHN")
  }
  printAstAndStatement(qCities5.ast, ctx.translate(qCities5))
  val cities5: List[City] = ctx.run(qCities5)
  cities5 foreach println
  println

  val qCities6            = quote {
    query[City].filter(c => c.population > 5000000).filter(_.countryCode == "CHN")
  }
  printAstAndStatement(qCities6.ast, ctx.translate(qCities6))
  val cities6: List[City] = ctx.run(qCities6)
  cities6 foreach println
  println

  s"$dash10 Lifting $dash10".magenta.println

  def findCityById(cityId: Int) =
    quote {
      query[City].filter(_.id == lift(cityId))
    }

  val qCities7            = findCityById(3208)
  printAstAndStatement(qCities7.ast, ctx.translate(qCities7))
  val cities7: List[City] = ctx.run(qCities7)
  cities7 foreach println
  val qCities8            = findCityById(3209)
  printAstAndStatement(qCities8.ast, ctx.translate(qCities8))
  val cities8: List[City] = ctx.run(qCities8)
  cities8 foreach println
  println

  // val qCities9 = quote {
  //   query[City].filter(_.name.length == 1)
  // }
  // compile error
  // [error]       Tree 'x$6.name.length()' can't be parsed to 'Ast'

  // val qCities10 = quote {
  //   query[City].filter(_.name.substring(0, 1) == "S")
  // }
  // compile error
  // [error]       Tree 'x$6.name.substring(0, 1)' can't be parsed to 'Ast'

  s"$dash10 Mapping $dash10".magenta.println

  val qCountries2                        = quote {
    query[Country].map(c => (c.name, c.continent)).take(5)
  }
  printAstAndStatement(qCountries2.ast, ctx.translate(qCountries2))
  val countries2: List[(String, String)] = ctx.run(qCountries2)
  countries2 foreach println
  println

  val qCountries3                             = quote {
    query[Country].map(c => (c.name, c.continent, c.population)).take(5)
  }
  printAstAndStatement(qCountries3.ast, ctx.translate(qCountries3))
  val countries3: List[(String, String, Int)] = ctx.run(qCountries3)
  countries3 foreach println
  println

  def findCityNameById(cityId: Int) =
    quote {
      query[City].filter(_.id == lift(cityId)).map(_.name)
    }

  val qCities11              = findCityNameById(3208)
  printAstAndStatement(qCities11.ast, ctx.translate(qCities11))
  val cities11: List[String] = ctx.run(qCities11)
  cities11 foreach println
  val qCities12              = findCityNameById(3209)
  printAstAndStatement(qCities12.ast, ctx.translate(qCities12))
  val cities12: List[String] = ctx.run(qCities12)
  cities12 foreach println
  println

  s"$dash10 Joins $dash10".magenta.println

  val qCities13              = quote {
    query[City]
      .join(query[Country])
      .on { case (city, country) => city.countryCode == country.code }
      .filter { case (_, country) => country.continent == "Asia" }
      .map { case (city, _) => city.name }
      .take(5)
  }
  printAstAndStatement(qCities13.ast, ctx.translate(qCities13))
  val cities13: List[String] = ctx.run(qCities13)
  cities13 foreach println
  println

  s"$dash10 Inserts $dash10".magenta.println

  val delete        = quote {
    query[City].filter(_.population == 0).delete
  }
  printAstAndStatement(delete.ast, ctx.translate(delete))
  val deleted: Long = ctx.run(delete)
  deleted pipe println

  val insert         = quote {
    query[City].insert(City(10000, "test", "TST", "Test County", 0))
  }
  printAstAndStatement(insert.ast, ctx.translate(insert))
  val inserted: Long = ctx.run(insert)
  inserted pipe println

  val qCities14            = quote {
    query[City].filter(_.population == 0)
  }
  printAstAndStatement(qCities14.ast, ctx.translate(qCities14))
  val cities14: List[City] = ctx.run(qCities14)
  cities14 foreach println

  s"$dash10 Batch Inserts with 'liftQuery' and 'foreach' $dash10".magenta.println

  val insertBatch               = quote {
    liftQuery(
      List(
        City(10001, "testville", "TSV", "Test County", 0),
        City(10002, "testopolis", "TSO", "Test County", 0),
        City(10003, "testberg", "TSB", "Test County", 0)
      )
    ).foreach(city => query[City].insert(city))
  }
  printAst(insertBatch.ast)
  // printStatement(ctx.translate(insertBatch))
  val batchInserted: List[Long] = ctx.run(insertBatch)
  batchInserted pipe println

  val qCities15            = quote {
    query[City].filter(_.population == 0)
  }
  printAstAndStatement(qCities15.ast, ctx.translate(qCities15))
  val cities15: List[City] = ctx.run(qCities15)
  cities15 foreach println
  println

  s"$dash10 Updates $dash10".magenta.println

  val update        = quote {
    query[City].filter(_.id == 10000).update(City(10000, "testham", "TST", "Test County", 0))
  }
  printAstAndStatement(update.ast, ctx.translate(update))
  val updated: Long = ctx.run(update)
  updated pipe println

  val qCities16            = quote {
    query[City].filter(_.id == 10000)
  }
  printAstAndStatement(qCities16.ast, ctx.translate(qCities16))
  val cities16: List[City] = ctx.run(qCities16)
  cities16 foreach println
  println

  val update2        = quote {
    query[City].filter(_.id == 10000).update(_.name -> "testford")
  }
  printAstAndStatement(update2.ast, ctx.translate(update2))
  val updated2: Long = ctx.run(update2)
  updated2 pipe println

  val qCities17            = quote {
    query[City].filter(_.id == 10000)
  }
  printAstAndStatement(qCities17.ast, ctx.translate(qCities17))
  val cities17: List[City] = ctx.run(qCities17)
  cities17 foreach println
  println

  val update3        = quote {
    query[City].filter(_.district == "Test County").update(_.district -> "Test Borough")
  }
  printAstAndStatement(update3.ast, ctx.translate(update3))
  val updated3: Long = ctx.run(update3)
  updated3 pipe println

  val qCities18            = quote {
    query[City].filter(_.population == 0)
  }
  printAstAndStatement(qCities18.ast, ctx.translate(qCities18))
  val cities18: List[City] = ctx.run(qCities18)
  cities18 foreach println
  println

  s"$dash10 Transactions $dash10".magenta.println

  val update4 = quote {
    query[City].filter(_.district == "Test Borough").update(_.district -> "Test County")
  }
  printAstAndStatement(update4.ast, ctx.translate(update4))
  try {
    @annotation.nowarn("cat=w-flag-dead-code")
    val updated4: Long = ctx.transaction {
      ctx.run(update4)
      throw new Exception("BOOOMMMMM !!!!!")
    }
    updated4 pipe println
  } catch {
    case e: Throwable => println(e.toString)
  }

  val qCities19            = quote {
    query[City].filter(_.population == 0).sortBy(_.id)(Ord.ascNullsLast)
  }
  printAstAndStatement(qCities19.ast, ctx.translate(qCities19))
  val cities19: List[City] = ctx.run(qCities19)
  cities19 foreach println
  println

  val delete2        = quote {
    query[City].filter(_.population == 0).delete
  }
  printAstAndStatement(delete2.ast, ctx.translate(delete2))
  val deleted2: Long = ctx.run(delete2)
  deleted2 pipe println
}
