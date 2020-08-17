package io.paytouch.core.resources.users

import java.util.UUID

import com.github.t3hnar.bcrypt._

import org.scalacheck.Arbitrary

import io.paytouch.core.data.model.{ ImageUploadRecord, MerchantRecord, UserRecord, UserRoleRecord }
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities.{ User => UserEntity, _ }
import io.paytouch.core.entities.enums.{ LoginSource, MerchantSetupStatus, MerchantSetupSteps }
import io.paytouch.core.services.UtilService
import io.paytouch.core.utils._

abstract class UsersFSpec extends FSpec {

  abstract class UserResourceFSpecContext extends FSpecContext with MultipleLocationFixtures with SetupStepsAssertions {
    val imageUploadDao = daos.imageUploadDao
    val merchantDao = daos.merchantDao
    val userDao = daos.userDao
    val userLocationDao = daos.userLocationDao
    val userRoleDao = daos.userRoleDao

    def buildAccess(role: UserRoleRecord): Seq[LoginSource] = {
      import LoginSource._
      Map(
        role.hasDashboardAccess -> PtDashboard,
        role.hasRegisterAccess -> PtRegister,
        role.hasTicketsAccess -> PtTickets,
      ).view.filterKeys(identity).values.toSeq
    }

    def assertCreation(
        creation: UserCreation,
        id: UUID,
        imageUpload: Option[ImageUploadRecord],
      ) = {
      assertUpdate(creation.asUpdate, id, creation.isOwner.getOrElse(false), imageUpload)

      assertSetupStepCompleted(merchant, MerchantSetupSteps.SetupEmployees)
    }

    def assertUpdate(
        update: UserUpdate,
        userId: UUID,
        userOwner: Boolean,
        imageUpload: Option[ImageUploadRecord],
      ) = {
      val record = userDao.findById(userId).await.get

      if (update.userRoleId.isDefined) update.userRoleId ==== record.userRoleId
      if (update.firstName.isDefined) update.firstName ==== Some(record.firstName)
      if (update.lastName.isDefined) update.lastName ==== Some(record.lastName)
      if (update.password.isDefined) update.password.get.isBcrypted(record.encryptedPassword) must beTrue
      if (update.email.isDefined) update.email.map(_.toLowerCase) ==== Some(record.email)
      if (update.pin.isDefined) update.pin.toOption.map(sha1Encrypt) ==== record.pin
      if (update.dob.isDefined) update.dob ==== record.dob
      if (update.phoneNumber.isDefined) update.phoneNumber ==== record.phoneNumber
      if (update.avatarBgColor.isDefined) update.avatarBgColor ==== record.avatarBgColor
      if (update.hourlyRateAmount.isDefined) update.hourlyRateAmount ==== record.hourlyRateAmount
      if (update.overtimeRateAmount.isDefined) update.overtimeRateAmount ==== record.overtimeRateAmount
      if (update.paySchedule.isDefined) update.paySchedule ==== record.paySchedule

      if (userOwner) record.isOwner ==== userOwner
      else if (update.isOwner.isDefined) update.isOwner ==== Some(record.isOwner)

      if (userOwner) record.active ==== userOwner
      else if (update.active.isDefined) update.active === Some(record.active)

      assertUpdateAddress(update.address, record)
      if (!userOwner && update.locationIds.isDefined) {
        val userLocations = userLocationDao.findByItemId(userId).await
        update.locationIds ==== Some(userLocations.map(_.locationId))
      }

      assertUpdateImageUpload(imageUpload, userId, ImageUploadType.User)
    }

    private def assertUpdateAddress(addressUpdate: AddressUpsertion, userRecord: UserRecord) = {
      if (addressUpdate.line1.isDefined) addressUpdate.line1 ==== userRecord.addressLine1
      if (addressUpdate.line2.isDefined) addressUpdate.line2 ==== userRecord.addressLine2
      if (addressUpdate.city.isDefined) addressUpdate.city ==== userRecord.city
      if (addressUpdate.state.isDefined) addressUpdate.state ==== userRecord.state
      if (addressUpdate.country.isDefined) addressUpdate.country ==== userRecord.country
      if (addressUpdate.postalCode.isDefined) addressUpdate.postalCode ==== userRecord.postalCode
    }

