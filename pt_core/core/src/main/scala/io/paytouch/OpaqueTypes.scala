package io.paytouch

// I think for now we can keep all opaque types in here and just let this file grow endlessly.
// Please keep them alphabetically sorted.

// Add Serializers to core/src/main/scala/io/paytouch/core/json/serializers/OpaqueTypeSerializers.scala
// Add Unmarshallers to core/src/main/scala/io/paytouch/core/json/serializers/CustomUnmarshallers.scala

import java.util.UUID

import io.paytouch.implicits._

final case class CountryCode(value: String) extends Opaque[String]
object CountryCode extends OpaqueCompanion[String, CountryCode]

final case class CountryName(value: String) extends Opaque[String]
case object CountryName extends OpaqueCompanion[String, CountryName]

object GiftCardPass {
  final case class Id(value: String) extends Opaque.Prism(IdPostgres, UUID.fromString) {
    override val productPrefix: String =
      "GiftCardPass.Id"
  }
  case object Id extends OpaqueCompanion[String, Id]

  final case class IdPostgres(value: UUID) extends Opaque.Iso[UUID, String, Id](Id, _.toString)
  case object IdPostgres extends OpaqueCompanion[UUID, IdPostgres]

  final case class OnlineCode(value: String) extends Opaque[String] {
    def hyphenated: OnlineCode.Hyphenated =
      OnlineCode.Hyphenated(value.hyphenatedAfterEvery(position = 4))
  }

  case object OnlineCode extends OpaqueCompanion[String, OnlineCode] {
    final case class Hyphenated(value: String) extends Opaque[String] {
      def hyphenless: OnlineCode =
        OnlineCode(value.hyphenless)

      override val productPrefix: String =
        "OnlineCode.Hyphenated"
    }

    case object Hyphenated extends OpaqueCompanion[String, Hyphenated]

    final case class Raw(value: String) extends Opaque[String] {
      override val productPrefix: String =
        "OnlineCode.Raw"
    }

    case object Raw extends OpaqueCompanion[String, Raw]
  }
}

final case class Minimum(value: Int) extends Opaque[Int]
object Minimum extends OpaqueCompanion[Int, Minimum]

final case class Maximum(value: Int) extends Opaque[Int]
object Maximum extends OpaqueCompanion[Int, Maximum]

final case class MerchantId(value: String) extends Opaque.Prism(MerchantIdPostgres, UUID.fromString)
case object MerchantId extends OpaqueCompanion[String, MerchantId]

final case class MerchantIdPostgres(value: UUID) extends Opaque.Iso[UUID, String, MerchantId](MerchantId, _.toString)
case object MerchantIdPostgres extends OpaqueCompanion[UUID, MerchantIdPostgres]

final case class OrderId(value: String) extends Opaque.Prism(OrderIdPostgres, UUID.fromString)
case object OrderId extends OpaqueCompanion[String, OrderId]

final case class OrderIdPostgres(value: UUID) extends Opaque.Iso[UUID, String, OrderId](OrderId, _.toString)
case object OrderIdPostgres extends OpaqueCompanion[UUID, OrderIdPostgres]

final case class CatalogId(value: String) extends Opaque.Prism(CatalogIdPostgres, UUID.fromString)
case object CatalogId extends OpaqueCompanion[String, CatalogId]

final case class CatalogIdPostgres(value: UUID) extends Opaque.Iso[UUID, String, CatalogId](CatalogId, _.toString)
case object CatalogIdPostgres extends OpaqueCompanion[UUID, CatalogIdPostgres]

final case class StateCode(value: String) extends Opaque[String]
case object StateCode extends OpaqueCompanion[String, StateCode]

final case class StateName(value: String) extends Opaque[String]
case object StateName extends OpaqueCompanion[String, StateName]

final case class Auth0UserId(value: String) extends Opaque[String]
case object Auth0UserId extends OpaqueCompanion[String, Auth0UserId]
