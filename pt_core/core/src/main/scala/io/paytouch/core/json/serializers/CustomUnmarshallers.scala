package io.paytouch.core.json.serializers

import java.time._
import java.util.UUID

import scala.collection.immutable
import scala.concurrent._
import scala.util._
import scala.util.control.NoStackTrace

import akka.http.scaladsl.unmarshalling.FromStringUnmarshaller
import akka.http.scaladsl.unmarshalling.Unmarshaller.NoContentException
import akka.stream.Materializer

import cats.implicits._

import enumeratum._

import io.paytouch._

import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.enums._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.entities.enums.ops.GroupOrderByFields
import io.paytouch.core.utils.UtcTime

trait CustomUnmarshallers {
  implicit object UUIDUnmarshaller extends FromStringUnmarshaller[UUID] {
    def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[UUID] =
      Future(UUID.fromString(value))
  }

  implicit object UUIDsUnmarshaller extends FromStringUnmarshaller[Seq[UUID]] {
    def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[Seq[UUID]] =
      Future {
        val seq: Seq[UUID] =
          immutable.ArraySeq.unsafeWrapArray(value.split(",").filter(_.trim.nonEmpty).map(UUID.fromString))

        if (seq.nonEmpty)
          seq
        else
          throw NoContentException
      }
  }

  implicit object PositiveBigDecimalUnmarshaller extends FromStringUnmarshaller[BigDecimal] {
    def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[BigDecimal] =
      Future {
        Try(BigDecimal(value)) match {
          case Success(bg) if bg < 0 => throw new RuntimeException("BigDecimal cannot be negative") with NoStackTrace
          case Success(bg)           => bg
          case Failure(ex)           => throw ex
        }
      }
  }

  implicit object ZonedDateTimeUnmarshaller extends FromStringUnmarshaller[ZonedDateTime] {
    def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[ZonedDateTime] =
      Future(ZonedDateTime.parse(value)) fallbackTo LocalDateTimeUnmarshaller(value).map(UtcTime.ofLocalDateTime)
  }

  implicit object LocalDateTimeUnmarshaller extends FromStringUnmarshaller[LocalDateTime] {
    def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[LocalDateTime] =
      Future(LocalDateTime.parse(value)) fallbackTo LocalDateUnmarshaller(value).map(_.atStartOfDay)
  }

  implicit object LocalDateUnmarshaller extends FromStringUnmarshaller[LocalDate] {
    def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[LocalDate] = {
      val dateValue = value.split("T").head
      Future(LocalDate.parse(dateValue))
    }
  }

