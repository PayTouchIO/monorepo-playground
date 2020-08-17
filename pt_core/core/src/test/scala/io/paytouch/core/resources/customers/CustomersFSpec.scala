package io.paytouch.core.resources.customers

import java.util.UUID

import io.paytouch.implicits._

import io.paytouch.core.data.model.LoyaltyProgramRecord
import io.paytouch.core.entities._
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.json.JsonSupport
import io.paytouch.core.utils._

trait CustomersFSpec extends FSpec {
  abstract class CustomerResourceFSpecContext extends CustomerAssertions with MultipleLocationFixtures

  trait CustomerAssertions extends FSpecContext with BaseFixtures {
    val globalCustomerDao = daos.globalCustomerDao
    val customerMerchantDao = daos.customerMerchantDao
    val loyaltyMembershipDao = daos.loyaltyMembershipDao

    def assertCustomerResponse(
        entity: CustomerMerchant,
        locationIds: Seq[UUID] = Seq.empty,
        loyaltyProgramIds: Option[Seq[UUID]] = None,
        totalVisits: Option[Int] = None,
        totalSpend: Option[Seq[MonetaryAmount]] = None,
        avgTips: Option[Seq[MonetaryAmount]] = None,
        loyaltyMemberships: Option[Seq[LoyaltyMembership]] = None,
      ) = {
      val customerId = entity.id
      val globalCustomer = globalCustomerDao.findById(customerId).await.get
      val customerMerchantRecord =
        customerMerchantDao.findByCustomerIdsAndMerchantId(Seq(customerId), merchant.id).await.head

      entity.id ==== customerId
      entity.firstName ==== customerMerchantRecord.firstName
      entity.lastName ==== customerMerchantRecord.lastName
      entity.dob ==== customerMerchantRecord.dob
      entity.anniversary ==== customerMerchantRecord.anniversary
      entity.email ==== customerMerchantRecord.email
      entity.phoneNumber ==== customerMerchantRecord.phoneNumber
      entity.address.line1 ==== customerMerchantRecord.addressLine1
      entity.address.line2 ==== customerMerchantRecord.addressLine2
      entity.address.city ==== customerMerchantRecord.city
      entity.address.state ==== customerMerchantRecord.state
      entity.address.country ==== customerMerchantRecord.country
      entity.address.postalCode ==== customerMerchantRecord.postalCode
      entity.mobileStorefrontLastLogin ==== globalCustomer.mobileStorefrontLastLogin
      entity.webStorefrontLastLogin ==== globalCustomer.webStorefrontLastLogin
      entity.createdAt ==== customerMerchantRecord.createdAt
      entity.updatedAt ==== customerMerchantRecord.updatedAt
      entity.locations.map(_.map(_.id) ==== locationIds)
      entity.loyaltyPrograms.map(_.map(_.id).toSet) ==== loyaltyProgramIds.map(_.toSet)
      entity.totalVisits ==== totalVisits
      entity.totalSpend ==== totalSpend
      entity.avgTips ==== avgTips
      entity.loyaltyMemberships ==== loyaltyMemberships
    }

    def assertFullyExpandedCustomerResponse(
        customerResponse: CustomerMerchant,
        locationIds: Option[Seq[UUID]] = None,
        loyaltyProgramIds: Seq[UUID] = Seq.empty,
        loyaltyMemberships: Option[Seq[LoyaltyMembership]] = None,
      ) =
      assertCustomerResponse(
        customerResponse,
        locationIds = locationIds.getOrElse(Seq.empty),
        loyaltyProgramIds = Some(loyaltyProgramIds),
        loyaltyMemberships = loyaltyMemberships.orElse(Some(Seq.empty)),
        totalVisits = Some(0),
        totalSpend = Some(Seq(0.$$$)),
        avgTips = Some(Seq.empty),
      )

    def assertCustomerIsEnrolled(
        customerId: UUID,
        loyaltyProgram: LoyaltyProgramRecord,
        expectedBalance: Option[Int] = None,
      ) = {
      val maybeMembership =
        loyaltyMembershipDao.findByCustomerIdAndLoyaltyProgramId(merchant.id, customerId, loyaltyProgram.id).await
      maybeMembership must beSome

      val membership = maybeMembership.get
      membership.merchantOptInAt must beSome

      expectedBalance.map(balance => membership.points ==== balance)
    }

    def findCustomerMemberships(customerId: UUID, loyaltyProgram: LoyaltyProgramRecord) = {
      val loyaltyMembership =
        loyaltyMembershipDao.findByCustomerIdAndLoyaltyProgramId(merchant.id, customerId, loyaltyProgram.id).await.get
      MockedRestApi.loyaltyMembershipService.findById(loyaltyMembership.id).await.toSeq
    }

    def assertCustomerIsNotEnrolled(customerId: UUID, loyaltyProgram: LoyaltyProgramRecord) = {
      val maybeMembership =
        loyaltyMembershipDao.findByCustomerIdAndLoyaltyProgramId(merchant.id, customerId, loyaltyProgram.id).await
      (maybeMembership must beNone) or (maybeMembership.get.merchantOptInAt must beNone)
    }

    def assertUpdate(customerId: UUID, update: CustomerMerchantUpsertion) = {
      val customerMerchant =
        customerMerchantDao
          .findByCustomerIdsAndMerchantId(Seq(customerId), merchant.id)
          .await
          .head

      if (update.firstName.isDefined) customerMerchant.firstName ==== update.firstName
      if (update.lastName.isDefined) customerMerchant.lastName ==== update.lastName
      if (update.dob.isDefined) customerMerchant.dob ==== update.dob
      if (update.anniversary.isDefined) customerMerchant.anniversary ==== update.anniversary
      if (update.email.isDefined) customerMerchant.email ==== update.email
      if (update.phoneNumber.isDefined) customerMerchant.phoneNumber ==== update.phoneNumber
      if (update.billingDetails.isDefined) customerMerchant.billingDetails ==== update.billingDetails

      if (update.address.line1.isDefined) customerMerchant.addressLine1 ==== update.address.line1
      if (update.address.line2.isDefined) customerMerchant.addressLine2 ==== update.address.line2
      if (update.address.city.isDefined) customerMerchant.city ==== update.address.city
      if (update.address.state.isDefined) customerMerchant.state ==== update.address.state
      if (update.address.country.isDefined) customerMerchant.country ==== update.address.country
      if (update.address.countryCode.isDefined) customerMerchant.countryCode ==== update.address.countryCode
      if (update.address.stateCode.isDefined) customerMerchant.stateCode ==== update.address.stateCode
      if (update.address.postalCode.isDefined) customerMerchant.postalCode ==== update.address.postalCode
    }
  }
}
