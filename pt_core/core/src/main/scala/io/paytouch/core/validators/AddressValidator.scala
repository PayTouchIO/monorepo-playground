package io.paytouch.core.validators

import cats.implicits._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.entities._
import io.paytouch.core.errors.InvalidAddress
import io.paytouch.core.services.UtilService
import io.paytouch.core.utils.DependentErrorHandling

object AddressValidator extends DependentErrorHandling[InvalidAddress] {
  def validated[WeaklyTyped](
      address: WeaklyTyped,
    )(implicit
      iso: Address.LossyIso[WeaklyTyped],
    ): MultipleErrorsOr[WeaklyTyped] =
    address
      .pipe(iso.weakToStrong)
      .pipe(validatedCodes)
      .map(iso.strongToWeak)

  private def validatedCodes[WeaklyTyped](
      stronglyTyped: Address.LossyIso.StronglyTyped[WeaklyTyped],
    ): MultipleErrorsOr[Address.LossyIso.StronglyTyped[WeaklyTyped]] =
    (stronglyTyped.countryCode, stronglyTyped.stateCode) match {
      case (Some(countryCode), Some(stateCode)) =>
        for {
          country <- resolved(countryCode)
          state <- resolved(CountryCode(country.code), fallbackStateName = stronglyTyped.stateName, stateCode)
        } yield stronglyTyped.withDataFrom(country, state)

      case (Some(_), None) | (None, Some(_)) =>
        InvalidAddress.CountryAndStateCodesMustBeSubmittedTogether.bad

      case _ =>
        validatedNames(stronglyTyped)
    }

  private def validatedNames[WeaklyTyped](
      stronglyTyped: Address.LossyIso.StronglyTyped[WeaklyTyped],
    ): MultipleErrorsOr[Address.LossyIso.StronglyTyped[WeaklyTyped]] =
    (stronglyTyped.countryName, stronglyTyped.stateName) match {
      case (Some(countryName), Some(stateName)) =>
        for {
          country <- resolved(countryName)
          state <- resolved(CountryName(country.name), stateName)
        } yield stronglyTyped.withDataFrom(country, state.toAddressState)

      case (Some(_), None) | (None, Some(_)) =>
        InvalidAddress.CountryAndStateNamesMustBeSubmittedTogether.bad

      case _ =>
        stronglyTyped.good // we should change this to bad eventually
    }

  def resolved(country: CountryCode): MultipleErrorsOr[Country] =
    UtilService
      .Geo
      .country(country)
      .goodOrBad(InvalidAddress.InvalidCountry(country))

  def resolved(
      country: CountryCode,
      fallbackStateName: Option[StateName],
      state: StateCode,
    ): MultipleErrorsOr[AddressState] =
    UtilService
      .Geo
      .addressState(country, fallbackStateName)(state)
      .goodOrBad(InvalidAddress.InvalidState(state))

  def resolved(country: CountryName): MultipleErrorsOr[Country] =
    UtilService
      .Geo
      .country(country)
      .goodOrBad(InvalidAddress.InvalidCountry(country))

  def resolved(country: CountryName, state: StateName): MultipleErrorsOr[State] =
    UtilService
      .Geo
      .state(country)(state)
      .goodOrBad(InvalidAddress.InvalidState(state))
}