  implicit object AcceptanceStatusUnmarshaller extends EnumUnmarshaller(AcceptanceStatus)
  implicit object ArticleScopeUnmarshaller extends EnumUnmarshaller(ArticleScope)
  implicit object ArticleTypeAliasUnmarshaller extends EnumUnmarshaller(ArticleTypeAlias)
  implicit object BusinessTypeUnmarshaller extends EnumUnmarshaller(BusinessType)
  implicit object CancellationStatusUnmarshaller extends EnumUnmarshaller(CancellationStatus)
  implicit object CustomerGroupByUnmarshaller extends EnumUnmarshaller(CustomerGroupBy)
  implicit object CustomerSourceAliasUnmarshaller extends EnumUnmarshaller(CustomerSourceAlias)
  implicit object CustomerSourceUnmarshaller extends EnumUnmarshaller(CustomerSource)
  implicit object DeliveryProviderUnmarshaller extends EnumUnmarshaller(DeliveryProvider)
  implicit object ExposedNameUnmarshaller extends EnumUnmarshaller(ExposedName)
  implicit object FeedbackStatusUnmarshaller extends EnumUnmarshaller(FeedbackStatus)
  implicit object GiftCardPassGroupByUnmarshaller extends EnumUnmarshaller(GiftCardPassGroupBy)
  implicit object HandledViaUnmarshaller extends EnumUnmarshaller(HandledVia)
  implicit object ImageUploadTypeUnmarshaller extends EnumUnmarshaller(ImageUploadType)
  implicit object InventoryCountStatusUnmarshaller extends EnumUnmarshaller(InventoryCountStatus)
  implicit object LoginSourceUnmarshaller extends EnumUnmarshaller(LoginSource)
  implicit object LoyaltyOrdersGroupByUnmarshaller extends EnumUnmarshaller(LoyaltyOrdersGroupBy)
  implicit object MerchantModeUnmarshaller extends EnumUnmarshaller(MerchantMode)
  implicit object MerchantSetupStepsUnmarshaller extends EnumUnmarshaller(MerchantSetupSteps)
  implicit object NoGroupByUnmarshaller extends EnumUnmarshaller(NoGroupBy)
  implicit object OrderGroupByUnmarshaller extends EnumUnmarshaller(OrderGroupBy)
  implicit object OrderStatusUnmarshaller extends EnumUnmarshaller(OrderStatus)
  implicit object PassItemTypeUnmarshaller extends EnumUnmarshaller(PassItemType)
  implicit object PassTypeUnmarshaller extends EnumUnmarshaller(PassType)
  implicit object PaymentStatusUnmarshaller extends EnumUnmarshaller(PaymentStatus)
  implicit object PaymentTypeUnmarshaller extends EnumUnmarshaller(OrderPaymentType)
  implicit object ProductSalesGroupByUnmarshaller extends EnumUnmarshaller(ProductSalesGroupBy)
  implicit object ReceivingObjectStatusUnmarshaller extends EnumUnmarshaller(ReceivingObjectStatus)
  implicit object ReceivingOrderStatusUnmarshaller extends EnumUnmarshaller(ReceivingOrderStatus)
  implicit object ReceivingOrderViewUnmarshaller extends EnumUnmarshaller(ReceivingOrderView)
  implicit object ReportIntervalUnmarshaller extends EnumUnmarshaller(ReportInterval)
  implicit object ReturnOrderStatusUnmarshaller extends EnumUnmarshaller(ReturnOrderStatus)
  implicit object SalesGroupByUnmarshaller extends EnumUnmarshaller(SalesGroupBy)
  implicit object ShiftStatusUnmarshaller extends EnumUnmarshaller(ShiftStatus)
  implicit object SourceUnmarshaller extends EnumUnmarshaller(Source)
  implicit object TaxGroupByUnmarshaller extends EnumUnmarshaller(OrderTaxRateGroupBy)
  implicit object TicketStatusUnmarshaller extends EnumUnmarshaller(TicketStatus)
  implicit object TimeCardStatusUnmarshaller extends EnumUnmarshaller(TimeCardStatus)
  implicit object TipsHandlingModeUnmarshaller extends EnumUnmarshaller(TipsHandlingMode)
  implicit object TrackableActionUnmarshaller extends EnumUnmarshaller(TrackableAction)
  implicit object ViewUnmarshaller extends EnumUnmarshaller(View)

  class EnumUnmarshaller[A <: EnumEntry: Manifest](enum: Enum[A]) extends FromStringUnmarshaller[A] {
    def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[A] =
      Future(enum.withName(value))
  }

