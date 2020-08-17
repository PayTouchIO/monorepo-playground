package io.paytouch.core.validators

import cats.implicits._

import org.scalacheck._
import org.scalacheck.ScalacheckShapeless._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.entities._
import io.paytouch.core.errors.InvalidAddress
import io.paytouch.core.services.UtilService
import io.paytouch.core.utils.PaytouchSuite

final class AddressValidatorSuite extends PaytouchSuite {
  import AddressValidator._
  import AddressValidatorSuite._

  implicit protected val noShrink: Shrink[Int] =
    Shrink.shrinkAny

  "AddressValidator.validated" should {
    "for AddressUpsertion" in {
      "codes" in {
        "yield BAD" in {
          "if country and state CODES are NOT submitted together (only countryCode is set)" in {
            prop { countryName: CountryName =>
              assert(
                inputCountryCode = UtilService.Geo.UnitedStates.code.some.map(CountryCode),
                inputCountryName = countryName.some,
                expected = InvalidAddress.CountryAndStateCodesMustBeSubmittedTogether.bad,
              )
            }
          }

          "if country and state CODES are NOT submitted together (only stateCode is set)" in {
            assert(
              inputStateCode = "CA".some.map(StateCode),
              expected = InvalidAddress.CountryAndStateCodesMustBeSubmittedTogether.bad,
            )
          }

          "if country and state CODES are submitted together but the state of a country with SUPPORTED states doesn't exist" in {
            prop { (countryWithSupportedStates: Country, countryName: CountryName) =>
              assert(
                inputCountryCode = countryWithSupportedStates.code.some.map(CountryCode),
                inputCountryName = countryName.some,
                inputStateCode = "does not exist".some.map(StateCode),
                expected = InvalidAddress.InvalidState(StateCode("does not exist")).bad,
              )
            }.setGen1(LocalGens.CountriesWithSupportedStates)
          }

          "if input is entirely EMPTY" in {
            assert(
              expected = (None, None, None, None).good,
            )
          }
        }

        "yield GOOD" in {
          "if country and state CODES are submitted together while setting both countryName and stateName" in {
            prop { (data: (Country, State), countryName: CountryName) =>
              val (supportedCountry, supportedState) = data

              assert(
                inputCountryCode = supportedCountry.code.some.map(CountryCode),
                inputCountryName = countryName.some,
                inputStateCode = supportedState.code.some.map(StateCode),
                expected = (
                  supportedCountry.code.some.map(CountryCode),
                  supportedState.code.some.map(StateCode),
                  supportedCountry.name.some.map(CountryName),
                  supportedState.name.some.map(StateName),
                ).good,
              )
            }.setGen1(LocalGens.CountriesWithSupportedStatesTupledWithStates)
          }

          "if country and state CODES are submitted together but the stateCode does NOT exist should yield GOOD while recovering (overwriting) the input countryName for countries with UNSUPPORTED states and trimming the input stateName" in {
            prop { (countryWithUnsupportedStates: Country, countryName: CountryName, stateName: Option[StateName]) =>
              assert(
                inputCountryCode = countryWithUnsupportedStates.code.some.map(CountryCode),
                inputCountryName = countryName.some,
                inputStateCode = "does not exist".some.map(StateCode),
                inputStateName = stateName,
                expected = (
                  countryWithUnsupportedStates.code.some.map(CountryCode),
                  "does not exist".some.map(StateCode),
                  countryWithUnsupportedStates.name.some.map(CountryName),
                  stateName.map(_.map(_.trim)),
                ).good,
              )
            }.setGen1(LocalGens.CountriesWithUnsupportedStates)
          }
        }
      }

      "names" in {
        "yield BAD" in {
          "if country and state NAMES are NOT submitted together (only countryName is set)" in {
            assert(
              inputCountryName = UtilService.Geo.UnitedStates.name.some.map(CountryName),
              expected = InvalidAddress.CountryAndStateNamesMustBeSubmittedTogether.bad,
              runForAddress = false,
              runForAddressImproved = false,
              runForAddressImprovedUpsertion = false,
            )
          }

          "if country and state NAMES are NOT submitted together (only stateName is set)" in {
            assert(
              inputStateName = "California".some.map(StateName),
              expected = InvalidAddress.CountryAndStateNamesMustBeSubmittedTogether.bad,
              runForAddress = false,
              runForAddressImproved = false,
              runForAddressImprovedUpsertion = false,
            )
          }
        }

        "yield GOOD" in {
          "if country and state NAMES are submitted together while setting both countryCode and stateCode for countries with SUPPORTED states" in {
            prop { data: (Country, State) =>
              val (supportedCountry, supportedState) = data

              assert(
                inputCountryName = supportedCountry.name.some.map(CountryName),
                inputStateName = supportedState.name.some.map(StateName),
                expected = (
                  supportedCountry.code.some.map(CountryCode),
                  supportedState.code.some.map(StateCode),
                  supportedCountry.name.some.map(CountryName),
                  supportedState.name.some.map(StateName),
                ).good,
                runForAddress = false,
                runForAddressImproved = false,
                runForAddressImprovedUpsertion = false,
              )
            }.setGen(LocalGens.CountriesWithSupportedStatesTupledWithStates)
          }

          "if country and state NAMES are submitted together for 'us' and 'cali' while setting all the fields (both codes and names)" in {
            assert(
              inputCountryName = "us".some.map(CountryName),
              inputStateName = "cali".some.map(StateName),
              expected = (
                UtilService.Geo.UnitedStates.code.some.map(CountryCode),
                "CA".some.map(StateCode),
                UtilService.Geo.UnitedStates.name.some.map(CountryName),
                "California".some.map(StateName),
              ).good,
              runForAddress = false,
              runForAddressImproved = false,
              runForAddressImprovedUpsertion = false,
            )
          }

          "if country and state NAMES are submitted together for a country with UNSUPPORTED states WIHTHOUT setting the codes (only names are set) the state name should be used as code!" in {
            prop { countryWithUnsupportedStates: Country =>
              assert(
                inputCountryName = countryWithUnsupportedStates.name.some.map(CountryName),
                inputStateName = "  some state  ".some.map(StateName),
                expected = (
                  countryWithUnsupportedStates.code.some.map(CountryCode),
                  "some state".some.map(StateCode), // using name as code but only for countries with UNSUPPORTED states
                  countryWithUnsupportedStates.name.some.map(CountryName),
                  "some state".some.map(StateName),
                ).good,
                runForAddress = false,
                runForAddressImproved = false,
                runForAddressImprovedUpsertion = false,
              )
            }.setGen(LocalGens.CountriesWithUnsupportedStates)
          }
        }
      }
    }
  }

