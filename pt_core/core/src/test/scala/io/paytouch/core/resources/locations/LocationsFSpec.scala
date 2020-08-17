package io.paytouch.core.resources.locations

import java.util.UUID

import cats.implicits._

import io.paytouch.core.Availabilities
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.{
  LocationEmailReceiptUpdate => _,
  LocationPrintReceiptUpdate => _,
  LocationReceiptUpdate => _,
  LocationSettingsUpdate => _,
  LocationUpdate => _,
  _,
}
import io.paytouch.core.entities.enums.MerchantSetupSteps
import io.paytouch.core.entities.{ LocationSettingsUpdate, LocationUpdate, Location => LocationEntity, _ }
import io.paytouch.core.utils._

abstract class LocationsFSpec extends FSpec {

  abstract class LocationsResourceFSpecContext
      extends FSpecContext
         with MultipleLocationFixtures
         with SetupStepsAssertions {
    val imageUploadDao = daos.imageUploadDao
    val locationDao = daos.locationDao
    val locationAvailabilityDao = daos.locationAvailabilityDao
    val locationEmailReceiptDao = daos.locationEmailReceiptDao
    val locationPrintReceiptDao = daos.locationPrintReceiptDao
    val locationReceiptDao = daos.locationReceiptDao
    val locationSettingsDao = daos.locationSettingsDao
    val userLocationDao = daos.userLocationDao

    def assertResponseById(entity: LocationEntity, id: UUID) = {
      val record = locationDao.findById(id).await.get
      assertResponse(record, entity)
    }

    def assertResponse(
        record: LocationRecord,
        entity: LocationEntity,
        withSettings: Boolean = false,
        taxRateIds: Option[Seq[UUID]] = None,
        availabilityMap: Option[Availabilities] = None,
      ) = {
      entity.id ==== record.id
      entity.name ==== record.name
      entity.email ==== record.email
      entity.phoneNumber ==== record.phoneNumber.getOrElse("")
      entity.address.line1 ==== record.addressLine1
      entity.address.line2 ==== record.addressLine2
      entity.address.city ==== record.city
      entity.address.state ==== record.state
      if (entity.address.country.isDefined) entity.address.country ==== record.country
      entity.address.postalCode ==== record.postalCode
      entity.timezone ==== record.timezone
      entity.currency ==== currency
      entity.active ==== record.active

      if (withSettings) {
        entity.settings must beSome
        assertResponseSettings(record, entity.settings.get)
        assertResponseEmailReceipt(record, entity.settings.flatMap(_.locationEmailReceipt).get)
        assertResponsePrintReceipt(record, entity.settings.flatMap(_.locationPrintReceipt).get)
        assertResponseReceipt(record, entity.settings.flatMap(_.locationReceipt).get)
      }

      if (taxRateIds.isDefined) {
        entity.taxRates must beSome
        entity.taxRates.map(_.map(_.id)) ==== taxRateIds
      }

      if (availabilityMap.isDefined)
        entity.openingHours ==== availabilityMap
    }

    def assertResponseSettings(record: LocationRecord, settingsEntity: LocationSettings) = {
      val settingsRecord = locationSettingsDao.findByLocationIds(Seq(record.id), record.merchantId).await.head

      settingsEntity.locationId ==== settingsRecord.locationId
      settingsEntity.orderRoutingAuto ==== settingsRecord.orderRoutingAuto
      settingsEntity.orderTypeDineIn ==== settingsRecord.orderTypeDineIn
      settingsEntity.orderTypeTakeOut ==== settingsRecord.orderTypeTakeOut
      settingsEntity.orderTypeDeliveryRestaurant ==== settingsRecord.orderTypeDeliveryRestaurant
      settingsEntity.orderTypeInStore ==== settingsRecord.orderTypeInStore
      settingsEntity.orderTypeInStorePickUp ==== settingsRecord.orderTypeInStorePickUp
      settingsEntity.orderTypeDeliveryRetail ==== settingsRecord.orderTypeDeliveryRetail
      settingsEntity.invoicesActive ==== settingsRecord.invoicesActive
      settingsEntity.discountBelowCostActive ==== settingsRecord.discountBelowCostActive
      settingsEntity.cashDrawerManagementActive ==== settingsRecord.cashDrawerManagementActive
      settingsEntity.giftCardsActive ==== settingsRecord.giftCardsActive
      settingsEntity.paymentTypeCreditCard ==== settingsRecord.paymentTypeCreditCard
      settingsEntity.paymentTypeCash ==== settingsRecord.paymentTypeCash
      settingsEntity.paymentTypeDebitCard ==== settingsRecord.paymentTypeDebitCard
      settingsEntity.paymentTypeCheck ==== settingsRecord.paymentTypeCheck
      settingsEntity.paymentTypeGiftCard ==== settingsRecord.paymentTypeGiftCard
      settingsEntity.paymentTypeStoreCredit ==== settingsRecord.paymentTypeStoreCredit
      settingsEntity.paymentTypeEbt ==== settingsRecord.paymentTypeEbt
      settingsEntity.paymentTypeApplePay ==== settingsRecord.paymentTypeApplePay
      settingsEntity.tipsHandling ==== settingsRecord.tipsHandling
      settingsEntity.tipsOnDeviceEnabled ==== settingsRecord.tipsOnDeviceEnabled
      settingsEntity.bypassSignatureAmount ==== settingsRecord.bypassSignatureAmount
      settingsEntity.onlineStorefrontEnabled ==== settingsRecord.onlineStorefrontEnabled
      settingsEntity.deliveryProvidersEnabled ==== settingsRecord.deliveryProvidersEnabled
      settingsEntity.preauthEnabled ==== settingsRecord.preauthEnabled
    }

    def assertResponseEmailReceipt(record: LocationRecord, emailReceiptEntity: LocationEmailReceipt) = {
      val emailReceiptRecord = locationEmailReceiptDao.findByLocationIds(Seq(record.id), record.merchantId).await.head

      emailReceiptEntity.locationId ==== emailReceiptRecord.locationId
      emailReceiptEntity.headerColor ==== emailReceiptRecord.headerColor
      emailReceiptEntity.locationName ==== emailReceiptRecord.locationName
      emailReceiptEntity.address.line1 ==== emailReceiptRecord.locationAddressLine1
      emailReceiptEntity.address.line2 ==== emailReceiptRecord.locationAddressLine2
      emailReceiptEntity.address.city ==== emailReceiptRecord.locationCity
      emailReceiptEntity.address.state ==== emailReceiptRecord.locationState
      emailReceiptEntity.address.country ==== emailReceiptRecord.locationCountry
      emailReceiptEntity.address.postalCode ==== emailReceiptRecord.locationPostalCode
      emailReceiptEntity.includeItemDescription ==== emailReceiptRecord.includeItemDescription
      emailReceiptEntity.websiteUrl ==== emailReceiptRecord.websiteUrl
      emailReceiptEntity.facebookUrl ==== emailReceiptRecord.facebookUrl
      emailReceiptEntity.twitterUrl ==== emailReceiptRecord.twitterUrl
    }

    def assertResponsePrintReceipt(record: LocationRecord, printReceiptEntity: LocationPrintReceipt) = {
      val printReceiptRecord = locationPrintReceiptDao.findByLocationIds(Seq(record.id), record.merchantId).await.head

      printReceiptEntity.locationId ==== printReceiptRecord.locationId
      printReceiptEntity.headerColor ==== printReceiptRecord.headerColor
      printReceiptEntity.locationName ==== printReceiptRecord.locationName
      printReceiptEntity.address.line1 ==== printReceiptRecord.locationAddressLine1
      printReceiptEntity.address.line2 ==== printReceiptRecord.locationAddressLine2
      printReceiptEntity.address.city ==== printReceiptRecord.locationCity
      printReceiptEntity.address.state ==== printReceiptRecord.locationState
      printReceiptEntity.address.country ==== printReceiptRecord.locationCountry
      printReceiptEntity.address.postalCode ==== printReceiptRecord.locationPostalCode
      printReceiptEntity.includeItemDescription ==== printReceiptRecord.includeItemDescription
    }

    def assertResponseReceipt(record: LocationRecord, receiptEntity: LocationReceipt) = {
      val receiptRecord = locationReceiptDao.findByLocationIds(Seq(record.id), record.merchantId).await.head

      receiptEntity.locationId ==== receiptRecord.locationId
      receiptEntity.locationName ==== receiptRecord.locationName.getOrElse(record.name)
      receiptEntity.headerColor ==== receiptRecord.headerColor
      receiptEntity.address.line1 ==== receiptRecord.addressLine1.orElse(record.addressLine1)
      receiptEntity.address.line2 ==== receiptRecord.addressLine2.orElse(record.addressLine2)
      receiptEntity.address.city ==== receiptRecord.city.orElse(record.city)
      receiptEntity.address.state ==== receiptRecord.state.orElse(record.state)
      receiptEntity.address.country ==== receiptRecord.country.orElse(record.country)
      receiptEntity.address.postalCode ==== receiptRecord.postalCode.orElse(record.postalCode)
      receiptEntity.phoneNumber ==== receiptRecord.phoneNumber.orElse(record.phoneNumber.filterNot(_.isEmpty))
      receiptEntity.websiteUrl ==== receiptRecord.websiteUrl.orElse(record.website)
      receiptEntity.facebookUrl ==== receiptRecord.facebookUrl
      receiptEntity.twitterUrl ==== receiptRecord.twitterUrl
      receiptEntity.showCustomText ==== receiptRecord.showCustomText
      receiptEntity.customText ==== receiptRecord.customText
      receiptEntity.showReturnPolicy ==== receiptRecord.showReturnPolicy
      receiptEntity.returnPolicyText ==== receiptRecord.returnPolicyText
      receiptEntity.createdAt ==== receiptRecord.createdAt
      receiptEntity.updatedAt ==== receiptRecord.updatedAt

      assertResponseImageUpload(receiptRecord.locationId, ImageUploadType.EmailReceipt, receiptEntity.emailImageUrls)
      assertResponseImageUpload(receiptRecord.locationId, ImageUploadType.PrintReceipt, receiptEntity.printImageUrls)
    }

    private def assertResponseImageUpload(
        itemId: UUID,
        imageUploadType: ImageUploadType,
        imageUrls: Seq[ImageUrls],
      ) = {
      val imageUploads = imageUploadDao.findByObjectIds(Seq(itemId), imageUploadType).await
      imageUploads.map(_.id) ==== imageUrls.map(_.imageUploadId)
    }

    def assertCreation(creation: LocationCreation, id: UUID) = {

      if (!creation.address.country.isDefined) {
        val record = locationDao.findById(id).await.get
        record.country ==== "US".some
      }

      assertUpdate(creation.asUpdate, id)
      assertLocationReceipt(creation, id)
    }

    def assertUpdate(update: LocationUpdate, id: UUID) = {
      val dbLocation = locationDao.findById(id).await.get
      if (update.name.isDefined) update.name ==== Some(dbLocation.name)
      if (update.email.isDefined) update.email ==== dbLocation.email
      if (update.phoneNumber.isDefined) update.phoneNumber ==== dbLocation.phoneNumber
      if (update.timezone.isDefined) update.timezone === Some(dbLocation.timezone)

      assertUpdateAddress(update.address, dbLocation)
      update.openingHours.map(openHours => assertOpeningHours(openHours, dbLocation))

      val userLocation = userLocationDao.findOneByItemIdAndLocationId(itemId = user.id, locationId = id).await
      userLocation should beSome

      val locationReceipt =
        locationReceiptDao.findByLocationId(merchantId = merchant.id, locationId = id).await.headOption
      locationReceipt should beSome
    }

    def assertLocationUpdated(record: LocationRecord) = {
      val updatedRecord = locationDao.findById(record.id).await.get
      record.updatedAt !=== updatedRecord.updatedAt
    }

    private def assertUpdateAddress(addressUpdate: AddressUpsertion, dbLocation: LocationRecord) = {
      if (addressUpdate.line1.isDefined) addressUpdate.line1 ==== dbLocation.addressLine1
      if (addressUpdate.line2.isDefined) addressUpdate.line2 ==== dbLocation.addressLine2
      if (addressUpdate.city.isDefined) addressUpdate.city ==== dbLocation.city
      if (addressUpdate.state.isDefined) addressUpdate.state ==== dbLocation.state
      if (addressUpdate.country.isDefined) addressUpdate.country ==== dbLocation.country
      if (addressUpdate.postalCode.isDefined) addressUpdate.postalCode ==== dbLocation.postalCode
    }

    private def assertOpeningHours(openingHours: Availabilities, dbLocation: LocationRecord) = {
      val availabilities = locationAvailabilityDao.findByItemId(dbLocation.id).await
      availabilities.size ==== 1
      val availability = availabilities.head
      availability.start ==== openingHours.values.head.head.start
      availability.end ==== openingHours.values.head.head.end
      availability.sunday ==== openingHours.keySet.contains(Weekdays.Sunday)
      availability.monday ==== openingHours.keySet.contains(Weekdays.Monday)
      availability.tuesday ==== openingHours.keySet.contains(Weekdays.Tuesday)
      availability.wednesday ==== openingHours.keySet.contains(Weekdays.Wednesday)
      availability.thursday ==== openingHours.keySet.contains(Weekdays.Thursday)
      availability.friday ==== openingHours.keySet.contains(Weekdays.Friday)
      availability.saturday ==== openingHours.keySet.contains(Weekdays.Saturday)
    }

    def assertUpdateSettings(location: LocationRecord, u: LocationSettingsUpdate) = {
      val r = locationSettingsDao.findByLocationIds(Seq(location.id), location.merchantId).await.head

      if (u.orderRoutingAuto.isDefined) u.orderRoutingAuto ==== Some(r.orderRoutingAuto)
      if (u.orderTypeDineIn.isDefined) u.orderTypeDineIn ==== Some(r.orderTypeDineIn)
      if (u.orderTypeTakeOut.isDefined) u.orderTypeTakeOut ==== Some(r.orderTypeTakeOut)
      if (u.orderTypeDeliveryRestaurant.isDefined)
        u.orderTypeDeliveryRestaurant ==== Some(r.orderTypeDeliveryRestaurant)
      if (u.orderTypeInStore.isDefined) u.orderTypeInStore ==== Some(r.orderTypeInStore)
      if (u.orderTypeInStorePickUp.isDefined) u.orderTypeInStorePickUp ==== Some(r.orderTypeInStorePickUp)
      if (u.orderTypeDeliveryRetail.isDefined) u.orderTypeDeliveryRetail ==== Some(r.orderTypeDeliveryRetail)
      if (u.invoicesActive.isDefined) u.invoicesActive ==== Some(r.invoicesActive)
      if (u.discountBelowCostActive.isDefined) u.discountBelowCostActive ==== Some(r.discountBelowCostActive)
      if (u.cashDrawerManagement.isDefined) u.cashDrawerManagement ==== Some(r.cashDrawerManagement)
      if (u.giftCardsActive.isDefined) u.giftCardsActive ==== Some(r.giftCardsActive)
      if (u.paymentTypeCreditCard.isDefined) u.paymentTypeCreditCard ==== Some(r.paymentTypeCreditCard)
      if (u.paymentTypeCash.isDefined) u.paymentTypeCash ==== Some(r.paymentTypeCash)
      if (u.paymentTypeDebitCard.isDefined) u.paymentTypeDebitCard ==== Some(r.paymentTypeDebitCard)
      if (u.paymentTypeCheck.isDefined) u.paymentTypeCheck ==== Some(r.paymentTypeCheck)
      if (u.paymentTypeGiftCard.isDefined) u.paymentTypeGiftCard ==== Some(r.paymentTypeGiftCard)
      if (u.paymentTypeStoreCredit.isDefined) u.paymentTypeStoreCredit ==== Some(r.paymentTypeStoreCredit)
      if (u.paymentTypeEbt.isDefined) u.paymentTypeEbt ==== Some(r.paymentTypeEbt)
      if (u.paymentTypeApplePay.isDefined) u.paymentTypeApplePay ==== Some(r.paymentTypeApplePay)
      if (u.tipsHandling.isDefined) u.tipsHandling ==== Some(r.tipsHandling)
      if (u.tipsOnDeviceEnabled.isDefined) u.tipsOnDeviceEnabled ==== Some(r.tipsOnDeviceEnabled)
      if (u.bypassSignatureAmount.isDefined) u.bypassSignatureAmount ==== Some(r.bypassSignatureAmount)
      if (u.deliveryProvidersEnabled.isDefined) u.deliveryProvidersEnabled ==== Some(r.deliveryProvidersEnabled)

      if (u.locationEmailReceipt.isDefined) assertUpdateEmailReceipt(location, u.locationEmailReceipt.get)
      if (u.locationPrintReceipt.isDefined) assertUpdatePrintReceipt(location, u.locationPrintReceipt.get)
      if (u.locationReceipt.isDefined) assertUpdateReceipt(location, u.locationReceipt.get)

      assertSetupStepCompleted(merchant, MerchantSetupSteps.DesignReceipts)
    }

    private def assertUpdateEmailReceipt(location: LocationRecord, update: LocationEmailReceiptUpdate) = {
      val emailReceiptRecord =
        locationEmailReceiptDao.findByLocationIds(Seq(location.id), location.merchantId).await.head

      if (update.headerColor.isDefined) update.headerColor ==== emailReceiptRecord.headerColor
      if (update.locationName.isDefined) update.locationName ==== emailReceiptRecord.locationName
      if (update.includeItemDescription.isDefined)
        update.includeItemDescription ==== Some(emailReceiptRecord.includeItemDescription)
      if (update.websiteUrl.isDefined) update.websiteUrl ==== emailReceiptRecord.websiteUrl
      if (update.facebookUrl.isDefined) update.facebookUrl ==== emailReceiptRecord.facebookUrl
      if (update.twitterUrl.isDefined) update.twitterUrl ==== emailReceiptRecord.twitterUrl

      assertUpdateAddress(update.address, emailReceiptRecord)
    }

    private def assertUpdateAddress(addressUpdate: AddressUpsertion, record: LocationEmailReceiptRecord) = {
      if (addressUpdate.line1.isDefined) addressUpdate.line1 ==== record.locationAddressLine1
      if (addressUpdate.line2.isDefined) addressUpdate.line2 ==== record.locationAddressLine2
      if (addressUpdate.city.isDefined) addressUpdate.city ==== record.locationCity
      if (addressUpdate.state.isDefined) addressUpdate.state ==== record.locationState
      if (addressUpdate.country.isDefined) addressUpdate.country ==== record.locationCountry
      if (addressUpdate.postalCode.isDefined) addressUpdate.postalCode ==== record.locationPostalCode
    }

    private def assertUpdatePrintReceipt(location: LocationRecord, update: LocationPrintReceiptUpdate) = {
      val printReceiptRecord =
        locationPrintReceiptDao.findByLocationIds(Seq(location.id), location.merchantId).await.head

      if (update.headerColor.isDefined) update.headerColor ==== printReceiptRecord.headerColor
      if (update.locationName.isDefined) update.locationName ==== printReceiptRecord.locationName
      if (update.includeItemDescription.isDefined)
        update.includeItemDescription ==== Some(printReceiptRecord.includeItemDescription)

      assertUpdateAddress(update.address, printReceiptRecord)
    }

    private def assertUpdateReceipt(location: LocationRecord, update: LocationReceiptUpdate) = {
      val receiptRecord = locationReceiptDao.findByLocationIds(Seq(location.id), location.merchantId).await.head

      if (update.locationName.isDefined) update.locationName ==== receiptRecord.locationName
      if (update.headerColor.isDefined) update.headerColor ==== receiptRecord.headerColor
      if (update.phoneNumber.isDefined) update.phoneNumber ==== receiptRecord.phoneNumber
      if (update.websiteUrl.isDefined) update.websiteUrl ==== receiptRecord.websiteUrl
      if (update.facebookUrl.isDefined) update.facebookUrl ==== receiptRecord.facebookUrl
      if (update.twitterUrl.isDefined) update.twitterUrl ==== receiptRecord.twitterUrl
      if (update.showCustomText.isDefined) update.showCustomText ==== Some(receiptRecord.showCustomText)
      if (update.customText.isDefined) update.customText ==== receiptRecord.customText
      if (update.showReturnPolicy.isDefined) update.showReturnPolicy ==== Some(receiptRecord.showReturnPolicy)
      if (update.returnPolicyText.isDefined) update.returnPolicyText ==== receiptRecord.returnPolicyText

      assertUpdateAddress(update.address, receiptRecord)
      assertUpdateImageUploads(update.emailImageUploadIds, receiptRecord.locationId, ImageUploadType.EmailReceipt)
      assertUpdateImageUploads(update.printImageUploadIds, receiptRecord.locationId, ImageUploadType.PrintReceipt)
    }

    private def assertUpdateAddress(addressUpdate: AddressUpsertion, record: LocationPrintReceiptRecord) = {
      if (addressUpdate.line1.isDefined) addressUpdate.line1 ==== record.locationAddressLine1
      if (addressUpdate.line2.isDefined) addressUpdate.line2 ==== record.locationAddressLine2
      if (addressUpdate.city.isDefined) addressUpdate.city ==== record.locationCity
      if (addressUpdate.state.isDefined) addressUpdate.state ==== record.locationState
      if (addressUpdate.country.isDefined) addressUpdate.country ==== record.locationCountry
      if (addressUpdate.postalCode.isDefined) addressUpdate.postalCode ==== record.locationPostalCode
    }

    private def assertUpdateAddress(addressUpdate: AddressUpsertion, record: LocationReceiptRecord) = {
      if (addressUpdate.line1.isDefined) addressUpdate.line1 ==== record.addressLine1
      if (addressUpdate.line2.isDefined) addressUpdate.line2 ==== record.addressLine2
      if (addressUpdate.city.isDefined) addressUpdate.city ==== record.city
      if (addressUpdate.state.isDefined) addressUpdate.state ==== record.state
      if (addressUpdate.country.isDefined) addressUpdate.country ==== record.country
      if (addressUpdate.postalCode.isDefined) addressUpdate.postalCode ==== record.postalCode
    }

    private def assertUpdateImageUploads(
        imageUploadIds: Option[Seq[UUID]],
        locationId: UUID,
        imageUploadType: ImageUploadType,
      ) = {
      val imageUploads = imageUploadDao.findByObjectIds(Seq(locationId), imageUploadType).await
      if (imageUploadIds.isDefined) imageUploadIds ==== Some(imageUploads.map(_.id))
    }

    private def assertLocationReceipt(creation: LocationCreation, locationId: UUID) = {
      val maybeLocationReceipt =
        locationReceiptDao.findByLocationId(merchantId = merchant.id, locationId = locationId).await.headOption
      maybeLocationReceipt should beSome

      val locationReceipt = maybeLocationReceipt.get
      locationReceipt.merchantId ==== merchant.id
      locationReceipt.locationId ==== locationId
      locationReceipt.locationName ==== Some(creation.name)
      locationReceipt.addressLine1 ==== creation.address.line1
      locationReceipt.addressLine2 ==== creation.address.line2
      locationReceipt.city ==== creation.address.city
      locationReceipt.state ==== creation.address.state
      locationReceipt.country ==== creation.address.country
      locationReceipt.postalCode ==== creation.address.postalCode
      locationReceipt.phoneNumber ==== creation.phoneNumber
      locationReceipt.websiteUrl ==== creation.website
    }

  }
}