  implicit object SeqArticleTypeAliasUnmarshaller extends SeqEnumUnmarshaller(ArticleTypeAlias)
  implicit object SeqCategorySalesOrderByFieldUnmarshaller extends SeqEnumUnmarshaller(CategorySalesOrderByFields)
  implicit object SeqCustomerFieldUnmarshaller extends SeqEnumUnmarshaller(CustomerFields)
  implicit object SeqCustomerOrderByFieldUnmarshaller extends SeqEnumUnmarshaller(CustomerOrderByFields)
  implicit object SeqCustomerSourceAliasUnmarshaller extends SeqEnumUnmarshaller(CustomerSourceAlias)
  implicit object SeqCustomerSourceUnmarshaller extends SeqEnumUnmarshaller(CustomerSource)
  implicit object SeqEmployeeSalesFieldUnmarshaller extends SeqEnumUnmarshaller(EmployeeSalesFields)
  implicit object SeqEmployeeSalesOrderByFieldUnmarshaller extends SeqEnumUnmarshaller(EmployeeSalesOrderByFields)
  implicit object SeqGiftCardPassFieldUnmarshaller extends SeqEnumUnmarshaller(GiftCardPassFields)
  implicit object SeqGroupOrderByFieldUnmarshaller extends SeqEnumUnmarshaller(GroupOrderByFields)
  implicit object SeqLocationGiftCardPassesOrderByFieldUnmarshaller
      extends SeqEnumUnmarshaller(LocationGiftCardPassesOrderByFields)
  implicit object SeqLocationGiftCardPassFieldUnmarshaller extends SeqEnumUnmarshaller(LocationGiftCardPassFields)
  implicit object SeqLocationSalesFieldUnmarshaller extends SeqEnumUnmarshaller(LocationSalesFields)
  implicit object SeqLocationSalesOrderByFieldUnmarshaller extends SeqEnumUnmarshaller(LocationSalesOrderByFields)
  implicit object SeqLoyaltyOrdersFieldUnmarshaller extends SeqEnumUnmarshaller(LoyaltyOrdersFields)
  implicit object SeqOrderFieldUnmarshaller extends SeqEnumUnmarshaller(OrderFields)
  implicit object SeqOrderItemSalesFieldUnmarshaller extends SeqEnumUnmarshaller(OrderItemSalesFields)
  implicit object SeqOrderStatusUnmarshaller extends SeqEnumUnmarshaller(OrderStatus)
  implicit object SeqOrderTaxRateFieldUnmarshaller extends SeqEnumUnmarshaller(OrderTaxRateFields)
  implicit object SeqOrderTypeUnmarshaller extends SeqEnumUnmarshaller(OrderType)
  implicit object SeqProductOrderByFieldUnmarshaller extends SeqEnumUnmarshaller(ProductOrderByFields)
  implicit object SeqProductSalesFieldUnmarshaller extends SeqEnumUnmarshaller(ProductSalesFields)
  implicit object SeqProductSalesOrderByFieldUnmarshaller extends SeqEnumUnmarshaller(ProductSalesOrderByFields)
  implicit object SeqRewardRedemptionsFieldUnmarshaller extends SeqEnumUnmarshaller(RewardRedemptionsFields)
  implicit object SeqSalesFieldUnmarshaller extends SeqEnumUnmarshaller(SalesFields)
  implicit object SeqSourceUnmarshaller extends SeqEnumUnmarshaller(Source)

  class SeqEnumUnmarshaller[A <: EnumEntry: Manifest](enum: Enum[A]) extends FromStringUnmarshaller[Seq[A]] {
    def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[Seq[A]] =
      Future {
        val seq: Seq[A] =
          immutable.ArraySeq.unsafeWrapArray(value.split(",").filter(_.trim.nonEmpty).map(enum.withName))

        if (seq.nonEmpty)
          seq
        else
          throw NoContentException
      }
  }

  implicit object CountryCodeUnmarshaller extends OpaqueStringUnmarshaller(CountryCode)
  implicit object CountryNameUnmarshaller extends OpaqueStringUnmarshaller(CountryName)
  implicit object GiftCardPassOnlineCodeHyphentaedUnmarshaller
      extends OpaqueStringUnmarshaller(GiftCardPass.OnlineCode.Hyphenated)
  implicit object GiftCardPassOnlineCodeRawUnmarshaller extends OpaqueStringUnmarshaller(GiftCardPass.OnlineCode.Raw)
  implicit object GiftCardPassOnlineCodeUnmarshaller extends OpaqueStringUnmarshaller(GiftCardPass.OnlineCode)
  implicit object MerchantIdUnmarshaller extends OpaqueStringUnmarshaller(MerchantId)
  implicit object OrderIdUnmarshaller extends OpaqueStringUnmarshaller(OrderId)
  implicit object CatalogIdUnmarshaller extends OpaqueStringUnmarshaller(CatalogId)
  implicit object StateCodeUnmarshaller extends OpaqueStringUnmarshaller(StateCode)
  implicit object StateNameUnmarshaller extends OpaqueStringUnmarshaller(StateName)

  class OpaqueStringUnmarshaller[O <: Opaque[String]: Manifest](f: String => O) extends FromStringUnmarshaller[O] {
    override def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[O] =
      f(value).pure[Future]
  }

  implicit object SeqCatalogIdUnmarshaller extends OpaqueStringSeqUnmarshaller(CatalogId)

  class OpaqueStringSeqUnmarshaller[O <: Opaque[String]: Manifest](f: String => O)
      extends FromStringUnmarshaller[Seq[O]] {
    override def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[Seq[O]] =
      Future {
        val seq: Seq[O] =
          immutable.ArraySeq.unsafeWrapArray(value.split(",").filter(_.trim.nonEmpty).map(f))

        if (seq.nonEmpty)
          seq
        else
          throw NoContentException
      }
  }
}
