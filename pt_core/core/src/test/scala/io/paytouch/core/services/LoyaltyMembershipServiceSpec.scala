package io.paytouch.core.services

import io.paytouch._

import java.util.UUID

import scala.concurrent._

import com.softwaremill.macwire._

import io.paytouch.core.async.sqs._
import io.paytouch.core.barcodes.entities.BarcodeMetadata
import io.paytouch.core.barcodes.services.BarcodeService
import io.paytouch.core.clients.urbanairship.entities.Pass
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums._
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.expansions._
import io.paytouch.core.messages.entities._
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }
import io.paytouch.utils.Tagging._

class LoyaltyMembershipServiceSpec extends ServiceDaoSpec {
  val loyaltyMembershipDao = daos.loyaltyMembershipDao

  abstract class LoyaltyMembershipServiceSpecContext extends ServiceDaoSpecContext {
    val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])
    val barcodeServiceMock = mock[BarcodeService]
    val urbanAirshipServiceMock = mock[UrbanAirshipService]
    val instance = wire[LoyaltyMembershipService]
    val globalCustomer = Factory.globalCustomerWithEmail(merchant = Some(merchant), email = Some(randomEmail)).create
    val order = Factory
      .order(
        merchant,
        globalCustomer = Some(globalCustomer),
        paymentStatus = Some(PaymentStatus.Paid),
        totalAmount = Some(40),
        location = Some(rome),
      )
      .create
    val product = Factory.simpleProduct(merchant).create
    Factory
      .orderItem(order, product = Some(product), totalPriceAmount = Some(40), paymentStatus = Some(PaymentStatus.Paid))
      .create
    Factory
      .paymentTransaction(
        order,
        paymentType = Some(TransactionPaymentType.Cash),
        paymentDetails = Some(PaymentDetails(amount = Some(40))),
        `type` = Some(TransactionType.Payment),
      )
      .create
    val orderEntity = orderService
      .enrich(order, orderService.defaultFilters)(OrderExpansions.withOrderItems(withPaymentTransactions = true))
      .await

    val orderPointsData = OrderPointsData.fromOrderEntity(orderEntity).get

    val barcodeUrl = randomWord
    barcodeServiceMock.generate(any)(any) returns Future.successful(barcodeUrl)

    def assertBalanceUpdated(loyaltyMembership: LoyaltyMembershipRecord, expectedPoints: Int) =
      loyaltyMembershipDao.findById(loyaltyMembership.id).await.get.points ==== expectedPoints

    def assertCustomerIsEnrolled(customerId: UUID, loyaltyProgram: LoyaltyProgramRecord) = {
      val maybeMembership =
        loyaltyMembershipDao.findByCustomerIdAndLoyaltyProgramId(merchant.id, customerId, loyaltyProgram.id).await
      maybeMembership must beSome
      (maybeMembership.get.merchantOptInAt must beSome) or (maybeMembership.get.customerOptInAt must beSome)
    }

    def assertWelcomeEmailIsSent(
        loyaltyMembership: LoyaltyMembershipRecord,
        loyaltyProgram: LoyaltyProgramRecord,
        createMessage: (LoyaltyMembership, LoyaltyProgram) => SQSMessage[_],
      ) = {
      val loyaltyMembershipEntity =
        loyaltyMembershipService
          .enrich(loyaltyMembership)
          .await

      val loyaltyProgramEntity =
        loyaltyProgramService
          .enrich(loyaltyProgram, loyaltyProgramService.defaultFilters)(LoyaltyProgramExpansions(withLocations = false))
          .await

      actorMock.expectMsg(SendMsgWithRetry(createMessage(loyaltyMembershipEntity, loyaltyProgramEntity)))

      ok
    }

    def assertWelcomeEmailNotSent() = {
      actorMock.expectNoMessage()
      ok
    }
  }

  trait Fixtures { self: LoyaltyMembershipServiceSpecContext =>
    val loyaltyProgram =
      Factory
        .loyaltyProgram(
          merchant,
          points = Some(200),
          pointsToReward = Some(150),
          `type` = Some(LoyaltyProgramType.Frequency),
          locations = Seq(rome),
          minimumPurchaseAmount = Some(10),
          spendAmountForPoints = Some(5),
          appleWalletTemplateId = Some("fooAppleTemplateId"),
          androidPayTemplateId = Some("fooAndroidTemplateId"),
        )
        .create

    val loyaltyMembership =
      Factory
        .loyaltyMembership(globalCustomer, loyaltyProgram, merchantOptInAt = Some(UtcTime.now))
        .create
  }

  trait UpsertPassFixtures { self: LoyaltyMembershipServiceSpecContext =>
    val passCreatedAndroid = random[Pass]
    val passCreatedIos = random[Pass]
    urbanAirshipServiceMock.upsertPass(any, beTypedEqualTo(PassType.Ios), any)(any) returns Future.successful(
      passCreatedIos,
    )
    urbanAirshipServiceMock.upsertPass(any, beTypedEqualTo(PassType.Android), any)(any) returns Future
      .successful(passCreatedAndroid)

    def assertPassesCreatedAndSaved(loyaltyMembershipId: UUID) = {
      val updatedLoyaltyMembership = daos.loyaltyMembershipDao.findById(loyaltyMembershipId).await.get
      updatedLoyaltyMembership.androidPassPublicUrl ==== Some(passCreatedAndroid.publicUrl.path)
      updatedLoyaltyMembership.iosPassPublicUrl ==== Some(passCreatedIos.publicUrl.path)

      there was one(urbanAirshipServiceMock).upsertPass(
        beTypedEqualTo("fooAppleTemplateId"),
        beTypedEqualTo(PassType.Ios),
        any,
      )(any)
      there was one(urbanAirshipServiceMock).upsertPass(
        beTypedEqualTo("fooAndroidTemplateId"),
        beTypedEqualTo(PassType.Android),
        any,
      )(any)
    }
  }

  "LoyaltyMembershipsService" in {
    "sendWelcomeEmail" in new LoyaltyMembershipServiceSpecContext with Fixtures {
      instance.sendWelcomeEmail(loyaltyMembership.id).await

      assertWelcomeEmailIsSent(
        loyaltyMembership,
        loyaltyProgram,
        LoyaltyMembershipSignedUp(
          globalCustomer.email.get,
          _,
          merchantService
            .enrich(merchant, merchantService.defaultFilters)(MerchantExpansions.none)
            .await,
          _,
          barcodeUrl,
        ),
      )
    }

    "if customer not enrolled in membership" should {
      "notSendWelcomeEmail" in new LoyaltyMembershipServiceSpecContext with Fixtures {
        val anotherCustomer = Factory.globalCustomerWithEmail(merchant = Some(merchant), email = None).create

        val anotherLoyaltyMembership =
          Factory
            .loyaltyMembership(anotherCustomer, loyaltyProgram, merchantOptInAt = None, customerOptInAt = None)
            .create

        instance.sendWelcomeEmail(anotherLoyaltyMembership.id).await

        assertWelcomeEmailNotSent()
      }
    }

    "logOrderPoints" in new LoyaltyMembershipServiceSpecContext with Fixtures {
      val expectedLoyaltyMembership =
        instance.enrich(loyaltyMembership).await.copy(points = 200, pointsToNextReward = 0)

      instance.logOrderPoints(orderPointsData, loyaltyProgram).await

      actorMock.expectMsg(SendMsgWithRetry(LoyaltyMembershipChanged(expectedLoyaltyMembership)))
    }

    "fromRecordToEntity" in {
      "if loyalty membership points are greater than points" should {
        "have pointsToNextReward set to 0" in new LoyaltyMembershipServiceSpecContext {
          val loyaltyProgram = Factory.loyaltyProgram(merchant, pointsToReward = Some(150)).create
          val loyaltyMembership =
            Factory
              .loyaltyMembership(
                globalCustomer,
                loyaltyProgram,
                merchantOptInAt = Some(UtcTime.now),
                points = Some(320),
              )
              .create
          val loyaltyProgramEntity = loyaltyProgramService
            .enrich(loyaltyProgram, loyaltyProgramService.defaultFilters)(
              LoyaltyProgramExpansions(withLocations = false),
            )
            .await

          val entity = instance.fromRecordToEntity(
            loyaltyMembership,
            CustomerTotals(globalCustomer.id, 0.$$$, 0),
            loyaltyProgramEntity,
          )
          entity.pointsToNextReward ==== 0
        }
      }
    }

    "findOrCreate" in {
      "if globalCustomer is enrolled in loyalty program" should {
        "return her loyalty status without interacting with UA" in new LoyaltyMembershipServiceSpecContext
          with Fixtures {
          val expectedLoyaltyMembership = instance.enrich(loyaltyMembership).await
          val result = instance.findOrCreateInActiveProgram(globalCustomer.id, Some(rome.id)).await.get

          result ==== expectedLoyaltyMembership

          there was no(urbanAirshipServiceMock).upsertPass(any, any, any)(any)
        }
      }

      "if globalCustomer is NOT enrolled in loyalty program" should {
        "create loyalty status and return loyalty status with UA pass" in new LoyaltyMembershipServiceSpecContext
          with Fixtures
          with UpsertPassFixtures {
          val anotherCustomer = Factory.globalCustomerWithEmail(merchant = Some(merchant), email = None).create

          val result = instance.findOrCreateInActiveProgram(anotherCustomer.id, Some(rome.id)).await.get

          val expectedAndroidUrl = passService.generateUrl(result.id, PassType.Android, PassItemType.LoyaltyMembership)
          val expectedIosUrl = passService.generateUrl(result.id, PassType.Ios, PassItemType.LoyaltyMembership)
          val expectedLoyaltyMembership = LoyaltyMembership(
            id = result.id,
            customerId = anotherCustomer.id,
            loyaltyProgramId = loyaltyProgram.id,
            lookupId = result.lookupId,
            points = 0,
            pointsToNextReward = 150,
            passPublicUrls = PassUrls(
              android = Some(expectedAndroidUrl),
              ios = Some(expectedIosUrl),
            ),
            customerOptInAt = None,
            merchantOptInAt = None,
            enrolled = false,
            visits = 0,
            totalSpend = 0.$$$,
          )

          result ==== expectedLoyaltyMembership

          assertPassesCreatedAndSaved(result.id)
        }
      }
    }

    "enrolViaMerchant" in {
      "if loyalty program rewards signups" should {
        "assign SignupBonus and send welcome email to loyalty membership" in new LoyaltyMembershipServiceSpecContext
          with UpsertPassFixtures {
          val loyaltyProgram =
            Factory
              .loyaltyProgram(
                merchant,
                signupRewardEnabled = Some(true),
                signupRewardPoints = Some(100),
                appleWalletTemplateId = Some("fooAppleTemplateId"),
                androidPayTemplateId = Some("fooAndroidTemplateId"),
              )
              .create

          val loyaltyMembership = Factory.loyaltyMembership(globalCustomer, loyaltyProgram, points = Some(0)).create

          val updatedLoyaltyMembership = instance.enrolViaMerchant(globalCustomer.id, Some(loyaltyProgram)).await.get

          eventually {
            assertCustomerIsEnrolled(globalCustomer.id, loyaltyProgram)
            assertBalanceUpdated(loyaltyMembership, expectedPoints = 100)
          }

          assertWelcomeEmailIsSent(updatedLoyaltyMembership, loyaltyProgram, PrepareLoyaltyMembershipSignedUp.apply)
        }

        "don't assign SignupBonus and don't send welcome email if already enrolled" in new LoyaltyMembershipServiceSpecContext {
          val loyaltyProgram =
            Factory.loyaltyProgram(merchant, signupRewardEnabled = Some(true), signupRewardPoints = Some(100)).create

          val loyaltyMembership = Factory
            .loyaltyMembership(globalCustomer, loyaltyProgram, points = Some(100), merchantOptInAt = Some(UtcTime.now))
            .create
          Factory
            .loyaltyPointsHistory(loyaltyMembership, points = 100, `type` = LoyaltyPointsHistoryType.SignUpBonus)
            .create

          val updatedLoyaltyMembership = instance.enrolViaMerchant(globalCustomer.id, Some(loyaltyProgram)).await.get

          eventually {
            assertCustomerIsEnrolled(globalCustomer.id, loyaltyProgram)
            assertBalanceUpdated(loyaltyMembership, expectedPoints = 100)
          }

          actorMock.expectNoMessage()
        }
      }

      "if loyalty program signupRewardEnabled = true and signupRewardPoints = null" should {
        "don't assign SignupBonus to loyalty membership and send welcome email" in new LoyaltyMembershipServiceSpecContext
          with UpsertPassFixtures {
          val loyaltyProgram =
            Factory
              .loyaltyProgram(
                merchant,
                signupRewardEnabled = Some(true),
                signupRewardPoints = None,
                appleWalletTemplateId = Some("fooAppleTemplateId"),
                androidPayTemplateId = Some("fooAndroidTemplateId"),
              )
              .create

          val loyaltyMembership = Factory.loyaltyMembership(globalCustomer, loyaltyProgram, points = Some(0)).create

          val updatedLoyaltyMembership = instance.enrolViaMerchant(globalCustomer.id, Some(loyaltyProgram)).await.get

          eventually {
            assertCustomerIsEnrolled(globalCustomer.id, loyaltyProgram)
            assertBalanceUpdated(loyaltyMembership, expectedPoints = 0)
          }

          assertWelcomeEmailIsSent(updatedLoyaltyMembership, loyaltyProgram, PrepareLoyaltyMembershipSignedUp.apply)
        }
      }

      "if loyalty program doesn't rewards signups" should {
        "don't assign SignupBonus to loyalty membership and send welcome email" in new LoyaltyMembershipServiceSpecContext
          with UpsertPassFixtures {
          val loyaltyProgram =
            Factory
              .loyaltyProgram(
                merchant,
                signupRewardEnabled = Some(false),
                signupRewardPoints = Some(100),
                appleWalletTemplateId = Some("fooAppleTemplateId"),
                androidPayTemplateId = Some("fooAndroidTemplateId"),
              )
              .create

          val loyaltyMembership = Factory.loyaltyMembership(globalCustomer, loyaltyProgram, points = Some(0)).create

          val updatedLoyaltyMembership = instance.enrolViaMerchant(globalCustomer.id, Some(loyaltyProgram)).await.get

          eventually {
            assertCustomerIsEnrolled(globalCustomer.id, loyaltyProgram)
            assertBalanceUpdated(loyaltyMembership, expectedPoints = 0)
          }

          assertWelcomeEmailIsSent(updatedLoyaltyMembership, loyaltyProgram, PrepareLoyaltyMembershipSignedUp.apply)

        }
      }
    }
    "enrolViaCustomer" in {
      "if customer is already enrolled" should {
        "not assign SignupBonus to loyalty membership nor send welcome email nor assign order points" in new LoyaltyMembershipServiceSpecContext
          with UpsertPassFixtures {
          val loyaltyProgram =
            Factory.loyaltyProgram(merchant, signupRewardEnabled = Some(true), signupRewardPoints = Some(100)).create
          val loyaltyMembership = Factory
            .loyaltyMembership(globalCustomer, loyaltyProgram, points = Some(0), merchantOptInAt = Some(UtcTime.now))
            .create

          val updatedLoyaltyMembership =
            instance.enrolViaCustomer(loyaltyMembership.id, orderId = Some(order.id)).await.get

          assertBalanceUpdated(loyaltyMembership, expectedPoints = 0)
          assertWelcomeEmailNotSent()
        }
      }
      "if customer is not enrolled" should {
        "if loyalty program rewards signups" should {
          "assign SignupBonus to loyalty membership and send welcome email and assign order points" in new LoyaltyMembershipServiceSpecContext
            with UpsertPassFixtures {
            val loyaltyProgram =
              Factory
                .loyaltyProgram(
                  merchant,
                  locations = Seq(rome),
                  signupRewardEnabled = Some(true),
                  signupRewardPoints = Some(100),
                  `type` = Some(LoyaltyProgramType.Frequency),
                  points = Some(33),
                  minimumPurchaseAmount = Some(10),
                )
                .create
            val loyaltyMembership = Factory.loyaltyMembership(globalCustomer, loyaltyProgram, points = Some(0)).create

            val updatedLoyaltyMembership =
              instance.enrolViaCustomer(loyaltyMembership.id, orderId = Some(order.id)).await.get

            eventually {
              assertCustomerIsEnrolled(globalCustomer.id, loyaltyProgram)
              assertBalanceUpdated(loyaltyMembership, expectedPoints = 133)
            }

            assertWelcomeEmailIsSent(updatedLoyaltyMembership, loyaltyProgram, PrepareLoyaltyMembershipSignedUp.apply)
          }
        }
      }
      "if loyalty program doesn't rewards signups" should {
        "not assign SignupBonus to loyalty membership but send welcome email and assign order points" in new LoyaltyMembershipServiceSpecContext {
          val loyaltyProgram =
            Factory
              .loyaltyProgram(
                merchant,
                signupRewardEnabled = Some(false),
                signupRewardPoints = Some(100),
                `type` = Some(LoyaltyProgramType.Frequency),
                points = Some(33),
                minimumPurchaseAmount = Some(10),
              )
              .create

          val loyaltyMembership = Factory.loyaltyMembership(globalCustomer, loyaltyProgram, points = Some(0)).create

          val updatedLoyaltyMembership =
            instance.enrolViaCustomer(loyaltyMembership.id, orderId = Some(order.id)).await.get

          assertCustomerIsEnrolled(globalCustomer.id, loyaltyProgram)
          assertBalanceUpdated(loyaltyMembership, expectedPoints = 33)

          assertWelcomeEmailIsSent(updatedLoyaltyMembership, loyaltyProgram, PrepareLoyaltyMembershipSignedUp.apply)
        }
      }
    }
    "updateLinksWithOrderId" should {
      "add order_id parameter to urls" in new LoyaltyMembershipServiceSpecContext {
        val loyaltyMembershipId = UUID.fromString("50f8e6c7-cd31-4233-b712-fca0847f17e8")
        val orderId = UUID.fromString("3a437ce1-894c-40ad-8ae9-b4182b599c87")
        val passUrls = PassUrls(ios = Some("ios"), android = Some("android"))
        val loyaltyMembership = random[LoyaltyMembership].copy(id = loyaltyMembershipId, passPublicUrls = passUrls)

        val result = instance.updateLinksWithOrderId(Some(loyaltyMembership), orderId).get
        result.passPublicUrls.ios ==== Some(
          "http://localhost:7000/v1/public/passes.install?id=50f8e6c7-cd31-4233-b712-fca0847f17e8&item_type=loyalty_membership&order_id=3a437ce1-894c-40ad-8ae9-b4182b599c87&token=7995fa39deaefda03b8de36de416873e82a169d7&type=ios",
        )
        result.passPublicUrls.android ==== Some(
          "http://localhost:7000/v1/public/passes.install?id=50f8e6c7-cd31-4233-b712-fca0847f17e8&item_type=loyalty_membership&order_id=3a437ce1-894c-40ad-8ae9-b4182b599c87&token=853cae440f346788ad8476f52daed7e6bd7b223b&type=android",
        )
      }
    }
  }
}
