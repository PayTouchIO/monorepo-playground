package io.paytouch.core.conversions

import io.paytouch._
import io.paytouch.core.data.model._
import io.paytouch.core.entities._
import io.paytouch.core.services.UtilService
import io.paytouch.core.validators.RecoveredDeliveryAddressUpsertion

trait OrderDeliveryAddressConversions {
  def fromRecordsToEntities(records: Seq[OrderDeliveryAddressRecord]): Seq[DeliveryAddress] =
    records.map(fromRecordToEntity)

  def fromRecordToEntity(record: OrderDeliveryAddressRecord) =
    DeliveryAddress(
      id = record.id,
      firstName = record.firstName,
      lastName = record.lastName,
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
      drivingDistanceInMeters = record.drivingDistanceInMeters,
      estimatedDrivingTimeInMins = record.estimatedDrivingTimeInMins,
    )

  protected def toUpdate(deliveryAddress: RecoveredDeliveryAddressUpsertion)(implicit user: UserContext) =
    OrderDeliveryAddressUpdate(
      id = Some(deliveryAddress.id),
      merchantId = Some(user.merchantId),
      firstName = deliveryAddress.firstName,
      lastName = deliveryAddress.lastName,
      addressLine1 = deliveryAddress.address.line1,
      addressLine2 = deliveryAddress.address.line2,
      city = deliveryAddress.address.city,
      state = deliveryAddress.address.state,
      country = deliveryAddress.address.country,
      stateCode = deliveryAddress.address.stateCode,
      countryCode = deliveryAddress.address.countryCode,
      postalCode = deliveryAddress.address.postalCode,
      drivingDistanceInMeters = deliveryAddress.drivingDistanceInMeters,
      estimatedDrivingTimeInMins = deliveryAddress.estimatedDrivingTimeInMins,
    )
}
