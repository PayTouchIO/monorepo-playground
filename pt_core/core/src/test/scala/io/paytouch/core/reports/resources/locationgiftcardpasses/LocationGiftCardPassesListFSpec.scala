package io.paytouch.core.reports.resources.locationgiftcardpasses

import io.paytouch.core.entities.{ MonetaryAmount, Pagination }
import io.paytouch.core.reports.entities.{ GiftCardPassAggregate, LocationGiftCardPasses }
import io.paytouch.core.reports.resources.ReportsListFSpec
import io.paytouch.core.reports.views.LocationGiftCardPassView

class LocationGiftCardPassesListFSpec extends ReportsListFSpec[LocationGiftCardPassView] {

  def view = LocationGiftCardPassView()

  class LocationGiftCardPassesFSpecContext extends ReportsListFSpecContext with LocationGiftCardPassesFSpecFixtures {
    def londonResult(data: GiftCardPassAggregate) =
      LocationGiftCardPasses(id = london.id, name = london.name, addressLine1 = london.addressLine1, data = data)

    def romeResult(data: GiftCardPassAggregate) =
      LocationGiftCardPasses(id = rome.id, name = rome.name, addressLine1 = rome.addressLine1, data = data)

    def willow1Result(data: GiftCardPassAggregate) =
      LocationGiftCardPasses(id = willow1.id, name = willow1.name, addressLine1 = willow1.addressLine1, data = data)

    def willow2Result(data: GiftCardPassAggregate) =
      LocationGiftCardPasses(id = willow2.id, name = willow2.name, addressLine1 = willow2.addressLine1, data = data)

    def willow3Result(data: GiftCardPassAggregate) =
      LocationGiftCardPasses(id = willow3.id, name = willow3.name, addressLine1 = willow3.addressLine1, data = data)

    def orderedResult(
        willow1Data: GiftCardPassAggregate,
        willow2Data: GiftCardPassAggregate,
        willow3Data: GiftCardPassAggregate,
        londonData: GiftCardPassAggregate,
        romeData: GiftCardPassAggregate,
      ) =
      Seq(
        londonResult(londonData),
        romeResult(romeData),
        willow1Result(willow1Data),
        willow2Result(willow2Data),
        willow3Result(willow3Data),
      )

    val totalCount = 5
  }

  val fixtures = new LocationGiftCardPassesFSpecContext
  import fixtures._

