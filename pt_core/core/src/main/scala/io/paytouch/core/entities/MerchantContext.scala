package io.paytouch.core.entities

import java.util.{ Currency, UUID }

import io.paytouch.core.data.model.MerchantRecord

final case class MerchantContext(
    id: UUID,
    currency: Currency,
    pusherSocketId: Option[String] = None,
  )

object MerchantContext {

  def extract(record: MerchantRecord): MerchantContext = apply(record.id, record.currency)
  def extract(entity: Merchant): MerchantContext = apply(entity.id, entity.currency)
}
