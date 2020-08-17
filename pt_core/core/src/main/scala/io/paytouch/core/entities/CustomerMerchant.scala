package io.paytouch.core.entities

import java.time.{ LocalDate, ZonedDateTime }
import java.util.UUID

import io.paytouch.core.entities.enums.{ CustomerSource, ExposedName }

final case class CustomerMerchant(
    id: UUID,
    firstName: Option[String],
    lastName: Option[String],
    dob: Option[LocalDate],
    anniversary: Option[LocalDate],
    email: Option[String],
    phoneNumber: Option[String],
    address: Address,
    mobileStorefrontLastLogin: Option[ZonedDateTime],
    webStorefrontLastLogin: Option[ZonedDateTime],
    totalVisits: Option[Int],
    totalSpend: Option[Seq[MonetaryAmount]],
    avgTips: Option[Seq[MonetaryAmount]],
    locations: Option[Seq[Location]],
    loyaltyPrograms: Option[Seq[LoyaltyProgram]],
    loyaltyStatuses: Option[Seq[LoyaltyMembership]],
    loyaltyMemberships: Option[Seq[LoyaltyMembership]],
    billingDetails: Option[BillingDetails],
    source: CustomerSource,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.Customer
}

final case class CustomerMerchantUpsertion(
    firstName: ResettableString = None,
    lastName: ResettableString = None,
    dob: ResettableLocalDate = None,
    anniversary: ResettableLocalDate = None,
    email: ResettableString = None,
    phoneNumber: ResettableString = None,
    address: AddressSync = AddressSync.empty,
    enrollInLoyaltyProgramId: Option[UUID] = None,
    billingDetails: ResettableBillingDetails = None,
  )
