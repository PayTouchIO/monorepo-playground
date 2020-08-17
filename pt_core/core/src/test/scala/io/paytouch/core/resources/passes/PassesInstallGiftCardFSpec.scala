package io.paytouch.core.resources.passes

import java.util.UUID

import akka.http.javadsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import io.paytouch.core.entities.enums.{ PassItemType, PassType }
import io.paytouch.core.utils.{ MockedRestApi, UtcTime, FixtureDaoFactory => Factory }

class PassesInstallGiftCardFSpec extends PassesFSpec {

  abstract class PassesInstallCardStatusFSpecContext extends PassResourceFSpecContext {
    val passIosPublicUrl = "foo"
    val androidPassPublicUrl = "bar"
    val order = Factory.order(merchant, location = Some(london)).create
    val orderItem = Factory.orderItem(order).create
    val giftCardProduct = Factory.giftCardProduct(merchant).create
    val giftCard = Factory.giftCard(giftCardProduct).create

    val passService = MockedRestApi.passService

    def assertClickTimeIsStored(id: UUID) = reloadGiftCardPass(id).passInstalledAt must beSome

    def reloadGiftCardPass(id: UUID) = daos.giftCardPassDao.findById(id).await.get
  }

  "GET /v1/public/passes.install?id=$&type=[android|ios]&item_type=gift_card" in {
    "if request has valid token" in {
      "if the id doesn't exist in gift card pass table" should {
        "return 404" in new PassesInstallCardStatusFSpecContext {
          val randomUuid = UUID.randomUUID
          val path = passService.generatePath(randomUuid, PassType.Ios, PassItemType.GiftCard)

          Get(path) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
      "if the id exists but pass url is not set gift card pass table" should {
        "return 404" in new PassesInstallCardStatusFSpecContext {
          val giftCardPass =
            Factory.giftCardPass(giftCard, orderItem, iosPassPublicUrl = None).create
          val path = passService.generatePath(giftCardPass.id, PassType.Ios, PassItemType.GiftCard)

          Get(path) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
      "if the id exists in gift card pass table" in {
        "if type=ios" should {
          "redirect" in new PassesInstallCardStatusFSpecContext {
            val giftCardPass = Factory
              .giftCardPass(giftCard, orderItem, iosPassPublicUrl = Some(passIosPublicUrl))
              .create
            val path = passService.generatePath(giftCardPass.id, PassType.Ios, PassItemType.GiftCard)

            Get(path) ~> routes ~> check {
              assertStatus(StatusCodes.Found)
              header[Location] ==== Some(Location(passIosPublicUrl))
              assertClickTimeIsStored(giftCardPass.id)
            }
          }
        }
        "if type=android" should {
          "redirect" in new PassesInstallCardStatusFSpecContext {
            val giftCardPass = Factory
              .giftCardPass(giftCard, orderItem, androidPassPublicUrl = Some(androidPassPublicUrl))
              .create
            val path = passService.generatePath(giftCardPass.id, PassType.Android, PassItemType.GiftCard)

            Get(path) ~> routes ~> check {
              assertStatus(StatusCodes.Found)
              header[Location] ==== Some(Location(androidPassPublicUrl))
              assertClickTimeIsStored(giftCardPass.id)
            }
          }
        }
        "if url is None" should {
          "return not found" in new PassesInstallCardStatusFSpecContext {
            val giftCardPass = Factory
              .giftCardPass(giftCard, orderItem, androidPassPublicUrl = None, iosPassPublicUrl = None)
              .create
            val path = passService.generatePath(giftCardPass.id, PassType.Android, PassItemType.GiftCard)

            Get(path) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
            }
          }
        }
        "if passInstalledAt has already a value" should {
          "redirect without updating it" in new PassesInstallCardStatusFSpecContext {
            val giftCardPass = Factory
              .giftCardPass(giftCard, orderItem, passInstalledAt = Some(UtcTime.now))
              .create
            val path = passService.generatePath(giftCardPass.id, PassType.Android, PassItemType.GiftCard)

            Get(path) ~> routes ~> check {
              reloadGiftCardPass(giftCardPass.id).passInstalledAt ==== giftCardPass.passInstalledAt
            }
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new PassesInstallCardStatusFSpecContext {
        val giftCardPass = Factory
          .giftCardPass(giftCard, orderItem, iosPassPublicUrl = Some(passIosPublicUrl))
          .create
        Get(s"/v1/public/passes.install?id=${giftCardPass.id}&type=ios&token=randomtoken") ~> routes ~> check {
          rejection should beAnInstanceOf[AuthorizationFailedRejection]
        }
      }
    }
  }
}
