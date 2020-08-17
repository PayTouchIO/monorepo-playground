package io.paytouch.core.conversions

import java.util.UUID

import scala.concurrent._

import io.paytouch._
import io.paytouch.core.data.model._
import io.paytouch.core.entities._
import io.paytouch.core.services.UtilService

trait GlobalCustomerConversions extends EntityConversion[GlobalCustomerRecord, GlobalCustomer] {
  def fromRecordToEntity(record: GlobalCustomerRecord)(implicit user: UserContext): GlobalCustomer =
    GlobalCustomer(
      id = record.id,
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
      mobileStorefrontLastLogin = record.mobileStorefrontLastLogin,
      webStorefrontLastLogin = record.webStorefrontLastLogin,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )

  def toGlobalCustomerUpdate(id: UUID, email: Option[String]): GlobalCustomerUpdate =
    GlobalCustomerUpdate(
      id = Some(id),
      firstName = None,
      lastName = None,
      dob = None,
      anniversary = None,
      email = email,
      phoneNumber = None,
      addressLine1 = None,
      addressLine2 = None,
      city = None,
      state = None,
      country = None,
      stateCode = None,
      countryCode = None,
      postalCode = None,
      mobileStorefrontLastLogin = None,
      webStorefrontLastLogin = None,
    )

  implicit def toFutureMapOfMonetaryAmounts(
      f: Future[Map[UUID, Seq[BigDecimal]]],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[MonetaryAmount]]] =
    f.map(_.transform((_, v) => v.map(MonetaryAmount(_))))
}
