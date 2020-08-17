package io.paytouch.core.messages.entities

import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities._

final case class PrepareCashDrawerReport(eventName: String, payload: EntityPayload[PrepareCashDrawerReportPayload])
    extends PtCoreMsg[PrepareCashDrawerReportPayload]

object PrepareCashDrawerReport {

  val eventName = "prepare_cash_drawer_report"

  def apply(payload: PrepareCashDrawerReportPayload)(implicit user: UserContext): PrepareCashDrawerReport =
    PrepareCashDrawerReport(
      eventName,
      EntityPayload(payload, payload.merchant.id, Some(payload.location.id), user.pusherSocketId),
    )
}

final case class PrepareCashDrawerReportPayload(
    cashDrawer: CashDrawer,
    merchant: Merchant,
    targetUsers: Seq[User],
    location: Location,
    locationReceipt: LocationReceipt,
    cashier: UserInfo,
  ) extends ExposedEntity {
  val classShortName = ExposedName.PrepareCashDrawerReportPayload
}

final case class CashDrawerReport(eventName: String, payload: EmailEntityPayload[CashDrawerReportPayload])
    extends PtNotifierMsg[CashDrawerReportPayload]

object CashDrawerReport {

  val eventName = "cash_drawer_report"

  def apply(recipientEmail: String, payload: CashDrawerReportPayload): CashDrawerReport =
    CashDrawerReport(eventName, EmailEntityPayload(recipientEmail, payload, payload.merchant.id))
}

final case class CashDrawerReportPayload(
    cashDrawer: CashDrawer,
    activitiesFileUrl: String,
    merchant: Merchant,
    location: Location,
    locationReceipt: LocationReceipt,
    cashier: UserInfo,
  ) extends ExposedEntity {
  val classShortName = ExposedName.CashDrawerReportPayload
}

object CashDrawerReportPayload {
  def apply(activitiesFileUrl: String, preparePayload: PrepareCashDrawerReportPayload): CashDrawerReportPayload =
    CashDrawerReportPayload(
      preparePayload.cashDrawer,
      activitiesFileUrl,
      preparePayload.merchant,
      preparePayload.location,
      preparePayload.locationReceipt,
      preparePayload.cashier,
    )
}
