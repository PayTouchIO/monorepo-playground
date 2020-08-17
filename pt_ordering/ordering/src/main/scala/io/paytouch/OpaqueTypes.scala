package io.paytouch

// I think for now we can keep all opaque types in here and just let this file grow endlessly.
// Please keep them alphabetically sorted.

// Add Serializers to ordering/src/main/scala/io/paytouch/ordering/json/serializers/OpaqueTypeSerializers.scala
// Add Unmarshallers to ordering/src/main/scala/io/paytouch/ordering/json/serializers/CustomUnmarshallers.scala

import java.util.UUID

import io.paytouch.implicits._

object GiftCardPass {
  final case class Id(value: String) extends Opaque.Prism(IdPostgres, UUID.fromString) {
    override val productPrefix: String =
      "GiftCardPass.Id"
  }
  case object Id extends OpaqueCompanion[String, Id]

  final case class IdPostgres(value: UUID) extends Opaque.Iso[UUID, String, Id](Id, _.toString)
  case object IdPostgres extends OpaqueCompanion[UUID, IdPostgres]

  case object OnlineCode {
    final case class Raw(value: String) extends Opaque[String] {
      override val productPrefix: String =
        "OnlineCode.Raw"
    }

    case object Raw extends OpaqueCompanion[String, Raw]
  }
}

final case class OrderId(value: String) extends Opaque.Prism(OrderIdPostgres, UUID.fromString)
case object OrderId extends OpaqueCompanion[String, OrderId]

final case class OrderIdPostgres(value: UUID) extends Opaque.Iso[UUID, String, OrderId](OrderId, _.toString)
case object OrderIdPostgres extends OpaqueCompanion[UUID, OrderIdPostgres]
