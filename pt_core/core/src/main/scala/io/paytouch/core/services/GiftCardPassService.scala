package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import cats.data._
import cats.implicits._

import com.typesafe.scalalogging.LazyLogging

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core._
import io.paytouch.core.barcodes.entities.BarcodeMetadata
import io.paytouch.core.barcodes.services.BarcodeService
import io.paytouch.core.calculations.LookupIdUtils
import io.paytouch.core.clients.urbanairship.entities.Pass
import io.paytouch.core.conversions.GiftCardPassConversions
import io.paytouch.core.data.daos._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities.enums.PassType
import io.paytouch.core.expansions._
import io.paytouch.core.filters._
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.services.features.FindByIdFeature
import io.paytouch.core.utils.UtcTime
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.utils.Multiple
import io.paytouch.core.validators._

class GiftCardPassService(
    barcodeService: BarcodeService,
    giftCardService: GiftCardService,
    val giftCardPassTransactionService: GiftCardPassTransactionService,
    val locationReceiptService: LocationReceiptService,
    val locationSettingsService: LocationSettingsService,
    val merchantService: MerchantService,
    messageHandler: SQSMessageHandler,
    orderService: => OrderService,
    orderItemService: => OrderItemService,
    val passService: PassService,
    urbanAirshipService: UrbanAirshipService,
    generateOnlineCode: GiftCardPassService.GenerateOnlineCode,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends GiftCardPassConversions
       with FindByIdFeature
       with LookupIdUtils
       with LazyLogging {
  import GiftCardPassService._

  type Record = GiftCardPassRecord
  type Update = GiftCardPassUpdate
  type Entity = entities.GiftCardPass
  type Expansions = GiftCardPassExpansions
  type Filters = NoFilters
  type Validator = GiftCardPassValidator
  type Dao = GiftCardPassDao

  val defaultFilters = NoFilters()
  protected val validator = new GiftCardPassValidator

  protected val dao = daos.giftCardPassDao
  val giftCardDao = daos.giftCardDao

  override def enrich(
      records: Seq[Record],
      filters: Filters,
    )(
      e: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] =
    getOptionalTransactionsPerGiftCardPass(records)(e.withTransactions)
      .map(fromRecordsAndOptionsToEntities(records, _))

  private def getOptionalTransactionsPerGiftCardPass(
      records: Seq[Record],
    )(
      withTransactions: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataSeqByRecord[GiftCardPassTransaction]] =
    getExpandedMappedField[Seq[GiftCardPassTransaction]](
      giftCardPassTransactionService.findAllPerGiftCardId,
      _.id,
      records,
      withTransactions,
    )

  def findByLookupId(
      lookupId: String,
      filters: Filters = defaultFilters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Option[Entity]] =
    dao
      .findOneByMerchantIdAndLookupId(user.merchantId, lookupId)
      .flatMap(enrich(_, filters)(expansions))

  def findByOnlineCode(
      onlineCode: io.paytouch.GiftCardPass.OnlineCode.Raw,
      filters: Filters = defaultFilters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Option[Entity]] =
    dao
      .findOneByMerchantIdAndOnlineCode(user.merchantId, onlineCode)
      .flatMap(enrich(_, filters)(expansions))

  def findByOrderItemId(
      orderItemId: UUID,
      filters: Filters = defaultFilters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Option[Entity]] =
    dao
      .findByOrderItemId(orderItemId)
      .flatMap(enrich(_, filters)(expansions))

  def findInfoByOrderItemIds(orderItemIds: Seq[UUID]): Future[Map[UUID, GiftCardPassInfo]] =
    dao
      .findByOrderItemIds(orderItemIds)
      .map(
        _.groupBy(_.orderItemId)
          .filter { case (_, v) => v.nonEmpty }
          .transform((_, v) => v.map(GiftCardPassInfo.fromRecord).head),
      )

  def findRecordById(id: UUID): Future[Option[Record]] = findRecordsByIds(Seq(id)).map(_.headOption)

  def findRecordsByIds(ids: Seq[UUID]): Future[Seq[Record]] = dao.findByIds(ids)

  def decreaseBalance(
      id: UUID,
      amount: BigDecimal,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[GiftCardPassTransaction]]] =
    validator
      .validateBalanceDecrease(id, amount)
      .flatMapTraverse { giftCardPassRecord =>
        giftCardPassRecord
          .as {
            (for {
              (record, transactionRecord) <- OptionT(dao.decreaseBalance(id, amount))
              entity <- OptionT.liftF(enrich(record, defaultFilters)(GiftCardPassExpansions(withTransactions = true)))
              _ = messageHandler.sendGiftCardPassChangedMsg(entity)
            } yield giftCardPassTransactionService.fromRecordToEntityWithPass(transactionRecord, Some(entity))).value
          }
          .getOrElse(Future.successful(None))
      }

  def decreaseBalance(
      orderId: OrderId,
      bulkCharge: Seq[GiftCardPassCharge],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Order]]] =
    validator
      .validateBalanceDecrease(orderId, bulkCharge)
      .flatMap {
        case Validated.Valid(_) =>
          def seqFutures[T, U](xs: IterableOnce[T])(f: T => Future[U])(implicit ec: ExecutionContext)
              : Future[List[U]] = {
            val resBase = Future.successful(scala.collection.mutable.ListBuffer.empty[U])
            xs.iterator
              .foldLeft(resBase) { (futureRes, x) =>
                futureRes.flatMap { res =>
                  f(x).map(res += _)
                }
              }
              .map(_.toList)
          }
          for {
            data <-
              dao
                .decreaseBalance(
                  bulkCharge.map(p => p.giftCardPassId -> p.amount).toMap,
                )
            entities <-
              data
                .toList
                .traverse {
                  case (record, transactionRecord) =>
                    enrich(record, defaultFilters)(GiftCardPassExpansions(withTransactions = true))
                      .map(_ -> transactionRecord)
                }
            _ <-
              // ensure storePaymentTransaction runs sequentially so next iteration "sees" the updated results in the db
              seqFutures(entities) {
                case (entity, transactionRecord) =>
                  orderService.storePaymentTransaction(
                    orderId.cast.get.value,
                    createTransaction(
                      entity,
                      transactionRecord,
                    ),
                  )
              }
            order <- orderService.findOpenById(orderId.cast.get.value, orderService.defaultFilters)(
              orderService.defaultExpansions.copy(withPaymentTransactions = true),
            )
          } yield {
            entities.foreach {
              case (entity, _) =>
                messageHandler.sendGiftCardPassChangedMsg(entity)
            }

            Multiple.success(order)
          }

        case i @ Validated.Invalid(_) =>
          i.pure[Future]
      }

  private[this] def createTransaction(
      giftCardPass: GiftCardPass,
      giftCardPassTransactionRecord: GiftCardPassTransactionRecord,
    )(implicit
      user: UserContext,
    ): OrderService.PaymentTransactionUpsertion =
    OrderService.PaymentTransactionUpsertion(
      id = UUID.randomUUID(),
      `type` = TransactionType.Payment,
      paymentType = TransactionPaymentType.GiftCard,
      paymentDetails = PaymentDetails(
        amount = giftCardPassTransactionRecord.totalAmount.abs.some, /* : Option[BigDecimal] = None, */
        currency = user.currency.some, /* : Option[Currency] = None, */
        authCode = None, /* : Option[String] = None, */
        maskPan = None, /* : Option[String] = None, */
        cardHash = None, /* : Option[String] = None, */
        cardReference = None, /* : Option[String] = None, */
        cardType = None, /* : Option[CardType] = None, */
        terminalName = None, /* : Option[String] = None, */
        terminalId = None, /* : Option[String] = None, */
        transactionResult = CardTransactionResultType.Approved.some, /* : Option[CardTransactionResultType] = None, */
        transactionStatus = None, /* : Option[CardTransactionStatusType] = None, */
        transactionReference = None, /* : Option[String] = None, */
        last4Digits = None, /* : Option[String] = None, */
        paidInAmount = None, /* : Option[BigDecimal] = None, */
        paidOutAmount = None, /* : Option[BigDecimal] = None, */
        batchNumber = None, /* : Option[Int] = None, */
        merchantFee = None, /* : Option[BigDecimal] = None, */
        giftCardPassId = giftCardPassTransactionRecord.giftCardPassId.some, /* : Option[UUID] = None, */
        giftCardPassTransactionId = giftCardPassTransactionRecord.id.some, /* : Option[UUID] = None, */
        giftCardPassLookupId = giftCardPass.lookupId.some, /* : Option[String] = None, */
        isStandalone = None, /* : Option[Boolean] = None, */
        cashbackAmount = None, /* : Option[BigDecimal] = None, */
        customerId = None, /* : Option[UUID] = None, */
        tipAmount = 0, /* : BigDecimal = 0, */
        preauth = false, /* : Boolean = false, */
        terminalVerificationResult = None, /* : Option[String] = None, */
        applicationDedicatedFile = None, /* : Option[String] = None, */
        transactionStatusInfo = None, /* : Option[String] = None, */
        applicationLabel = None, /* : Option[String] = None, */
        applicationId = None, /* : Option[String] = None, */
        accountId = None, /* : Option[String] = None, */
        paymentAccountId = None, /* : Option[String] = None, */
        entryMode = None, /* : Option[String] = None, // Manual, Swipe, Chip, Contactless, Scan, Check Reader */
        transactionNumber = None, /* : Option[String] = None, */
        gatewayTransactionReference = None, /* : Option[String] = None, */
        cardHolderName = None, /* : Option[String] = None, */
        worldpay = None, /* : Option[JsonSupport.JValue] = None, */
        cryptogram = None, /* : Option[String] = None, */
        signatureRequired = None, /* : Option[Boolean] = None, */
        pinVerified = None, /* : Option[Boolean] = None, */
      ),
      paidAt = giftCardPassTransactionRecord.createdAt,
      version = 1,
      paymentProcessor = TransactionPaymentProcessor.Paytouch,
    )

  def convertToGiftCardPassUpdates(
      upsertion: RecoveredOrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[Update]]] = {
    val itemsWithGiftCards = upsertion.items.filter(_.withGiftCardCreation)

    if (itemsWithGiftCards.isEmpty)
      Future.successful(Multiple.success(Seq.empty))
    else
      validator.validateCreation(upsertion).flatMapTraverse { giftCard =>
        itemsWithGiftCards.toList.traverse { item =>
          generateOnlineCodeRetryingForeverIfItExists().map { onlineCode =>
            val id = UUID.randomUUID

            fromUpsertionToUpdate(
              id,
              lookupId = generateLookupId(id),
              giftCard = giftCard,
              item = item,
              onlineCode = onlineCode,
            )
          }
        }
      }
  }

  def createGiftCardPasses(
      items: Seq[OrderItemRecord],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[Entity]]] =
    validator.validateGiftCardExists.flatMapTraverse { giftCard =>
      items
        .toList
        .traverse { item =>
          generateOnlineCodeRetryingForeverIfItExists().map { onlineCode =>
            val id = UUID.randomUUID

            fromUpsertionToUpdate(
              id,
              lookupId = generateLookupId(id),
              giftCard = giftCard,
              item = item,
              onlineCode = onlineCode,
            )
          }
        }
        .flatMap {
          dao.bulkUpsert(_).flatMap { results =>
            enrich(results.map(_._2), defaultFilters)(GiftCardPassExpansions.empty)
          }
        }
    }

  def sendGiftCardPassReceipts(
      orderItems: Seq[OrderItemRecord],
      giftCardPasses: Seq[Entity],
    )(implicit
      user: UserContext,
    ): Future[Unit] =
    orderItems
      .toList
      .map(item => item.id -> item.giftCardPassRecipientEmail)
      .collect {
        case (orderItemId, Some(recipientEmail)) =>
          giftCardPasses
            .find(pass => Boolean.and(pass.orderItemId == orderItemId, pass.recipientEmail.isEmpty))
            .fold(Future.unit) { _ =>
              sendReceipt(orderItemId, SendReceiptData(recipientEmail)).void
            }
      }
      .sequence
      .void

  private def generateOnlineCodeRetryingForeverIfItExists(): Future[io.paytouch.GiftCardPass.OnlineCode] = {
    import io.paytouch.GiftCardPass.OnlineCode

    def loop(onlineCode: OnlineCode): Future[OnlineCode] =
      dao.doesOnlineCodeExist(onlineCode).flatMap {
        case true  => loop(generateOnlineCode())
        case false => onlineCode.pure[Future]
      }

    loop(generateOnlineCode())
  }

  def upsertPassesByOrderItemIds(orderItemIds: Seq[UUID])(implicit user: UserContext) =
    dao.findByOrderItemIds(orderItemIds).map(upsertPasses)

  def upsertPasses(giftCardPasses: Seq[GiftCardPassRecord])(implicit user: UserContext): Future[Unit] =
    enrich(giftCardPasses, defaultFilters)(GiftCardPassExpansions(withTransactions = true))
      .map(_.foreach(messageHandler.sendGiftCardPassChangedMsg))

  def upsertPass(
      giftCardPass: entities.GiftCardPass,
    )(implicit
      user: UserContext,
    ): Future[Option[entities.GiftCardPass]] =
    giftCardService.findRecordById(giftCardPass.giftCardId).flatMap {
      case Some(lp) =>
        for {
          _ <- Future.sequence(PassType.values.map(upsertPass(_, giftCardPass, lp)))
          giftCardPass <- findById(giftCardPass.id, defaultFilters)(GiftCardPassExpansions(withTransactions = true))
        } yield giftCardPass
      case _ => Future.successful(Some(giftCardPass))
    }

  private def upsertPass(
      passType: PassType,
      giftCardPass: entities.GiftCardPass,
      giftCardRecord: GiftCardRecord,
    )(implicit
      user: UserContext,
    ): Future[Unit] =
    giftCardRecord.templateIdByPassType(passType) match {
      case None =>
        logger.warn(s"Attempted to create pass $passType for passData $giftCardPass without a valid template id")
        Future.successful(None)
      case Some(templateId) =>
        urbanAirshipService.upsertPass(templateId, passType, giftCardPass).flatMap {
          case pass: Pass => updatePassPublicUrl(giftCardPass.id, passType, pass.publicUrl.path).void
          case _          => Future.successful(())
        }
    }

  private def updatePassPublicUrl(
      loyaltyMembershipId: UUID,
      passType: PassType,
      url: String,
    )(implicit
      user: UserContext,
    ): Future[Option[Entity]] =
    passType match {
      case PassType.Android => dao.updateAndroidPassPublicUrl(loyaltyMembershipId, url)
      case PassType.Ios     => dao.updateIosPassPublicUrl(loyaltyMembershipId, url)
    }

  def sendReceipt(
      orderItemId: UUID,
      sendReceiptData: SendReceiptData,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    validator
      .canSendReceipt(orderItemId, sendReceiptData)
      .flatMapTraverse { giftCardPass =>
        (for {
          updatedGiftCardPass <- OptionT(
            dao.updateRecipientEmail(giftCardPass.id, sendReceiptData.recipientEmail),
          )
          giftCardPassEntity <- OptionT.liftF(
            enrich(updatedGiftCardPass, defaultFilters)(GiftCardPassExpansions(withTransactions = true)),
          )
        } yield messageHandler.sendPrepareGiftCardPassReceiptRequestedMsg(giftCardPassEntity)).value.void
      }

  def sendGiftCardPassReceiptMsg(
      giftCardPass: entities.GiftCardPass,
    )(implicit
      user: UserContext,
    ): Future[Unit] =
    (for {
      email <- OptionT.fromOption[Future](giftCardPass.recipientEmail)
      orderItem <- OptionT(orderItemService.findByIds(Seq(giftCardPass.orderItemId)).map(_.headOption))
      order <- OptionT(orderService.findById(orderItem.orderId, orderService.defaultFilters)(OrderExpansions.empty))
      merchant <- OptionT(merchantService.findById(user.merchantId)(MerchantExpansions.none))
      locationId <- OptionT.fromOption[Future](order.location.map(_.id))
      locationReceipt <- OptionT(locationReceiptService.findByLocationId(locationId))
      barcodeMetadata = BarcodeMetadata.forGiftCardPassReceipt(giftCardPass)
      barcodeUrl <- OptionT.liftF(barcodeService.generate(barcodeMetadata)(user.toMerchantContext))
      locationSettings <- OptionT(locationSettingsService.findAllByLocationIds(Seq(locationId)).map(_.headOption))
    } yield messageHandler.sendGiftCardPassReceiptMsg(
      giftCardPass,
      email,
      merchant,
      locationReceipt,
      barcodeUrl,
      order.location,
      locationSettings,
    )).value.void

  def computeSalesSummary(
      filters: GiftCardPassSalesSummaryFilters,
    )(implicit
      user: UserContext,
    ): Future[GiftCardPassSalesSummary] =
    dao.computeGiftCardPassSalesSummary(filters).map {
      case (purchased, used, unused) =>
        GiftCardPassSalesSummary(
          purchased = toGiftCardPassSalesReport(purchased),
          used = toGiftCardPassSalesReport(used),
          unused = toGiftCardPassSalesReport(unused),
        )
    }
}

object GiftCardPassService {
  import io.paytouch.GiftCardPass.OnlineCode

  type GenerateOnlineCode = Function0[OnlineCode]

  object generateOnlineCode extends GenerateOnlineCode {
    override def apply(): OnlineCode =
      (0 to 15)
        .map(_ => Entropy.secureRandom)
        .mkString
        .pipe(OnlineCode)

    private val Entropy: IndexedSeq[Char] =
      ('0' to '9') ++ ('A' to 'Z')
  }

}

final case class GiftCardPassCharge(
    giftCardPassId: io.paytouch.GiftCardPass.Id,
    amount: BigDecimal,
  )

object GiftCardPassCharge {
  final case class Failure(
      giftCardPassId: io.paytouch.GiftCardPass.Id,
      requestedAmount: BigDecimal,
      actualBalance: BigDecimal,
    ) extends ExposedEntity {
    override def classShortName: ExposedName =
      ExposedName.GiftCardPassChargeFailure

    override val productPrefix: String =
      "GiftCardPassCharge.Failure"
  }
}
