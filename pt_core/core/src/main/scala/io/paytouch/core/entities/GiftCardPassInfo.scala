package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.GiftCardPassRecord

final case class GiftCardPassInfo(
    id: UUID,
    lookupId: String,
    onlineCode: io.paytouch.GiftCardPass.OnlineCode.Hyphenated,
    recipientEmail: Option[String],
  )

object GiftCardPassInfo {
  def fromRecord(record: GiftCardPassRecord) =
    GiftCardPassInfo(
      id = record.id,
      lookupId = record.lookupId,
      onlineCode = record.onlineCode.hyphenated,
      recipientEmail = record.recipientEmail,
    )
}
