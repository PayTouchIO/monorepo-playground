package io.paytouch.core.resources.locations

import java.time.LocalTime

import akka.http.scaladsl.model.StatusCodes

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.CashDrawerManagementMode
import io.paytouch.core.entities.enums.TipsHandlingMode
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

@scala.annotation.nowarn("msg=Auto-application")
class LocationsUpdateSettingsFSpec extends LocationsFSpec {
  abstract class LocationsUpdateSettingsFSpecContext extends LocationsResourceFSpecContext

  "GET /v1/locations.update_settings?location_id=$" in {
    "if request has valid token" in {
      "if a location settings does not exist" should {
        "create location settings and return them" in new LocationsUpdateSettingsFSpecContext {
          val emailReceiptUpdate = random[LocationEmailReceiptUpdate].copy(imageUploadId = None)
          val printReceiptUpdate = random[LocationPrintReceiptUpdate].copy(imageUploadId = None)
          val receiptUpdate = random[LocationReceiptUpdate]

          val settingsUpdate = random[LocationSettingsUpdate].copy(
            locationEmailReceipt = Some(emailReceiptUpdate),
            locationPrintReceipt = Some(printReceiptUpdate),
            locationReceipt = Some(receiptUpdate),
          )

          Post(s"/v1/locations.update_settings?location_id=${rome.id}", settingsUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val locationSettings = responseAs[ApiResponse[LocationSettings]].data
            assertResponseSettings(rome, locationSettings)
            assertUpdateSettings(rome, settingsUpdate)
            assertLocationUpdated(rome)
          }
        }
      }

      "if a location settings already exists" in {
        "if image uploads are sent as empty" should {
          "update location settings, remove image upload ids and return them" in new LocationsUpdateSettingsFSpecContext {
            val romeSettings = Factory
              .locationSettings(rome)
              .create

            val emailReceipt = Factory
              .imageUpload(
                merchant,
                objectId = Some(rome.id),
                imageUploadType = Some(ImageUploadType.EmailReceipt),
              )
              .create

            val printReceipt = Factory
              .imageUpload(
                merchant,
                objectId = Some(rome.id),
                imageUploadType = Some(ImageUploadType.PrintReceipt),
              )
              .create

            val emailReceiptUpdate =
              random[LocationEmailReceiptUpdate]
                .copy(imageUploadId = None)

            val printReceiptUpdate =
              random[LocationPrintReceiptUpdate]
                .copy(imageUploadId = None)

            val receiptUpdate =
              random[LocationReceiptUpdate]
                .copy(emailImageUploadIds = Some(Seq.empty), printImageUploadIds = Some(Seq.empty))

            val settingsUpdate =
              random[LocationSettingsUpdate]
                .copy(
                  locationEmailReceipt = Some(emailReceiptUpdate),
                  locationPrintReceipt = Some(printReceiptUpdate),
                  locationReceipt = Some(receiptUpdate),
                  deliveryProvidersEnabled = true.some,
                )

            Post(s"/v1/locations.update_settings?location_id=${rome.id}", settingsUpdate)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val locationSettings = responseAs[ApiResponse[LocationSettings]].data
              assertResponseSettings(rome, locationSettings)
              assertUpdateSettings(rome, settingsUpdate)
              assertLocationUpdated(rome)
            }
          }
        }

        "if image uploads are sent" should {
          "update location settings, image upload ids and return them" in new LocationsUpdateSettingsFSpecContext {
            val romeSettings = Factory.locationSettings(rome).create

            val emailReceipt = Factory
              .imageUpload(merchant, objectId = Some(rome.id), imageUploadType = Some(ImageUploadType.EmailReceipt))
              .create
            val printReceipt = Factory
              .imageUpload(merchant, objectId = Some(rome.id), imageUploadType = Some(ImageUploadType.PrintReceipt))
              .create

            val emailReceiptUpdate = random[LocationEmailReceiptUpdate].copy(imageUploadId = None)
            val printReceiptUpdate = random[LocationPrintReceiptUpdate].copy(imageUploadId = None)
            val receiptUpdate = random[LocationReceiptUpdate]
              .copy(emailImageUploadIds = Some(Seq(emailReceipt.id)), printImageUploadIds = Some(Seq(printReceipt.id)))
            val settingsUpdate = random[LocationSettingsUpdate].copy(
              locationEmailReceipt = Some(emailReceiptUpdate),
              locationPrintReceipt = Some(printReceiptUpdate),
            )

            Post(s"/v1/locations.update_settings?location_id=${rome.id}", settingsUpdate)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val locationSettings = responseAs[ApiResponse[LocationSettings]].data
              assertResponseSettings(rome, locationSettings)
              assertUpdateSettings(rome, settingsUpdate)
              assertLocationUpdated(rome)
            }
          }
        }
      }

