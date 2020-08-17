package io.paytouch.core.conversions

import cats.implicits._

import org.scalacheck.ScalacheckShapeless._

import io.paytouch.core.entities._
import io.paytouch.core.services._
import io.paytouch.core.utils._

final class MerchantConversionsSuite extends PaytouchSuite with MerchantConversions {
  def userService: UserService = ???
  def locationService: LocationService = ???

  "legalCountry" should {
    "yield 'United States' if legalDetails is empty" in {
      shouldBeUS(legalCountry(None))
    }

    "yield 'United States' if legalDetails.address is empty" in {
      shouldBeUS(
        legalCountry(
          LegalDetails(
            businessName = None,
            vatId = None,
            address = None,
            invoicingCode = None,
          ).some,
        ),
      )
    }

    "yield 'United States' if legalDetails.address.country is empty" in {
      shouldBeUS(
        legalCountry(
          LegalDetails(
            businessName = None,
            vatId = None,
            address = AddressImproved
              .empty
              .copy(
                countryData = None,
              )
              .some,
            invoicingCode = None,
          ).some,
        ),
      )
    }

    "take the country from legalDetails.address.country" in {
      prop { country: Country =>
        val actual: Country =
          legalCountry(
            LegalDetails(
              businessName = None,
              vatId = None,
              address = AddressImproved
                .empty
                .copy(
                  countryData = country.some,
                )
                .some,
              invoicingCode = None,
            ).some,
          )

        actual ==== country
      }
    }
  }

  private def shouldBeUS(actual: Country): MatchResult[Country] =
    actual ==== UtilService.Geo.UnitedStates
}
