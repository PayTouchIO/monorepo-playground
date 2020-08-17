package io.paytouch.core.reports.resources.locationgiftcardpasses

import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.resources.ReportsAggrFSpec
import io.paytouch.core.reports.views.LocationGiftCardPassView

abstract class LocationGiftCardPassesFSpec extends ReportsAggrFSpec[LocationGiftCardPassView] {
  def view = LocationGiftCardPassView()

  val fixtures = new GiftCardPassesFSpecContext

  @scala.annotation.nowarn("msg=Auto-application")
  implicit val mockUserContext =
    random[UserContext].copy(currency = fixtures.currency)

  class GiftCardPassesFSpecContext extends ReportsAggrFSpecContext with LocationGiftCardPassesFSpecFixtures {
    def londonResult(data: GiftCardPassAggregate) =
      ReportFields(
        key = Some(london.id.toString),
        LocationGiftCardPasses(id = london.id, name = london.name, addressLine1 = london.addressLine1, data = data),
      )

    def romeResult(data: GiftCardPassAggregate) =
      ReportFields(
        key = Some(rome.id.toString),
        LocationGiftCardPasses(id = rome.id, name = rome.name, addressLine1 = rome.addressLine1, data = data),
      )

    def willow1Result(data: GiftCardPassAggregate) =
      ReportFields(
        key = Some(willow1.id.toString),
        LocationGiftCardPasses(id = willow1.id, name = willow1.name, addressLine1 = willow1.addressLine1, data = data),
      )

    def willow2Result(data: GiftCardPassAggregate) =
      ReportFields(
        key = Some(willow2.id.toString),
        LocationGiftCardPasses(id = willow2.id, name = willow2.name, addressLine1 = willow2.addressLine1, data = data),
      )

    def willow3Result(data: GiftCardPassAggregate) =
      ReportFields(
        key = Some(willow3.id.toString),
        LocationGiftCardPasses(id = willow3.id, name = willow3.name, addressLine1 = willow3.addressLine1, data = data),
      )

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

  }
}
