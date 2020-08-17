package io.paytouch.core.entities

import java.util.UUID

final case class VariantOptionType(
    id: UUID,
    name: String,
    position: Int,
    options: Seq[VariantOption],
  )

final case class VariantOption(
    id: UUID,
    name: String,
    position: Int,
  )

final case class VariantOptionTypeUpsertion(
    id: UUID,
    name: String,
    position: Option[Int],
    options: Seq[VariantOptionUpsertion],
  )

final case class VariantOptionUpsertion(
    id: UUID,
    name: String,
    position: Option[Int],
  )

final case class VariantOptionWithType(
    id: UUID,
    name: String,
    typeName: String,
    position: Int,
    typePosition: Int,
  )

object VariantOptionWithType {

  def headers(seq: Seq[Seq[VariantOptionWithType]]): List[String] =
    seq.map(_.size) match {
      case sizes if sizes.isEmpty => List.empty
      case sizes                  => headers(sizes.max)
    }

  private def headers(maxIndex: Int): List[String] =
    (1 to maxIndex).toList.flatMap(idx => List(s"Variant Option $idx", s"Variant Option Type $idx"))

  def rows(seq: Seq[VariantOptionWithType]): List[String] =
    seq.toList.flatMap(vot => List(vot.name, vot.typeName))
}