    def assertResponse(
        entity: UserEntity,
        record: UserRecord,
        withPermissions: Boolean,
        imageUploads: Seq[ImageUploadRecord] = Seq.empty,
        access: Option[Seq[LoginSource]] = None,
        merchant: Option[MerchantRecord] = None,
        withMerchantSetupSteps: Boolean = false,
        merchantLegalDetails: Option[LegalDetails] = None,
      ) = {
      entity.id ==== record.id
      entity.firstName ==== record.firstName
      entity.lastName ==== record.lastName
      entity.email ==== record.email
      entity.merchantId ==== record.merchantId
      entity.userRoleId ==== record.userRoleId
      entity.dob ==== record.dob
      entity.phoneNumber ==== record.phoneNumber
      entity.address.line1 ==== record.addressLine1
      entity.address.line2 ==== record.addressLine2
      entity.address.city ==== record.city
      entity.address.country ==== record.country
      entity.address.state ==== record.state
      entity.address.postalCode ==== record.postalCode
      entity.avatarBgColor ==== record.avatarBgColor
      entity.active ==== record.active
      entity.hourlyRate.map(_.amount) ==== record.hourlyRateAmount
      entity.overtimeRate.map(_.amount) ==== record.overtimeRateAmount
      entity.paySchedule ==== record.paySchedule
      entity.dashboardLastLoginAt ==== record.dashboardLastLoginAt
      entity.registerLastLoginAt ==== record.registerLastLoginAt
      entity.avatarImageUrls.map(_.imageUploadId) should containTheSameElementsAs(imageUploads.map(_.id))
      entity.avatarImageUrls.map(_.urls) should containTheSameElementsAs(imageUploads.map(_.urls.getOrElse(Map.empty)))
      entity.createdAt ==== record.createdAt
      entity.updatedAt ==== record.updatedAt

      entity.userRole.map(assertUserRole(_, withPermissions))
      entity.access ==== access

      merchant.foreach(m => assertMerchant(entity.merchant, m, withMerchantSetupSteps, merchantLegalDetails))
    }

    private def assertUserRole(userRole: UserRole, withPermissions: Boolean) = {
      val record = userRoleDao.findById(userRole.id).await.get

      userRole.id ==== record.id
      userRole.name ==== record.name
      userRole.hasDashboardAccess ==== record.hasDashboardAccess
      userRole.hasRegisterAccess ==== record.hasRegisterAccess
      userRole.dashboard ==== (if (withPermissions) Some(record.dashboard) else None)
      userRole.register ==== (if (withPermissions) Some(record.register) else None)
      userRole.createdAt ==== record.createdAt
      userRole.updatedAt ==== record.updatedAt
    }

    private def assertUpdateImageUpload(
        record: Option[ImageUploadRecord],
        itemId: UUID,
        imageUploadType: ImageUploadType,
      ) = {
      val imageUploads = imageUploadDao.findByObjectIds(Seq(itemId), imageUploadType).await
      imageUploads.map(_.id) ==== record.map(_.id).toSeq
    }

    def assertMerchant(
        maybeMerchantEntity: Option[Merchant],
        merchantRecord: MerchantRecord,
        withMerchantSetupSteps: Boolean,
        merchantLegalDetails: Option[LegalDetails],
      ) = {
      maybeMerchantEntity must beSome
      val merchantEntity = maybeMerchantEntity.get
      merchantEntity.id ==== merchantRecord.id
      merchantEntity.businessType ==== merchantRecord.businessType
      merchantEntity.setupCompleted ==== merchantRecord.setupCompleted

      if (withMerchantSetupSteps)
        merchantEntity.setupSteps must beSome
      merchantEntity.legalDetails ==== merchantLegalDetails
      merchantLegalDetails.foreach { details =>
        merchantEntity.legalCountry ==== details.country.getOrElse(UtilService.Geo.UnitedStates)
      }
    }
  }
}