      "cashDrawerManagement" should {
        "convert from legacy fields" in new LocationsUpdateSettingsFSpecContext {
          val settingsUpdateTrue = random[LocationSettingsUpdate].copy(
            cashDrawerManagement = None,
            cashDrawerManagementActive = Some(true),
          )

          Post(s"/v1/locations.update_settings?location_id=${rome.id}", settingsUpdateTrue)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val locationSettings = responseAs[ApiResponse[LocationSettings]].data
            locationSettings.cashDrawerManagementActive ==== true
            locationSettings.cashDrawerManagement ==== CashDrawerManagementMode.Unlocked
          }

          val settingsUpdateFalse = random[LocationSettingsUpdate].copy(
            cashDrawerManagement = None,
            cashDrawerManagementActive = Some(false),
          )

          Post(s"/v1/locations.update_settings?location_id=${rome.id}", settingsUpdateFalse)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val locationSettings = responseAs[ApiResponse[LocationSettings]].data
            locationSettings.cashDrawerManagementActive ==== false
            locationSettings.cashDrawerManagement ==== CashDrawerManagementMode.Disabled
          }
        }

        "convert to legacy fields" in new LocationsUpdateSettingsFSpecContext {
          val settingsUpdateUnlocked =
            random[LocationSettingsUpdate]
              .copy(
                cashDrawerManagement = Some(CashDrawerManagementMode.Unlocked),
                cashDrawerManagementActive = None,
              )

          Post(s"/v1/locations.update_settings?location_id=${rome.id}", settingsUpdateUnlocked)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val locationSettings = responseAs[ApiResponse[LocationSettings]].data
            locationSettings.cashDrawerManagement ==== CashDrawerManagementMode.Unlocked
            locationSettings.cashDrawerManagementActive ==== true
          }

          val settingsUpdateDisabled = random[LocationSettingsUpdate].copy(
            cashDrawerManagement = Some(CashDrawerManagementMode.Disabled),
            cashDrawerManagementActive = None,
          )

          Post(s"/v1/locations.update_settings?location_id=${rome.id}", settingsUpdateDisabled)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val locationSettings = responseAs[ApiResponse[LocationSettings]].data
            locationSettings.cashDrawerManagement ==== CashDrawerManagementMode.Disabled
            locationSettings.cashDrawerManagementActive ==== false
          }

          val settingsUpdateLocked = random[LocationSettingsUpdate].copy(
            cashDrawerManagement = Some(CashDrawerManagementMode.Locked),
            cashDrawerManagementActive = None,
          )

          Post(s"/v1/locations.update_settings?location_id=${rome.id}", settingsUpdateLocked)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val locationSettings = responseAs[ApiResponse[LocationSettings]].data
            locationSettings.cashDrawerManagement ==== CashDrawerManagementMode.Locked
            locationSettings.cashDrawerManagementActive ==== true
          }
        }
      }

      "tips_enabled conversions" should {
        "convert tips_enabled = true to tips_handling = tip_jar" in new LocationsUpdateSettingsFSpecContext {
          val creation = random[LocationSettingsUpdate].copy(tipsEnabled = Some(true), tipsHandling = None)

          Post(s"/v1/locations.update_settings?location_id=${rome.id}", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val locationSettings = responseAs[ApiResponse[LocationSettings]].data
            locationSettings.tipsEnabled ==== true
            locationSettings.tipsHandling ==== TipsHandlingMode.TipJar
          }
        }

        "convert tips_enabled = false to tips_handling = disabled" in new LocationsUpdateSettingsFSpecContext {
          val creation = random[LocationSettingsUpdate].copy(tipsEnabled = Some(false), tipsHandling = None)

          Post(s"/v1/locations.update_settings?location_id=${rome.id}", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val locationSettings = responseAs[ApiResponse[LocationSettings]].data
            locationSettings.tipsEnabled ==== false
            locationSettings.tipsHandling ==== TipsHandlingMode.Disabled
          }
        }

        "ignore tips_enabled when tips_handling contradicts it" in new LocationsUpdateSettingsFSpecContext {
          val creation =
            random[LocationSettingsUpdate].copy(tipsEnabled = Some(false), tipsHandling = Some(TipsHandlingMode.TipJar))

          Post(s"/v1/locations.update_settings?location_id=${rome.id}", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val locationSettings = responseAs[ApiResponse[LocationSettings]].data
            locationSettings.tipsEnabled ==== true
            locationSettings.tipsHandling ==== TipsHandlingMode.TipJar
          }
        }
      }

      "cfd settings" should {
        "update custom text" in new LocationsUpdateSettingsFSpecContext {
          val text = Some("Hello world")

          val creation =
            random[LocationSettingsUpdate].copy(
              cfd = Some(CfdSettingsUpdate(showCustomText = Some(true), customText = text)),
            )

          Post(s"/v1/locations.update_settings?location_id=${rome.id}", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val locationSettings = responseAs[ApiResponse[LocationSettings]].data
            locationSettings.cfd.splashImageUrls ==== Seq.empty
            locationSettings.cfd.showCustomText ==== true
            locationSettings.cfd.customText ==== text
          }
        }

        "update splash image" in new LocationsUpdateSettingsFSpecContext {
          val image = Factory.imageUpload(merchant, imageUploadType = Some(ImageUploadType.CfdSplashScreen)).create

          val creation =
            random[LocationSettingsUpdate].copy(
              cfd = Some(CfdSettingsUpdate(splashImageUploadIds = Some(Seq(image.id)))),
            )

          Post(s"/v1/locations.update_settings?location_id=${rome.id}", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val locationSettings = responseAs[ApiResponse[LocationSettings]].data
            locationSettings.cfd.splashImageUrls.length ==== 1
            locationSettings.cfd.splashImageUrls.head.imageUploadId ==== image.id
          }
        }
      }

      "online order settings" should {
        "update estimated prep time" in new LocationsUpdateSettingsFSpecContext {
          val time = 25

          val creation =
            random[LocationSettingsUpdate].copy(
              onlineOrder = Some(
                OnlineOrderSettingsUpdate(
                  defaultEstimatedPrepTimeInMins = time,
                ),
              ),
            )

          Post(s"/v1/locations.update_settings?location_id=${rome.id}", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val locationSettings = responseAs[ApiResponse[LocationSettings]].data
            locationSettings.onlineOrder.defaultEstimatedPrepTimeInMins ==== Some(time)
          }
        }
      }
    }
  }
}
