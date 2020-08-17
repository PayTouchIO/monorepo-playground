package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model._

final case class TransferOrderUpsertion(
    transferOrder: TransferOrderUpdate,
    transferOrderProducts: Option[Seq[TransferOrderProductUpdate]],
  ) extends UpsertionModel[TransferOrderRecord]
