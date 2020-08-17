package io.paytouch.core.reports.resources.giftcardpasses

import io.paytouch.core.reports.resources.ReportsAggrFSpec
import io.paytouch.core.reports.views.GiftCardPassView

abstract class GiftCardPassesFSpec extends ReportsAggrFSpec[GiftCardPassView] {

  def view = GiftCardPassView

  val fixtures = new GiftCardPassesFSpecContext

  class GiftCardPassesFSpecContext extends ReportsAggrFSpecContext with GiftCardPassesFSpecFixtures
}
