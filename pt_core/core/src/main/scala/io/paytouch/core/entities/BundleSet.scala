package io.paytouch.core.entities

import java.util.UUID

final case class BundleSet(
    id: UUID,
    name: Option[String],
    position: Int,
    minQuantity: Int,
    maxQuantity: Int,
    options: Seq[BundleOption],
  )

final case class BundleSetCreation(
    id: UUID,
    name: Option[String],
    position: Int,
    minQuantity: Int,
    maxQuantity: Int,
    options: Option[Seq[BundleOptionUpdate]],
  ) {
  def asUpdate =
    BundleSetUpdate(
      id = id,
      name = Some(name),
      position = Some(position),
      minQuantity = Some(minQuantity),
      maxQuantity = Some(maxQuantity),
      options = options,
    )
}

final case class BundleSetUpdate(
    id: UUID,
    name: ResettableString,
    position: Option[Int],
    minQuantity: Option[Int],
    maxQuantity: Option[Int],
    options: Option[Seq[BundleOptionUpdate]],
  )
