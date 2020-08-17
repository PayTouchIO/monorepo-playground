package io.paytouch.core.errors

import java.util.UUID

import io.paytouch._
import io.paytouch.core.entities._

sealed abstract class InvalidAddress(override val message: String) extends DataError

object InvalidAddress {
  case object NeitherCodesNorNamesAreSpecified
      extends InvalidAddress(
        message = s"Neither codes nor names are specified.",
      )

  final case class InvalidCountryCode(countryCode: CountryCode)
      extends InvalidAddress(
        message = s"${countryCode.value} is not a valid country code.",
      )

  final case class InvalidStateCode(stateCode: StateCode)
      extends InvalidAddress(
        message = s"${stateCode.value} is not a valid state code.",
      )

  final case class InvalidCountryName(countryName: CountryName)
      extends InvalidAddress(
        message = s"${countryName.value} is not a valid country name.",
      )

  final case class InvalidStateName(stateName: StateName)
      extends InvalidAddress(
        message = s"${stateName.value} is not a valid state name.",
      )

  case object CountryAndStateCodesMustBeSubmittedTogether
      extends InvalidAddress(
        message = "Country and state codes must be submitted together.",
      )

  final case class InvalidCountry(country: Country)
      extends InvalidAddress(
        message = s"$country is not a valid country.",
      )

  case object InvalidCountry extends (Country => InvalidCountry) {
    def apply(country: CountryCode): InvalidAddress =
      InvalidCountryCode(country)

    def apply(country: CountryName): InvalidAddress =
      InvalidCountryName(country)
  }

  final case class InvalidState(state: AddressState)
      extends InvalidAddress(
        message = s"$state is not a valid state.",
      )

  case object InvalidState extends (AddressState => InvalidState) {
    def apply(state: StateCode): InvalidAddress =
      InvalidStateCode(state)

    def apply(state: StateName): InvalidAddress =
      InvalidStateName(state)
  }

  case object CountryAndStateNamesMustBeSubmittedTogether
      extends InvalidAddress(
        message = "Country and state names must be submitted together.",
      )
}
