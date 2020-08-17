package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.data._
import io.paytouch.core.data.model._
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.{ ContextSource, CustomerSource }
import io.paytouch.core.services.UtilService
import io.paytouch.core.utils.PaytouchLogger

trait CustomerMerchantConversions extends EntityConversionMerchantContext[CustomerMerchantRecord, CustomerMerchant] {
  val logger: PaytouchLogger

  def fromRecordToEntity(
      record: CustomerMerchantRecord,
    )(implicit
      merchant: MerchantContext,
    ): CustomerMerchant =
    fromRecordAndOptionsToEntity(record, None, None, None, None, None, None, None, None)

  def fromRecordsAndOptionsToEntities(
      records: Seq[CustomerMerchantRecord],
      customers: Seq[GlobalCustomer],
      totalVisitsPerCustomer: Option[Map[UUID, Int]],
      totalSpendPerCustomer: Option[Map[UUID, MonetaryAmount]],
      locationsPerCustomer: Option[Map[UUID, Seq[Location]]],
      loyaltyProgramsPerCustomer: Option[Map[UUID, Seq[LoyaltyProgram]]],
      loyaltyMembershipsPerCustomer: Option[Map[UUID, Seq[LoyaltyMembership]]],
      avgTipsPerCustomer: Option[Map[UUID, Seq[MonetaryAmount]]],
      billingDetailsPerCustomer: Option[Map[UUID, BillingDetails]],
    )(implicit
      merchant: MerchantContext,
    ) =
    records.map { record =>
      fromRecordAndOptionsToEntity(
        record,
        customers.find(_.id == record.customerId),
        totalVisitsPerCustomer,
        totalSpendPerCustomer,
        avgTipsPerCustomer,
        locationsPerCustomer,
        loyaltyProgramsPerCustomer,
        loyaltyMembershipsPerCustomer,
        billingDetailsPerCustomer,
      )
    }

  def fromRecordAndOptionsToEntity(
      record: CustomerMerchantRecord,
      customer: Option[GlobalCustomer],
      totalVisitsPerCustomer: Option[Map[UUID, Int]],
      totalSpendPerCustomer: Option[Map[UUID, MonetaryAmount]],
      avgTipsPerCustomer: Option[Map[UUID, Seq[MonetaryAmount]]],
      locationsPerCustomer: Option[Map[UUID, Seq[Location]]],
      loyaltyProgramsPerCustomer: Option[Map[UUID, Seq[LoyaltyProgram]]],
      loyaltyMembershipsPerCustomer: Option[Map[UUID, Seq[LoyaltyMembership]]],
      billingDetailsPerCustomer: Option[Map[UUID, BillingDetails]],
    )(implicit
      merchant: MerchantContext,
    ) =
    CustomerMerchant(
      id = record.id, // note that record.id == record.customerId
      firstName = record.firstName,
      lastName = record.lastName,
      dob = record.dob,
      anniversary = record.anniversary,
      email = record.email,
      phoneNumber = record.phoneNumber,
      address = Address(
        line1 = record.addressLine1,
        line2 = record.addressLine2,
        city = record.city,
        state = record.state,
        country = record.country,
        stateData = UtilService
          .Geo
          .addressState(
            record.countryCode.map(CountryCode),
            record.stateCode.map(StateCode),
            record.country.map(CountryName),
            record.state.map(StateName),
          ),
        countryData = UtilService
          .Geo
          .country(
            record.countryCode.map(CountryCode),
            record.country.map(CountryName),
          ),
        postalCode = record.postalCode,
      ),
      totalVisits = totalVisitsPerCustomer.map(_.getOrElse(record.customerId, 0)),
      totalSpend = totalSpendPerCustomer.map(totalSpend =>
        Seq(totalSpend.getOrElse(record.customerId, MonetaryAmount(0, merchant))),
      ),
      avgTips = avgTipsPerCustomer.map(_.getOrElse(record.customerId, Seq.empty)),
      mobileStorefrontLastLogin = customer.flatMap(_.webStorefrontLastLogin),
      webStorefrontLastLogin = customer.flatMap(_.mobileStorefrontLastLogin),
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
      locations = locationsPerCustomer.map(_.getOrElse(record.customerId, Seq.empty)),
      loyaltyPrograms = loyaltyProgramsPerCustomer.map(_.getOrElse(record.customerId, Seq.empty)),
      loyaltyStatuses = loyaltyMembershipsPerCustomer.map(_.getOrElse(record.customerId, Seq.empty)),
      loyaltyMemberships = loyaltyMembershipsPerCustomer.map(_.getOrElse(record.customerId, Seq.empty)),
      billingDetails = billingDetailsPerCustomer.map(_.getOrElse(record.customerId, BillingDetails.empty)),
      source = record.source,
    )

  def groupCustomerMerchantsPerGroup(
      customerGroups: Seq[CustomerGroupRecord],
      customerMerchants: Seq[CustomerMerchant],
    ): Map[UUID, Seq[CustomerMerchant]] =
    customerGroups.groupBy(_.groupId).transform { (_, customerGrps) =>
      customerGrps.flatMap { customerGroup =>
        customerMerchants.find(_.id == customerGroup.customerId)
      }
    }

  def toCustomerMerchantUpdate(
      update: CustomerMerchantUpsertion,
      globalCustomer: GlobalCustomerUpdate,
      source: CustomerSource,
    )(implicit
      merchant: MerchantContext,
    ): model.CustomerMerchantUpdate =
    model
      .CustomerMerchantUpdate(
        merchantId = Some(merchant.id),
        customerId = globalCustomer.id,
        firstName = update.firstName,
        lastName = update.lastName,
        dob = update.dob,
        anniversary = update.anniversary,
        email = update.email,
        phoneNumber = update.phoneNumber,
        addressLine1 = update.address.line1,
        addressLine2 = update.address.line2,
        city = update.address.city,
        state = update.address.state,
        country = update.address.country,
        stateCode = update.address.stateCode,
        countryCode = update.address.countryCode,
        postalCode = update.address.postalCode,
        billingDetails = update.billingDetails,
        source = Some(source),
      )

  def customerSourceFromContextSource(contextSource: ContextSource): CustomerSource =
    contextSource match {
      case ContextSource.PtDashboard => CustomerSource.PtDashboard
      case ContextSource.PtRegister  => CustomerSource.PtRegister
      case _ =>
        logger.recoverLog(
          s"No mapping setup for context source to customer source. Recovering with PtDashboard.",
          contextSource,
        )
        CustomerSource.PtDashboard
    }
}
