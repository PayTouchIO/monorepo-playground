package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import com.softwaremill.macwire._

import org.specs2.concurrent.ExecutionEnv

import io.paytouch.implicits._

import io.paytouch.core.async.sqs._
import io.paytouch.core.barcodes.services.BarcodeService
import io.paytouch.core.data.daos._
import io.paytouch.core.data.daos.features._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums._
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.expansions._
import io.paytouch.core.messages.entities._
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.core.validators._
import io.paytouch.core.validators.RecoveredOrderItemUpsertion
import io.paytouch.utils.Tagging._

@scala.annotation.nowarn("msg=Auto-application")
class GiftCardPassServiceSpec extends ServiceDaoSpec {
  import GiftCardPassService._

  final override implicit lazy val daos: Daos = new Daos {
    // the first online code generated will be 1 and this mock will say that it already exists
    // and thus force the online code generator which is used in one of the tests below
    // to generate another code which will be 2
    override lazy val giftCardPassDao = new GiftCardPassDao(orderItemDao, giftCardPassTransactionDao) {
      import io.paytouch.GiftCardPass.OnlineCode

      override def doesOnlineCodeExist(onlineCode: OnlineCode): Future[Boolean] =
        (onlineCode.value.toInt === 1).pure[Future]
    }
  }

