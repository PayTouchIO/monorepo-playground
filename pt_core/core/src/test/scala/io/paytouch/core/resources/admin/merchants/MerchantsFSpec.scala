package io.paytouch.core.resources.admin.merchants

import java.util.UUID

import com.github.t3hnar.bcrypt._

import io.paytouch.core._
import io.paytouch.core.data._
import io.paytouch.core.utils._
import io.paytouch.core.data.daos.LocationAvailabilityDao

abstract class MerchantsFSpec extends FSpec {
  abstract class MerchantResourceFSpecContext
      extends FSpecContext
         with AvailabilitiesSupport[LocationAvailabilityDao]
         with AdminFixtures {
    val catalogDao = daos.catalogDao
    val locationDao = daos.locationDao
    val availabilityDao = daos.locationAvailabilityDao
    val merchantDao = daos.merchantDao
    val userDao = daos.userDao
    val userRoleDao = daos.userRoleDao

    def assertCreation(
        creation: entities.MerchantCreation,
        id: UUID,
        features: Option[entities.MerchantFeatures] = None,
        legalDetails: Option[entities.LegalDetails] = None,
      ) = {
      val record = merchantDao.findById(id).await.get

      record.id ==== id
      record.active should beTrue
      record.businessType ==== creation.businessType
      record.businessName ==== creation.businessName
      record.restaurantType ==== creation.restaurantType
      record.currency ==== creation.currency
      record.mode ==== creation.mode
      record.defaultZoneId ==== creation.zoneId

      val userId = userDao.findUserLoginByEmail(creation.email).await.get.id
      val userOwnerRecord = userDao.findById(userId).await.get

      userOwnerRecord.email ==== creation.email
      userOwnerRecord.firstName ==== creation.firstName
      userOwnerRecord.lastName ==== creation.lastName
      userOwnerRecord.pin ==== creation.pin.map(sha1Encrypt)
      userOwnerRecord.isOwner should beTrue
      userOwnerRecord.active should beTrue
      userOwnerRecord.userRoleId should beSome
      creation.password.isBcrypted(userOwnerRecord.encryptedPassword) must beTrue

      val userRoleRecord = userRoleDao.findById(userOwnerRecord.userRoleId.get).await.get

      userRoleRecord.merchantId ==== id
      userRoleRecord.name ==== {
        creation.setupType match {
          case model.enums.SetupType.Dash     => model.UserRoleUpdate.Manager
          case model.enums.SetupType.Paytouch => model.UserRoleUpdate.Admin
        }
      }

      record.mode match {
        case model.enums.MerchantMode.Production => assertProductLocation(record)
        case model.enums.MerchantMode.Demo       => afterAWhile(assertDemoLocation(record))
      }

      features.foreach(record.features ==== _)

      legalDetails.foreach { details =>
        record.legalDetails.foreach(_ ==== details)
        record.legalCountry ==== details.country
      }

      catalogDao.findByMerchantIdAndType(id, entities.enums.CatalogType.DefaultMenu).await must beSome
    }

    def assertUpdate(
        update: entities.AdminMerchantUpdate,
        id: UUID,
        features: Option[entities.MerchantFeatures] = None,
        legalDetails: Option[entities.LegalDetails] = None,
      ) = {
      val record = merchantDao.findById(id).await.get
      id ==== record.id
      record.active should beTrue
      if (update.businessType.isDefined) update.businessType ==== Some(record.businessType)
      if (update.businessName.isDefined) update.businessName ==== Some(record.businessName)
      if (update.restaurantType.isDefined) update.restaurantType ==== Some(record.restaurantType)
      if (update.zoneId.isDefined) update.zoneId ==== Some(record.defaultZoneId)
      if (update.currency.isDefined) update.currency ==== Some(record.currency)

      features.foreach(record.features ==== _)

      legalDetails.foreach { details =>
        record.legalDetails.foreach(_ ==== details)
        record.legalCountry ==== details.country
      }
    }

    def assertResponse(
        entity: entities.Merchant,
        record: model.MerchantRecord,
        ownerUser: Option[model.UserRecord],
      ) = {
      entity.id ==== record.id
      entity.businessType ==== record.businessType
      entity.name ==== record.businessName
      entity.restaurantType ==== record.restaurantType
      entity.currency ==== record.currency
      entity.defaultZoneId ==== record.defaultZoneId
      entity.ownerUser.map(_.id) ==== ownerUser.map(_.id)
    }

    private def assertProductLocation(merchant: model.MerchantRecord) = {
      val users = userDao.findAllByMerchantId(merchant.id).await
      users.size ==== 1

      val locations = locationDao.findAllByMerchantId(merchant.id).await
      locations.size ==== 1

      locations.head.name ==== merchant.businessName
      locations.head.timezone ==== merchant.defaultZoneId
      locations.head.email ==== Some(users.head.email)
    }

    private def assertDemoLocation(merchant: model.MerchantRecord) = {
      val actualLocations =
        locationDao
          .findAllByMerchantId(merchant.id)
          .await

      val expectedLocations =
        merchant.businessType match {
          case model.enums.BusinessType.QSR        => 1
          case model.enums.BusinessType.Restaurant => 1
          case model.enums.BusinessType.Retail     => 2
        }

      actualLocations.size ==== expectedLocations
    }
  }
}
