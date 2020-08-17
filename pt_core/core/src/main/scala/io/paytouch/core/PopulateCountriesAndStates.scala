package io.paytouch.core

import java.util.UUID

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._

import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.data.db.ConfiguredDatabase
import io.paytouch.core.data.tables._
import io.paytouch.core.services.UtilService
import io.paytouch.core.data.driver.CustomPostgresDriver

/**
  * Adds countryCodes and stateCodes and also improves
  * countryNames and stateNames
  */
object PopulateCountriesAndStates extends App with MigrationHelpers {
  run(args.headOption.map(_.toBoolean).getOrElse(false)) // false means dry run

  def run(doWrites: Boolean): Unit = {
    val db = ConfiguredDatabase.db

    val dryRun = !doWrites

    println("─" * 100)

    runForCustomerMerchantsTable()
    runForGlobalCustomersTable()
    runForLocationEmailReceiptsTable()
    runForLocationPrintReceiptsTable()
    runForLocationReceiptsTable()
    runForLocationsTable()
    runForOrderDeliveryAddressesTable()
    runForUsersTable()

    println("─" * 100)

    def runForCustomerMerchantsTable(): Unit = {
      val table = TableQuery[CustomerMerchantsTable]

      println(
        s"Running for ${"CustomerMerchantsTable".cyan} with ${size(db, table).cyan} rows...",
      )

      val queries = pageQueries(db, table)
      val total = queries.size
      val zipped = queries.zipWithIndex.map(_.map(_ + 1))

      zipped.foreach {
        case (page, index) =>
          println(
            s"Running page ${index.cyan}/${total.cyan} for ${"CustomerMerchantsTable".cyan} with ${size(db, table).cyan} rows...",
          )

          page
            .map { record =>
              (
                record.id,
                record.countryCode,
                record.country, // name
                record.stateCode,
                record.state, // name
              )
            }
            .result
            .pipe(db.run)
            .pipe(await)
            .foreach {
              case (id, countryCode, countryName, stateCode, stateName) =>
                (
                  id,
                  countryCode.map(CountryCode),
                  countryName.map(CountryName),
                  stateCode.map(StateCode),
                  stateName.map(StateName),
                ).pipe(resolved)
                  .pipe {
                    case (id, countryCode, countryName, stateCode, stateName) =>
                      if (dryRun)
                        table.take(0).result // I just want a noop query
                      else
                        table
                          .filter(_.id === id)
                          .map { record =>
                            (
                              record.countryCode,
                              record.country,
                              record.stateCode,
                              record.state,
                            )
                          }
                          .update( // write query
                            countryCode.value.some,
                            countryName.value.some,
                            stateCode.value.some,
                            stateName.value.some,
                          )
                  }
                  .pipe(db.run)
                  .pipe(await)
            }
      }
    }

    def runForGlobalCustomersTable(): Unit = {
      val table = TableQuery[GlobalCustomersTable]

      println(
        s"Running for ${"GlobalCustomersTable".cyan} with ${size(db, table).cyan} rows...",
      )

      val queries = pageQueries(db, table)
      val total = queries.size
      val zipped = queries.zipWithIndex.map(_.map(_ + 1))

      zipped.foreach {
        case (page, index) =>
          println(
            s"Running page ${index.cyan}/${total.cyan} for ${"GlobalCustomersTable".cyan} with ${size(db, table).cyan} rows...",
          )

          page
            .map { record =>
              (
                record.id,
                record.countryCode,
                record.country, // name
                record.stateCode,
                record.state, // name
              )
            }
            .result
            .pipe(db.run)
            .pipe(await)
            .foreach {
              case (id, countryCode, countryName, stateCode, stateName) =>
                (
                  id,
                  countryCode.map(CountryCode),
                  countryName.map(CountryName),
                  stateCode.map(StateCode),
                  stateName.map(StateName),
                ).pipe(resolved)
                  .pipe {
                    case (id, countryCode, countryName, stateCode, stateName) =>
                      if (dryRun)
                        table.take(0).result // I just want a noop query
                      else
                        table
                          .filter(_.id === id)
                          .map { record =>
                            (
                              record.countryCode,
                              record.country,
                              record.stateCode,
                              record.state,
                            )
                          }
                          .update( // write query
                            countryCode.value.some,
                            countryName.value.some,
                            stateCode.value.some,
                            stateName.value.some,
                          )
                  }
                  .pipe(db.run)
                  .pipe(await)
            }
      }
    }

    def runForLocationEmailReceiptsTable(): Unit = {
      val table = TableQuery[LocationEmailReceiptsTable]

      println(
        s"Running for ${"LocationEmailReceiptsTable".cyan} with ${size(db, table).cyan} rows...",
      )

      val queries = pageQueries(db, table)
      val total = queries.size
      val zipped = queries.zipWithIndex.map(_.map(_ + 1))

      zipped.foreach {
        case (page, index) =>
          println(
            s"Running page ${index.cyan}/${total.cyan} for ${"LocationEmailReceiptsTable".cyan} with ${size(db, table).cyan} rows...",
          )

          page
            .map { record =>
              (
                record.id,
                record.locationCountryCode,
                record.locationCountry, // name
                record.locationStateCode,
                record.locationState, // name
              )
            }
            .result
            .pipe(db.run)
            .pipe(await)
            .foreach {
              case (id, countryCode, countryName, stateCode, stateName) =>
                (
                  id,
                  countryCode.map(CountryCode),
                  countryName.map(CountryName),
                  stateCode.map(StateCode),
                  stateName.map(StateName),
                ).pipe(resolved)
                  .pipe {
                    case (id, countryCode, countryName, stateCode, stateName) =>
                      if (dryRun)
                        table.take(0).result // I just want a noop query
                      else
                        table
                          .filter(_.id === id)
                          .map { record =>
                            (
                              record.locationCountryCode,
                              record.locationCountry,
                              record.locationStateCode,
                              record.locationState,
                            )
                          }
                          .update( // write query
                            countryCode.value.some,
                            countryName.value.some,
                            stateCode.value.some,
                            stateName.value.some,
                          )
                  }
                  .pipe(db.run)
                  .pipe(await)
            }
      }
    }

    def runForLocationPrintReceiptsTable(): Unit = {
      val table = TableQuery[LocationPrintReceiptsTable]

      println(
        s"Running for ${"LocationPrintReceiptsTable".cyan} with ${size(db, table).cyan} rows...",
      )

      val queries = pageQueries(db, table)
      val total = queries.size
      val zipped = queries.zipWithIndex.map(_.map(_ + 1))

      zipped.foreach {
        case (page, index) =>
          println(
            s"Running page ${index.cyan}/${total.cyan} for ${"LocationPrintReceiptsTable".cyan} with ${size(db, table).cyan} rows...",
          )

          page
            .map { record =>
              (
                record.id,
                record.locationCountryCode,
                record.locationCountry, // name
                record.locationStateCode,
                record.locationState, // name
              )
            }
            .result
            .pipe(db.run)
            .pipe(await)
            .foreach {
              case (id, countryCode, countryName, stateCode, stateName) =>
                (
                  id,
                  countryCode.map(CountryCode),
                  countryName.map(CountryName),
                  stateCode.map(StateCode),
                  stateName.map(StateName),
                ).pipe(resolved)
                  .pipe {
                    case (id, countryCode, countryName, stateCode, stateName) =>
                      if (dryRun)
                        table.take(0).result // I just want a noop query
                      else
                        table
                          .filter(_.id === id)
                          .map { record =>
                            (
                              record.locationCountryCode,
                              record.locationCountry,
                              record.locationStateCode,
                              record.locationState,
                            )
                          }
                          .update( // write query
                            countryCode.value.some,
                            countryName.value.some,
                            stateCode.value.some,
                            stateName.value.some,
                          )
                  }
                  .pipe(db.run)
                  .pipe(await)
            }
      }
    }

    def runForLocationReceiptsTable(): Unit = {
      val table = TableQuery[LocationReceiptsTable]

      println(
        s"Running for ${"LocationReceiptsTable".cyan} with ${size(db, table).cyan} rows...",
      )

      val queries = pageQueries(db, table)
      val total = queries.size
      val zipped = queries.zipWithIndex.map(_.map(_ + 1))

      zipped.foreach {
        case (page, index) =>
          println(
            s"Running page ${index.cyan}/${total.cyan} for ${"LocationReceiptsTable".cyan} with ${size(db, table).cyan} rows...",
          )

          page
            .map { record =>
              (
                record.id,
                record.countryCode,
                record.country, // name
                record.stateCode,
                record.state, // name
              )
            }
            .result
            .pipe(db.run)
            .pipe(await)
            .foreach {
              case (id, countryCode, countryName, stateCode, stateName) =>
                (
                  id,
                  countryCode.map(CountryCode),
                  countryName.map(CountryName),
                  stateCode.map(StateCode),
                  stateName.map(StateName),
                ).pipe(resolved)
                  .pipe {
                    case (id, countryCode, countryName, stateCode, stateName) =>
                      if (dryRun)
                        table.take(0).result // I just want a noop query
                      else
                        table
                          .filter(_.id === id)
                          .map { record =>
                            (
                              record.countryCode,
                              record.country,
                              record.stateCode,
                              record.state,
                            )
                          }
                          .update( // write query
                            countryCode.value.some,
                            countryName.value.some,
                            stateCode.value.some,
                            stateName.value.some,
                          )
                  }
                  .pipe(db.run)
                  .pipe(await)
            }
      }
    }

    def runForLocationsTable(): Unit = {
      val table = TableQuery[LocationsTable]

      println(
        s"Running for ${"LocationsTable".cyan} with ${size(db, table).cyan} rows...",
      )

      val queries = pageQueries(db, table)
      val total = queries.size
      val zipped = queries.zipWithIndex.map(_.map(_ + 1))

      zipped.foreach {
        case (page, index) =>
          println(
            s"Running page ${index.cyan}/${total.cyan} for ${"LocationsTable".cyan} with ${size(db, table).cyan} rows...",
          )

          page
            .map { record =>
              (
                record.id,
                record.countryCode,
                record.country, // name
                record.stateCode,
                record.state, // name
              )
            }
            .result
            .pipe(db.run)
            .pipe(await)
            .foreach {
              case (id, countryCode, countryName, stateCode, stateName) =>
                (
                  id,
                  countryCode.map(CountryCode),
                  countryName.map(CountryName),
                  stateCode.map(StateCode),
                  stateName.map(StateName),
                ).pipe(resolved)
                  .pipe {
                    case (id, countryCode, countryName, stateCode, stateName) =>
                      if (dryRun)
                        table.take(0).result // I just want a noop query
                      else
                        table
                          .filter(_.id === id)
                          .map { record =>
                            (
                              record.countryCode,
                              record.country,
                              record.stateCode,
                              record.state,
                            )
                          }
                          .update( // write query
                            countryCode.value.some,
                            countryName.value.some,
                            stateCode.value.some,
                            stateName.value.some,
                          )
                  }
                  .pipe(db.run)
                  .pipe(await)
            }
      }
    }

    def runForOrderDeliveryAddressesTable(): Unit = {
      val table = TableQuery[OrderDeliveryAddressesTable]

      println(
        s"Running for ${"OrderDeliveryAddressesTable".cyan} with ${size(db, table).cyan} rows...",
      )

      val queries = pageQueries(db, table)
      val total = queries.size
      val zipped = queries.zipWithIndex.map(_.map(_ + 1))

      zipped.foreach {
        case (page, index) =>
          println(
            s"Running page ${index.cyan}/${total.cyan} for ${"OrderDeliveryAddressesTable".cyan} with ${size(db, table).cyan} rows...",
          )

          page
            .map { record =>
              (
                record.id,
                record.countryCode,
                record.country, // name
                record.stateCode,
                record.state, // name
              )
            }
            .result
            .pipe(db.run)
            .pipe(await)
            .foreach {
              case (id, countryCode, countryName, stateCode, stateName) =>
                (
                  id,
                  countryCode.map(CountryCode),
                  countryName.map(CountryName),
                  stateCode.map(StateCode),
                  stateName.map(StateName),
                ).pipe(resolved)
                  .pipe {
                    case (id, countryCode, countryName, stateCode, stateName) =>
                      if (dryRun)
                        table.take(0).result // I just want a noop query
                      else
                        table
                          .filter(_.id === id)
                          .map { record =>
                            (
                              record.countryCode,
                              record.country,
                              record.stateCode,
                              record.state,
                            )
                          }
                          .update( // write query
                            countryCode.value.some,
                            countryName.value.some,
                            stateCode.value.some,
                            stateName.value.some,
                          )
                  }
                  .pipe(db.run)
                  .pipe(await)
            }
      }
    }

    def runForUsersTable(): Unit = {
      val table = TableQuery[UsersTable]

      println(
        s"Running for ${"UsersTable".cyan} with ${size(db, table).cyan} rows...",
      )

      val queries = pageQueries(db, table)
      val total = queries.size
      val zipped = queries.zipWithIndex.map(_.map(_ + 1))

      zipped.foreach {
        case (page, index) =>
          println(
            s"Running page ${index.cyan}/${total.cyan} for ${"UsersTable".cyan} with ${size(db, table).cyan} rows...",
          )

          page
            .map { record =>
              (
                record.id,
                record.countryCode,
                record.country, // name
                record.stateCode,
                record.state, // name
              )
            }
            .result
            .pipe(db.run)
            .pipe(await)
            .foreach {
              case (id, countryCode, countryName, stateCode, stateName) =>
                (
                  id,
                  countryCode.map(CountryCode),
                  countryName.map(CountryName),
                  stateCode.map(StateCode),
                  stateName.map(StateName),
                ).pipe(resolved)
                  .pipe {
                    case (id, countryCode, countryName, stateCode, stateName) =>
                      if (dryRun)
                        table.take(0).result // I just want a noop query
                      else
                        table
                          .filter(_.id === id)
                          .map { record =>
                            (
                              record.countryCode,
                              record.country,
                              record.stateCode,
                              record.state,
                            )
                          }
                          .update( // write query
                            countryCode.value.some,
                            countryName.value.some,
                            stateCode.value.some,
                            stateName.value.some,
                          )
                  }
                  .pipe(db.run)
                  .pipe(await)
            }
      }
    }

    def resolved(
        data: (UUID, Option[CountryCode], Option[CountryName], Option[StateCode], Option[StateName]),
      ): (UUID, CountryCode, CountryName, StateCode, StateName) =
      data match {
        case (id, countryCode, countryName, stateCode, stateName) =>
          (
            id,
            UtilService.Geo.country(countryCode, countryName),
            UtilService.Geo.addressState(countryCode, stateCode, countryName, stateName),
          ) tap {
            case (id, country, state) =>
              (country, state)
                .mapN { (country, state) =>
                  println(s"$id ${greenNone(countryCode)} | ${greenNone(countryName)} => $country")
                  println(s"$id ${greenNone(stateCode)} | ${greenNone(stateName)} => $state")
                }
          } pipe {
            case (id, country, state) =>
              (
                id,
                country.fold("" -> "")(c => c.code -> c.name),
                state.fold("" -> "")(s => s.code -> s.name.getOrElse("")),
              )
          } pipe {
            case (id, (countryCode, countryName), (stateCode, stateName)) =>
              (id, CountryCode(countryCode), CountryName(countryName), StateCode(stateCode), StateName(stateName))
          }
      }
  }
}

trait MigrationHelpers {
  def pageQueries[A, B](
      db: CustomPostgresDriver.api.Database,
      query: Query[A, B, Seq],
      pageSize: Int = 10000,
    ): Seq[Query[A, B, Seq]] = {
    type From = Int
    type To = Int
    type Range = (From, To)

    def multipleRanges(step: Int, size: Int): Seq[Range] =
      (0 to size + step by step sliding 2).map {
        case Seq(from, to) => from -> to
      }.toSeq

    multipleRanges(
      step = pageSize,
      size = size(db, query),
    ).map {
      case (from, to) => query.drop(from).take(to)
    }
  }

  def size[A, B](db: CustomPostgresDriver.api.Database, query: Query[A, B, Seq]): Int =
    query.size.result.pipe(db.run).pipe(await)

  def greenNone[A](input: Option[A]): String =
    if (input.isEmpty)
      input.green
    else
      input.toString

  def await[T](awaitable: Awaitable[T]): T =
    Await.result(awaitable, atMost = 1.hour)
}