  abstract class GiftCardPassServiceSpecContext extends ServiceDaoSpecContext {
    val giftCardPassDao = daos.giftCardPassDao
    val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])

    val urbanAirshipServiceMock = mock[UrbanAirshipService]
    val barcodeServiceMock = mock[BarcodeService]
    val merchantServiceMock = mock[MerchantService]
    val locationReceiptServiceMock = mock[LocationReceiptService]
    val locationSettingsServiceMock = mock[LocationSettingsService]

    val service = wire[GiftCardPassService]

    lazy val acceptanceStatus: AcceptanceStatus = AcceptanceStatus.Open
    val order = Factory
      .order(
        merchant,
        location = Some(london),
        // forces order to be Open as this orders are not returned by default by OrderService and we want to include them
        onlineOrderAttribute = Factory.onlineOrderAttribute(merchant, acceptanceStatus.some).create.some,
      )
      .create
    val orderItem = Factory.orderItem(order).create
    val giftCardProduct = Factory.giftCardProduct(merchant).create

    val giftCard =
      Factory
        .giftCard(
          giftCardProduct,
          appleWalletTemplateId = Some("123"),
          androidPayTemplateId = Some("456"),
        )
        .create

    def createGiftCardPass(
        currentBalance: BigDecimal,
        giftCard: GiftCardRecord,
        orderItem: OrderItemRecord,
      ): GiftCardPassRecord =
      Factory
        .giftCardPass(
          giftCard,
          orderItem,
          originalAmount = 25.somew,
          balanceAmount = currentBalance.some,
          androidPassPublicUrl = "foo".some,
          iosPassPublicUrl = "bar".some,
          recipientEmail = None,
        )
        .createForceOverride(
          _.copy(
            id = UUID.randomUUID().some,
            onlineCode = generateOnlineCode().some,
          ),
        )

    val giftCardPass =
      createGiftCardPass(currentBalance = 12, giftCard = giftCard, orderItem = orderItem)

    def find(giftCardPassRecord: GiftCardPassRecord) =
      service
        .findById(giftCardPassRecord.id)(GiftCardPassExpansions(withTransactions = true))
        .await
        .get
  }

  "GiftCardPassService" in {
    "decreaseBalance for a single pass" should {
      "update gift card pass and call monitor" in new GiftCardPassServiceSpecContext {
        service
          .decreaseBalance(giftCardPass.id, 10)
          .await
          .success
          .get

        val giftCardPassEntity = find(giftCardPass)
        actorMock.expectMsg(SendMsgWithRetry(GiftCardPassChanged(giftCardPassEntity)(userContext)))
      }
    }

    "decreaseBalance for multiple passes" should {
      "update gift card pass and call monitor for a single pass" in new GiftCardPassServiceSpecContext {
        import io.paytouch._

        service
          .decreaseBalance(
            orderId = OrderIdPostgres(order.id).cast,
            bulkCharge = Seq(
              GiftCardPassCharge(
                GiftCardPass.IdPostgres(giftCardPass.id).cast,
                10,
              ),
            ),
          )
          .await
          .success

        val giftCardPassEntity = find(giftCardPass)
        actorMock.expectMsg(SendMsgWithRetry(GiftCardPassChanged(giftCardPassEntity)(userContext)))
      }

      "update all gift card passes and call monitor for multiple passes" in new GiftCardPassServiceSpecContext {
        import io.paytouch._

        val giftCardPass2 =
          createGiftCardPass(
            currentBalance = 22,
            giftCard = Factory
              .giftCard(
                Factory.giftCardProduct(merchant).create,
                appleWalletTemplateId = Some("123"),
                androidPayTemplateId = Some("456"),
              )
              .createForceOverride(_.copy(id = UUID.randomUUID().some)),
            orderItem = Factory
              .orderItem(order)
              .createForceOverride(_.copy(id = UUID.randomUUID().some)),
          )

        val giftCardPassNotUsedInThisCallButPresentInDb =
          createGiftCardPass(
            currentBalance = 36,
            giftCard = Factory
              .giftCard(
                Factory.giftCardProduct(merchant).create,
                appleWalletTemplateId = Some("123"),
                androidPayTemplateId = Some("456"),
              )
              .createForceOverride(_.copy(id = UUID.randomUUID().some)),
            orderItem = Factory
              .orderItem(order)
              .createForceOverride(_.copy(id = UUID.randomUUID().some)),
          )

        val refreshedOrder =
          service
            .decreaseBalance(
              orderId = OrderIdPostgres(order.id).cast,
              bulkCharge = Seq(
                GiftCardPassCharge(
                  GiftCardPass.IdPostgres(giftCardPass.id).cast,
                  10,
                ),
                GiftCardPassCharge(
                  GiftCardPass.IdPostgres(giftCardPass2.id).cast,
                  20,
                ),
              ),
            )
            .await
            .success
            .get

        val giftCardPassEntity = find(giftCardPass)
        giftCardPassEntity.balance ==== (12 - 10).USD

        val giftCardPassEntity2 = find(giftCardPass2)
        giftCardPassEntity2.balance ==== (22 - 20).USD

        actorMock.expectMsgAllOf(
          SendMsgWithRetry(GiftCardPassChanged(giftCardPassEntity)(userContext)),
          SendMsgWithRetry(GiftCardPassChanged(giftCardPassEntity2)(userContext)),
        )

        val transactions =
          refreshedOrder.paymentTransactions.get

        transactions.size ==== 2

        transactions
          .find(_.paymentDetails.exists(_.giftCardPassId.contains(giftCardPassEntity.id)))
          .map { transaction =>
            transaction.paymentProcessorV2 ==== TransactionPaymentProcessor.Paytouch
            transaction.paymentDetails.map { pd =>
              pd.amount.contains(10) ==== true
              pd.currency ==== merchant.currency.some
            }
          }

        transactions
          .find(_.paymentDetails.exists(_.giftCardPassId.contains(giftCardPassEntity2.id)))
          .map { transaction =>
            transaction.paymentProcessorV2 ==== TransactionPaymentProcessor.Paytouch
            transaction.paymentDetails.map { pd =>
              pd.amount.contains(20) ==== true
              pd.currency ==== merchant.currency.some
            }
          }
      }

      "NOT update gift card passes and therefore NOT call monitor for multiple passes as soon as one of the cards has insufficient funds" in new GiftCardPassServiceSpecContext {
        import io.paytouch._

        val giftCardPass2 =
          createGiftCardPass(
            currentBalance = 22,
            giftCard = Factory
              .giftCard(
                Factory.giftCardProduct(merchant).create,
                appleWalletTemplateId = Some("123"),
                androidPayTemplateId = Some("456"),
              )
              .createForceOverride(_.copy(id = UUID.randomUUID().some)),
            orderItem = Factory
              .orderItem(order)
              .createForceOverride(_.copy(id = UUID.randomUUID().some)),
          )

        service
          .decreaseBalance(
            orderId = OrderIdPostgres(order.id).cast,
            bulkCharge = Seq(
              GiftCardPassCharge(
                GiftCardPass.IdPostgres(giftCardPass.id).cast,
                10,
              ),
              GiftCardPassCharge(
                GiftCardPass.IdPostgres(giftCardPass2.id).cast,
                23, // 1 over balance
              ),
            ),
          )
          .await
          .failures
          .head
          .tap(_.code ==== "InsufficientFunds")
          .tap(
            _.values.head.asInstanceOf[GiftCardPassCharge.Failure] ==== GiftCardPassCharge
              .Failure(
                giftCardPassId = GiftCardPass.IdPostgres(giftCardPass2.id).cast,
                requestedAmount = 23,
                actualBalance = 22,
              ),
          )
          .tap(
            _.values.head.toString ==== s"GiftCardPassCharge.Failure(GiftCardPass.Id(${giftCardPass2.id}),23,22.00)",
          )

        val giftCardPassEntity = find(giftCardPass)
        giftCardPassEntity.balance ==== giftCardPass.balanceAmount.USD

        val giftCardPassEntity2 = find(giftCardPass2)
        giftCardPassEntity2.balance ==== giftCardPass2.balanceAmount.USD

        actorMock.expectNoMessage()
      }

      "NOT update gift card passes and therefore NOT call monitor for multiple passes as soon as one of the cards is not found" in new GiftCardPassServiceSpecContext {
        import io.paytouch._

        service
          .decreaseBalance(
            orderId = OrderIdPostgres(order.id).cast,
            bulkCharge = Seq(
              GiftCardPassCharge(
                GiftCardPass.IdPostgres(giftCardPass.id).cast,
                10,
              ),
              GiftCardPassCharge(
                GiftCardPass.IdPostgres(UUID.randomUUID()).cast,
                20,
              ),
            ),
          )
          .await
          .failures
          .head
          .code ==== "GiftCardPassesNotAllFound"

        actorMock.expectNoMessage()
      }
    }

    "upsertPass" should {
      "fail the future if urbanairship can't upsert the pass" in new GiftCardPassServiceSpecContext {
        implicit val ee = ExecutionEnv.fromGlobalExecutionContext

        urbanAirshipServiceMock
          .upsertPass[GiftCardPass](any, any, any)(any) returns Future.failed(new RuntimeException("Error"))

        val giftCardPassEntity = find(giftCardPass)
        service.upsertPass(giftCardPassEntity) must throwAn[RuntimeException].await
      }
    }

    "sendReceipt" should {
      "send the correct message" in new GiftCardPassServiceSpecContext {
        val recipientData = SendReceiptData(randomEmail)

        val giftCardPassEntity = find(giftCardPass)

        service.sendReceipt(orderItem.id, recipientData).await.success

        val reloadedGiftCardPassEntity = find(giftCardPass)

        giftCardPassEntity.copy(
          recipientEmail = recipientData.recipientEmail.some,
          createdAt = reloadedGiftCardPassEntity.createdAt,
          updatedAt = reloadedGiftCardPassEntity.updatedAt,
        ) ==== reloadedGiftCardPassEntity

        actorMock.expectMsg(
          SendMsgWithRetry(
            PrepareGiftCardPassReceiptRequested(
              reloadedGiftCardPassEntity,
            ),
          ),
        )
      }
    }

    "sendGiftCardPassReceiptMsg" should {
      "send the correct message" in new GiftCardPassServiceSpecContext {
        // sendGiftCardPassReceiptMsg will not send for orders with AcceptanceStatus.Open
        override lazy val acceptanceStatus = AcceptanceStatus.Pending
        val recipientData = SendReceiptData(randomEmail)
        giftCardPassDao.updateRecipientEmail(giftCardPass.id, recipientData.recipientEmail).await

        val barcodeUrl = randomWord
        barcodeServiceMock.generate(any)(any) returns Future.successful(barcodeUrl)

        val merchantEntity = random[Merchant].copy(id = user.merchantId)
        merchantServiceMock.findById(any)(any) returns Future.successful(Some(merchantEntity))

        val orderEntity = orderService
          .enrich(order, orderService.defaultFilters)(OrderExpansions.empty)
          .await

        val locationReceiptEntity = random[LocationReceipt]
        locationReceiptServiceMock.findByLocationId(any)(any) returns Future
          .successful(Some(locationReceiptEntity))

        val locationSettingsEntity = random[LocationSettings]
        locationSettingsServiceMock.findAllByLocationIds(any)(any) returns Future
          .successful(Seq(locationSettingsEntity))

        val giftCardPassEntity = find(giftCardPass)
        service.sendGiftCardPassReceiptMsg(giftCardPassEntity).await

        val giftCardPassReloaded =
          giftCardPassDao.findById(giftCardPass.id).await.get
        val expectedIosPassUrl = Some(passService.generateUrl(giftCardPass.id, PassType.Ios, PassItemType.GiftCard))
        val expectedAndroidPassUrl =
          Some(passService.generateUrl(giftCardPass.id, PassType.Android, PassItemType.GiftCard))
        val expectedPassUrls =
          PassUrls(ios = expectedIosPassUrl, android = expectedAndroidPassUrl)

        val expectedGiftCardPassEntity =
          GiftCardPass(
            id = giftCardPassReloaded.id,
            lookupId = giftCardPassReloaded.lookupId,
            giftCardId = giftCard.id,
            orderItemId = orderItem.id,
            originalBalance = 25.$$$,
            balance = 12.$$$,
            passPublicUrls = expectedPassUrls,
            transactions = Some(Seq.empty),
            passInstalledAt = None,
            recipientEmail = Some(recipientData.recipientEmail),
            onlineCode = giftCardPass.onlineCode.hyphenated,
            createdAt = giftCardPassReloaded.createdAt,
            updatedAt = giftCardPassReloaded.updatedAt,
          )

        actorMock.expectMsg(
          SendMsgWithRetry(
            GiftCardPassReceiptRequested(
              expectedGiftCardPassEntity,
              recipientData.recipientEmail.toString,
              merchantEntity,
              locationReceiptEntity,
              barcodeUrl,
              orderEntity.location,
              locationSettingsEntity,
            ),
          ),
        )
        there was one(merchantServiceMock).findById(user.merchantId)(MerchantExpansions.none)
        there was one(locationReceiptServiceMock).findByLocationId(london.id)(userCtx)
        there was one(locationSettingsServiceMock).findAllByLocationIds(Seq(london.id))(userCtx)
      }
    }

    "convertToGiftCardPassUpdates" should {
      "generate a unique online code or die trying" in new GiftCardPassServiceSpecContext {
        import io.paytouch.GiftCardPass.OnlineCode

        override lazy val generateOnlineCode: GiftCardPassService.GenerateOnlineCode = {
          var nextOnlineCode = 1 // do not put this var inside of the lambda below

          () =>
            OnlineCode(nextOnlineCode.toString)
              .tapAs(nextOnlineCode += 1)
        }

        val upsertionWithGiftCardCreationItems: RecoveredOrderUpsertion =
          random[RecoveredOrderUpsertion]
            .copy(
              items = Seq(
                random[RecoveredOrderItemUpsertion]
                  .copy( // just making sure we pass the validator
                    withGiftCardCreation = true,
                    priceAmount = random[BigDecimal].some,
                  ),
              ),
            )

        service
          .convertToGiftCardPassUpdates(upsertionWithGiftCardCreationItems)
          .await
          .getOrElse(sys.error("fail"))
          .head
          .onlineCode
          .get ==== OnlineCode(2.toString)
      }
    }
  }
}
