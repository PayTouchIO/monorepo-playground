package io.paytouch.core.services.urbanairship

import scala.concurrent._

import akka.http.scaladsl.model._

import io.paytouch.core.clients.urbanairship._
import io.paytouch.core.clients.urbanairship.entities._
import io.paytouch.core.entities.enums.PassType
import io.paytouch.core.entities.MonetaryAmount._

class UrbanAirshipUpsertTemplateSpec extends UrbanAirshipServiceSpec {
  abstract class UrbanAirshipUpsertTemplateSpecContext[A <: TemplateData] extends UrbanAirshipServiceSpecContext {
    protected def jsonExpectation(passType: PassType): String

    final def expectedCreationJson(passType: PassType) =
      loadJsonAs[JValue](jsonExpectation(passType))

    def data: A
    def urbanAirshipProjectId: String
    implicit def configLoader: ConfigLoader[A]
    implicit def templateLoader: TemplateLoader[A]

    def setupUpdateTemplateFailure() =
      walletClientMock.updateTemplateWithExternalId(any, any) returns Future.successful(
        Left(
          Error(
            uri = Uri.Empty,
            requestBody = "some-request-body-here",
            status = StatusCodes.NotFound,
            responseBody = "some-response-body-here",
            ex = None,
          ),
        ),
      )

    def setupUpdateTemplateSuccess() =
      walletClientMock
        .updateTemplateWithExternalId(any, any)
        .returns(Future.successful(Right(TemplateUpserted("foo"))))

    def setupCreateTemplateSuccess() =
      walletClientMock
        .createTemplateWithProjectIdAndExternalId(any, any, any)
        .returns(Future.successful(Right(TemplateUpserted("foo"))))

    def setupUpdateTemplateCallExpectation(passType: PassType, expectedJson: JValue) =
      there was one(walletClientMock)
        .updateTemplateWithExternalId(
          s"${passType.entryName}-$randomUuid",
          expectedJson,
        )

    def setupCreateTemplateCallExpectation(passType: PassType, expectedJson: JValue) =
      there was one(walletClientMock)
        .createTemplateWithProjectIdAndExternalId(
          urbanAirshipProjectId,
          s"${passType.entryName}-$randomUuid",
          expectedJson,
        )

    def setupCreateTemplateNoCallExpectation(passType: PassType, expectedJson: JValue) =
      there was no(walletClientMock)
        .createTemplateWithProjectIdAndExternalId(
          urbanAirshipProjectId,
          s"${passType.entryName}-$randomUuid",
          expectedJson,
        )

    def assertCreateTemplate(passType: PassType) = {
      setupUpdateTemplateFailure()
      setupCreateTemplateSuccess()

      service.upsertTemplate[A](randomUuid, passType, data).await

      setupUpdateTemplateCallExpectation(passType, expectedCreationJson(passType))
      setupCreateTemplateCallExpectation(passType, expectedCreationJson(passType))
    }

    def assertUpdateTemplate(passType: PassType) = {
      setupUpdateTemplateSuccess()

      service.upsertTemplate(randomUuid, passType, data).await

      setupUpdateTemplateCallExpectation(passType, expectedCreationJson(passType))
      setupCreateTemplateNoCallExpectation(passType, expectedCreationJson(passType))
    }
  }

  trait LoyaltyTemplateDataFixtures extends UrbanAirshipUpsertTemplateSpecContext[TemplateData.LoyaltyTemplateData] {
    val urbanAirshipProjectId = "12345"
    val data = TemplateData.LoyaltyTemplateData(
      merchantName = "Willow Cafe",
      iconImage = None,
      logoImage = None,
      address = Some("test / bar"),
      details = Some("fooDetails"),
      phone = Some("+1 (604) 555-1212"),
      website = "https://foo.bar",
    )

    override protected def jsonExpectation(passType: PassType) =
      s"/urbanairship/spec/expectation_loyalty_${passType.entryName}.json"

    implicit def configLoader = ConfigLoader.loyaltyConfigLoader
    implicit def templateLoader = TemplateLoader.loyaltyTemplateLoader
  }

  trait GiftCardTemplateDataFixtures extends UrbanAirshipUpsertTemplateSpecContext[TemplateData.GiftCardTemplateData] {
    val urbanAirshipProjectId = "6789"
    val data = TemplateData.GiftCardTemplateData(
      merchantName = "Willow Cafe",
      originalBalance = 25.$$$,
      currentBalance = 19.78.$$$,
      lastSpend = Some(2.49.$$$),
      address = Some("Address"),
      details = Some("Some information about how the gift card works. Additional terms and support information."),
      phone = Some("+1 (604) 555-1212"),
      website = "http://www.willowcafe.ca",
      androidImage = None,
      appleFullWidthImage = None,
      logoImage = None,
    )

    override protected def jsonExpectation(passType: PassType) =
      s"/urbanairship/spec/expectation_gift_card_${passType.entryName}.json"

    implicit def configLoader = ConfigLoader.giftCardConfigLoader
    implicit def templateLoader = TemplateLoader.giftCardTemplateLoader
  }

  "UrbanAirshipService" in {
    "upsertTemplate with LoyaltyTemplateData" should {
      "with an Android pass type" should {
        "call WalletClient.updateTemplate if template exist" in new LoyaltyTemplateDataFixtures {
          assertUpdateTemplate(PassType.Android)
        }

        "call WalletClient.createTemplate if template doesn't exist" in new LoyaltyTemplateDataFixtures {
          assertCreateTemplate(PassType.Android)
        }
      }

      "with an Ios pass type" should {
        "call WalletClient.updateTemplate if template exist" in new LoyaltyTemplateDataFixtures {
          assertUpdateTemplate(PassType.Ios)
        }
        "call WalletClient.createTemplate if template doesn't exist" in new LoyaltyTemplateDataFixtures {
          assertCreateTemplate(PassType.Ios)
        }
      }
    }

    "upsertTemplate with GiftCardTemplateData" should {
      "with an Android pass type" should {
        "call WalletClient.updateTemplate if template exist" in new GiftCardTemplateDataFixtures {
          assertUpdateTemplate(PassType.Android)
        }
        "call WalletClient.createTemplate if template doesn't exist" in new GiftCardTemplateDataFixtures {
          assertCreateTemplate(PassType.Android)
        }
      }

      "with an Ios pass type" should {
        "call WalletClient.updateTemplate if template exist" in new GiftCardTemplateDataFixtures {
          assertUpdateTemplate(PassType.Ios)
        }
        "call WalletClient.createTemplate if template doesn't exist" in new GiftCardTemplateDataFixtures {
          assertCreateTemplate(PassType.Ios)
        }
      }
    }
  }
}
