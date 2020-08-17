package io.paytouch.core.reports.resources.locationgiftcardpasses

import io.paytouch.core.data.daos.ConfiguredTestDatabase
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.entities.GiftCardPassAggregate
import io.paytouch.core.reports.resources.giftcardpasses.GiftCardPassesFSpecFixtures
import io.paytouch.core.reports.services.AdminReportService
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.utils.FutureHelpers

trait LocationGiftCardPassesFSpecFixtures
    extends GiftCardPassesFSpecFixtures
       with ConfiguredTestDatabase
       with FutureHelpers {

  private val zeroFullAggregate = GiftCardPassAggregate(
    count = 0,
    customers = Some(0),
    total = Some(0.$$$),
    redeemed = Some(0.$$$),
    unused = Some(0.$$$),
  )

  /** london * */
  val londonEmptyAggregate = GiftCardPassAggregate(count = 0)
  val londonFullAggregate = zeroFullAggregate
  val londonW0FullAggregate = zeroFullAggregate
  val londonW1FullAggregate = zeroFullAggregate
  val londonW2FullAggregate = zeroFullAggregate
  val londonW3FullAggregate = zeroFullAggregate
  val londonW4FullAggregate = zeroFullAggregate

  /** rome * */
  val romeEmptyAggregate = GiftCardPassAggregate(count = 0)
  val romeFullAggregate = zeroFullAggregate
  val romeW0FullAggregate = zeroFullAggregate
  val romeW1FullAggregate = zeroFullAggregate
  val romeW2FullAggregate = zeroFullAggregate
  val romeW3FullAggregate = zeroFullAggregate
  val romeW4FullAggregate = zeroFullAggregate

  /** willow 1 * */
  val willow1EmptyAggregate = GiftCardPassAggregate(count = 2)
  val willow1FullAggregate = GiftCardPassAggregate(
    count = 2,
    customers = Some(2),
    total = Some(90.$$$),
    redeemed = Some(35.$$$),
    unused = Some(55.$$$),
  )
  val willow1W0FullAggregate = zeroFullAggregate
  val willow1W1FullAggregate = GiftCardPassAggregate(
    count = 1,
    customers = Some(1),
    total = Some(50.$$$),
    redeemed = Some(25.$$$),
    unused = Some(25.$$$),
  )
  val willow1W2FullAggregate = GiftCardPassAggregate(
    count = 1,
    customers = Some(1),
    total = Some(40.$$$),
    redeemed = Some(0.$$$),
    unused = Some(40.$$$),
  )
  val willow1W3FullAggregate = zeroFullAggregate
  val willow1W4FullAggregate = zeroFullAggregate

  /** willow 2 * */
  val willow2EmptyAggregate = GiftCardPassAggregate(count = 1)
  val willow2FullAggregate = GiftCardPassAggregate(
    count = 1,
    customers = Some(1),
    total = Some(20.$$$),
    redeemed = Some(20.$$$),
    unused = Some(0.$$$),
  )
  val willow2W0FullAggregate = zeroFullAggregate
  val willow2W1FullAggregate = GiftCardPassAggregate(
    count = 1,
    customers = Some(1),
    total = Some(20.$$$),
    redeemed = Some(0.$$$),
    unused = Some(20.$$$),
  )
  val willow2W2FullAggregate = zeroFullAggregate
  val willow2W3FullAggregate = zeroFullAggregate
  val willow2W4FullAggregate = zeroFullAggregate

  /** willow 3 * */
  val willow3EmptyAggregate = GiftCardPassAggregate(count = 1)
  val willow3FullAggregate = GiftCardPassAggregate(
    count = 1,
    customers = Some(1),
    total = Some(100.$$$),
    redeemed = Some(0.$$$),
    unused = Some(100.$$$),
  )

  val willow3W0FullAggregate = zeroFullAggregate
  val willow3W1FullAggregate = zeroFullAggregate
  val willow3W2FullAggregate = zeroFullAggregate
  val willow3W3FullAggregate = GiftCardPassAggregate(
    count = 1,
    customers = Some(1),
    total = Some(100.$$$),
    redeemed = Some(0.$$$),
    unused = Some(100.$$$),
  )
  val willow3W4FullAggregate = zeroFullAggregate

  new AdminReportService(db).triggerUpdateReports(filters = adminReportFilters).await
}