  private[this] def assert(
      inputCountryCode: Option[CountryCode] = None,
      inputStateCode: Option[StateCode] = None,
      inputCountryName: Option[CountryName] = None,
      inputStateName: Option[StateName] = None,
      expected: AddressValidator.MultipleErrorsOr[
        (Option[CountryCode], Option[StateCode], Option[CountryName], Option[StateName]),
      ],
      runForAddress: Boolean = true,
      runForAddressUpsertion: Boolean = true,
      runForAddressImproved: Boolean = true,
      runForAddressImprovedUpsertion: Boolean = true,
    ) = {
    val inputCountry = (
      inputCountryCode.map(_.value),
      inputCountryName.map(_.value),
    ).mapN(Country)

    if (runForAddress)
      AddressValidator
        .validated {
          Address
            .empty
            .copy(
              countryData = inputCountry,
              stateData = (
                inputStateName.map(_.value).some,
                inputStateCode.map(_.value),
                inputCountry.some,
              ).mapN(AddressState),
              country = inputCountryName.map(_.value),
              state = inputStateName.map(_.value),
            )
        } ==== expected.map {
        case (expectedCountryCode, expectedStateCode, expectedCountryName, expectedStateName) =>
          val expectedCountry = (expectedCountryCode.map(_.value), expectedCountryName.map(_.value)).mapN(Country)

          Address
            .empty
            .copy(
              countryData = expectedCountry,
              stateData = (
                expectedStateName.map(_.value).some,
                expectedStateCode.map(_.value),
                expectedCountry.some,
              ).mapN(AddressState),
              country = expectedCountryName.map(_.value),
              state = expectedStateName.map(_.value),
            )
      }

    if (runForAddressUpsertion)
      AddressValidator
        .validated {
          AddressUpsertion
            .empty
            .copy(
              countryCode = inputCountryCode.map(_.value),
              stateCode = inputStateCode.map(_.value),
              country = inputCountryName.map(_.value),
              state = inputStateName.map(_.value),
            )
        } ==== expected.map {
        case (expectedCountryCode, expectedStateCode, expectedCountryName, expectedStateName) =>
          AddressUpsertion
            .empty
            .copy(
              countryCode = expectedCountryCode.map(_.value),
              stateCode = expectedStateCode.map(_.value),
              country = expectedCountryName.map(_.value),
              state = expectedStateName.map(_.value),
            )
      }

    // sync does not need it's own flag
    if (runForAddressUpsertion)
      AddressValidator
        .validated {
          AddressSync
            .empty
            .copy(
              countryCode = inputCountryCode.map(_.value),
              stateCode = inputStateCode.map(_.value),
              country = inputCountryName.map(_.value),
              state = inputStateName.map(_.value),
            )
        } ==== expected.map {
        case (expectedCountryCode, expectedStateCode, expectedCountryName, expectedStateName) =>
          AddressSync
            .empty
            .copy(
              countryCode = expectedCountryCode.map(_.value),
              stateCode = expectedStateCode.map(_.value),
              country = expectedCountryName.map(_.value),
              state = expectedStateName.map(_.value),
            )
      }

    if (runForAddressImproved)
      AddressValidator
        .validated {
          AddressImproved
            .empty
            .copy(
              countryData = inputCountry,
              stateData = (
                inputStateName.map(_.value).some,
                inputStateCode.map(_.value),
                inputCountry.some,
              ).mapN(AddressState),
            )
        } ==== expected.map {
        case (expectedCountryCode, expectedStateCode, expectedCountryName, expectedStateName) =>
          val expectedCountry = (expectedCountryCode.map(_.value), expectedCountryName.map(_.value)).mapN(Country)

          AddressImproved
            .empty
            .copy(
              countryData = expectedCountry,
              stateData = (
                expectedStateName.map(_.value).some,
                expectedStateCode.map(_.value),
                expectedCountry.some,
              ).mapN(AddressState),
            )
      }

    if (runForAddressImprovedUpsertion)
      AddressValidator
        .validated {
          AddressImprovedUpsertion
            .empty
            .copy(
              countryCode = inputCountryCode.map(_.value),
              stateCode = inputStateCode.map(_.value),
            )
        } ==== expected.map {
        case (expectedCountryCode, expectedStateCode, expectedCountryName, expectedStateName) =>
          AddressImprovedUpsertion
            .empty
            .copy(
              countryCode = expectedCountryCode.map(_.value),
              stateCode = expectedStateCode.map(_.value),
            )
      }

    // sync does not need it's own flag
    if (runForAddressImprovedUpsertion)
      AddressValidator
        .validated {
          AddressImprovedSync
            .empty
            .copy(
              countryCode = inputCountryCode.map(_.value),
              stateCode = inputStateCode.map(_.value),
            )
        } ==== expected.map {
        case (expectedCountryCode, expectedStateCode, expectedCountryName, expectedStateName) =>
          AddressImprovedSync
            .empty
            .copy(
              countryCode = expectedCountryCode.map(_.value),
              stateCode = expectedStateCode.map(_.value),
            )
      }

    success // that's just for Specs2 to shut up
  }
}

object AddressValidatorSuite {
  private object LocalGens {
    val CountriesWithSupportedStates: Gen[Country] =
      Gen.oneOf(UtilService.Geo.countriesWithSupportedStates)

    val CountriesWithUnsupportedStates: Gen[Country] =
      Gen.oneOf(UtilService.Geo.countriesWithUnsupportedStates)

    val CountriesWithSupportedStatesTupledWithStates: Gen[(Country, State)] =
      for {
        country <- CountriesWithSupportedStates
        state <- supportedStates(country)
      } yield country -> state

    def supportedStates(country: Country): Gen[State] =
      Gen
        .oneOf(
          UtilService.Geo.supportedStates(CountryCode(country.code)),
        )
        .flatMap(Gen.oneOf(_))
  }
}