  "GET /v1/reports/location_gift_card_passes.list" in {
    "with default order by name" should {
      assertNoField()

      assertFieldResultWithPagination(
        "address",
        totalCount,
        orderedResult(
          londonData = londonEmptyAggregate,
          romeData = romeEmptyAggregate,
          willow1Data = willow1EmptyAggregate,
          willow2Data = willow2EmptyAggregate,
          willow3Data = willow3EmptyAggregate,
        ),
      )

      assertFieldResultWithPagination(
        "id",
        totalCount,
        orderedResult(
          londonData = londonEmptyAggregate,
          romeData = romeEmptyAggregate,
          willow1Data = willow1EmptyAggregate,
          willow2Data = willow2EmptyAggregate,
          willow3Data = willow3EmptyAggregate,
        ),
      )

      assertFieldResultWithPagination(
        "name",
        totalCount,
        orderedResult(
          londonData = londonEmptyAggregate,
          romeData = romeEmptyAggregate,
          willow1Data = willow1EmptyAggregate,
          willow2Data = willow2EmptyAggregate,
          willow3Data = willow3EmptyAggregate,
        ),
      )

      assertFieldResultWithPagination(
        "count",
        totalCount,
        orderedResult(
          londonData = londonEmptyAggregate,
          romeData = romeEmptyAggregate,
          willow1Data = willow1EmptyAggregate,
          willow2Data = willow2EmptyAggregate,
          willow3Data = willow3EmptyAggregate,
        ),
      )

      assertFieldResultWithPagination(
        "customers",
        totalCount,
        orderedResult(
          londonData = londonEmptyAggregate.copy(customers = londonFullAggregate.customers),
          romeData = romeEmptyAggregate.copy(customers = romeFullAggregate.customers),
          willow1Data = willow1EmptyAggregate.copy(customers = willow1FullAggregate.customers),
          willow2Data = willow2EmptyAggregate.copy(customers = willow2FullAggregate.customers),
          willow3Data = willow3EmptyAggregate.copy(customers = willow3FullAggregate.customers),
        ),
      )

      assertFieldResultWithPagination(
        "total",
        totalCount,
        orderedResult(
          londonData = londonEmptyAggregate.copy(total = londonFullAggregate.total),
          romeData = romeEmptyAggregate.copy(total = romeFullAggregate.total),
          willow1Data = willow1EmptyAggregate.copy(total = willow1FullAggregate.total),
          willow2Data = willow2EmptyAggregate.copy(total = willow2FullAggregate.total),
          willow3Data = willow3EmptyAggregate.copy(total = willow3FullAggregate.total),
        ),
      )

      assertFieldResultWithPagination(
        "redeemed",
        totalCount,
        orderedResult(
          londonData = londonEmptyAggregate.copy(redeemed = londonFullAggregate.redeemed),
          romeData = romeEmptyAggregate.copy(redeemed = romeFullAggregate.redeemed),
          willow1Data = willow1EmptyAggregate.copy(redeemed = willow1FullAggregate.redeemed),
          willow2Data = willow2EmptyAggregate.copy(redeemed = willow2FullAggregate.redeemed),
          willow3Data = willow3EmptyAggregate.copy(redeemed = willow3FullAggregate.redeemed),
        ),
      )

      assertFieldResultWithPagination(
        "unused",
        totalCount,
        orderedResult(
          londonData = londonEmptyAggregate.copy(unused = londonFullAggregate.unused),
          romeData = romeEmptyAggregate.copy(unused = romeFullAggregate.unused),
          willow1Data = willow1EmptyAggregate.copy(unused = willow1FullAggregate.unused),
          willow2Data = willow2EmptyAggregate.copy(unused = willow2FullAggregate.unused),
          willow3Data = willow3EmptyAggregate.copy(unused = willow3FullAggregate.unused),
        ),
      )

      assertAllFieldsResultWithPagination(
        totalCount,
        orderedResult(
          londonData = londonFullAggregate,
          romeData = romeFullAggregate,
          willow1Data = willow1FullAggregate,
          willow2Data = willow2FullAggregate,
          willow3Data = willow3FullAggregate,
        ),
      )
    }

    "with custom order" should {

      def orderedDataByOpt[A](
          ordering: LocationGiftCardPasses => Option[A],
        )(
          default: A,
          desc: Boolean = false,
        )(implicit
          order: Ordering[A],
        ) =
        orderedDataBy(x => ordering(x).getOrElse(default), desc)

      def orderedDataBy[A](ordering: LocationGiftCardPasses => A, desc: Boolean = false)(implicit ord: Ordering[A]) =
        Seq(
          londonResult(londonFullAggregate),
          romeResult(romeFullAggregate),
          willow1Result(willow1FullAggregate),
          willow2Result(willow2FullAggregate),
          willow3Result(willow3FullAggregate),
        ).sortBy(ordering)(if (desc) ord.reverse else ord)

      val zeroMonetary = MonetaryAmount(0, currency)

      assertOrderByResultWithPagination("id", totalCount, orderedDataBy(_.id.toString))

      assertOrderByResultWithPagination("name", totalCount, orderedDataBy(_.name))

      assertOrderByResultWithPagination("count", totalCount, orderedDataBy(_.data.count, desc = true))

      assertOrderByResultWithPagination("customers", totalCount, orderedDataByOpt(_.data.customers)(0, desc = true))

      assertOrderByResultWithPagination("total", totalCount, orderedDataByOpt(_.data.total)(zeroMonetary, desc = true))

      assertOrderByResultWithPagination(
        "redeemed",
        totalCount,
        orderedDataByOpt(_.data.redeemed)(zeroMonetary, desc = true),
      )

      assertOrderByResultWithPagination(
        "unused",
        totalCount,
        orderedDataByOpt(_.data.unused)(zeroMonetary, desc = true),
      )

    }

    "with pagination" should {
      assertAllFieldsResultWithPagination(
        totalCount,
        Seq(londonResult(londonFullAggregate)),
        pagination = Some(Pagination(1, 1)),
      )

    }

    "filtered by id[]" should {
      assertAllFieldsResultWithPagination(
        1,
        Seq(londonResult(londonFullAggregate)),
        extraFilters = Some(s"id[]=${london.id}"),
      )
    }

    "filtered by location_id" should {
      assertAllFieldsResultWithPagination(
        1,
        Seq(londonResult(londonFullAggregate)),
        extraFilters = Some(s"location_id=${london.id}"),
      )
    }
  }

}
