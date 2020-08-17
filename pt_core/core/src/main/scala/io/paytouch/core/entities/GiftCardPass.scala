package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class GiftCardPass(
    id: UUID,
    lookupId: String,
    giftCardId: UUID,
    orderItemId: UUID,
    originalBalance: MonetaryAmount,
    balance: MonetaryAmount,
    passPublicUrls: PassUrls,
    transactions: Option[Seq[GiftCardPassTransaction]],
    passInstalledAt: Option[ZonedDateTime],
    recipientEmail: Option[String],
    onlineCode: io.paytouch.GiftCardPass.OnlineCode.Hyphenated,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.GiftCardPass
}
