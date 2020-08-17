package io.paytouch.ordering.data.driver

import java.util.{ Currency, UUID }

import scala.reflect.ClassTag

import akka.http.scaladsl.model.Uri

import enumeratum.EnumEntry

import io.paytouch.ordering.clients.paytouch.core.entities.enums.CartItemType
import io.paytouch.ordering.clients.paytouch.core.entities.ImageUrls
import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.{ PaymentProcessorConfig, WorldpayPaymentType }
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums._
import io.paytouch.ordering.entities.worldpay.WorldpayPaymentStatus
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.json.JsonSupport.JValue

trait CustomColumnMappers extends JsonSupport {
  private def jsonMapper[T: ClassTag](implicit m: Manifest[T]) =
    MappedColumnType.base[T, JValue](pds => fromEntityToJValue(pds), json => json.extract[T])

  private def enumMapper[T <: EnumEntry: ClassTag](f: String => T) =
    MappedColumnType.base[T, String](_.entryName, f)

  implicit val currencyMapper =
    MappedColumnType.base[Currency, String](_.getCurrencyCode, Currency.getInstance)

  implicit val uriMapper =
    MappedColumnType.base[Uri, String](_.toString, Uri(_))

  implicit val cartItemTypeMapper = enumMapper(CartItemType.withName)
  implicit val cartStatusMapper = enumMapper(CartStatus.withName)
  implicit val modifierSetTypeMapper = enumMapper(ModifierSetType.withName)
  implicit val orderTypeMapper = enumMapper(OrderType.withName)
  implicit val paymentIntentStatusMapper = enumMapper(PaymentIntentStatus.withName)
  implicit val paymentMethodTypeMapper = enumMapper(PaymentMethodType.withName)
  implicit val paymentProcessorCallbackStatusMapper = enumMapper(PaymentProcessorCallbackStatus.withName)
  implicit val paymentProcessorMapper = enumMapper(PaymentProcessor.withName)
  implicit val unitTypeMapper = enumMapper(UnitType.withName)
  implicit val worldpayPaymentStatusMapper = enumMapper(WorldpayPaymentStatus.withName)
  implicit val worldpayPaymentTypeMapper = enumMapper(WorldpayPaymentType.withName)

  implicit val addressMapper = jsonMapper[Address]
  implicit val appliedGiftCardPassMapper = jsonMapper[Seq[GiftCardPassApplied]]
  implicit val giftCardDataMapper = jsonMapper[GiftCardData]
  implicit val mapOfStringMapper = jsonMapper[Map[String, String]]
  implicit val paymentIntentMetadataMapper = jsonMapper[PaymentIntentMetadata]
  implicit val paymentProcessorConfigMapper = jsonMapper[PaymentProcessorConfig]
  implicit val seqCartItemBundleSetMapper = jsonMapper[Seq[CartItemBundleSet]]
  implicit val seqImageUrlsMapper = jsonMapper[Seq[ImageUrls]]
  implicit val seqPaymentMethodMapper = jsonMapper[Seq[PaymentMethod]]
  implicit val seqUuidMapper = jsonMapper[Seq[UUID]]
}
