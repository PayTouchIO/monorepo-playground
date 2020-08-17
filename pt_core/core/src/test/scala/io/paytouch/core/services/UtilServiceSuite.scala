package io.paytouch.core.services

import cats.implicits._

import org.scalacheck._
import org.scalacheck.ScalacheckShapeless._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.entities._
import io.paytouch.core.utils.PaytouchSuite

final class UtilServiceSuite extends PaytouchSuite {
  import UtilServiceSuite._

  "UtilService.Geo.country" should {
    "not find a country by an empty name" in {
      UtilService.Geo.country(CountryName("")) ==== None
    }

    "find countries by full name" in {
      prop { country: Country => UtilService.Geo.country(CountryName(country.name)) ==== country.some }
        .setGen(ValidCountries)
    }

    "find countries even if code is used as a name" in {
      prop { country: Country => UtilService.Geo.country(CountryName(country.code)) ==== country.some }
        .setGen(ValidCountries)
    }

    "find US by typical names including full name and code case insensitively with trimming" in {
      mangled(Seq("United States of America", "United States", "US", "USA")).map { countryName: String =>
        UtilService.Geo.country(CountryName(countryName)) ==== UtilService.Geo.UnitedStates.some
      }
    }

    "find Russia by typical names including full name and code case insensitively with trimming" in {
      mangled(Seq("Russia")).map { countryName: String =>
        val Russia = Country(code = "RU", name = "Russian Federation")

        UtilService.Geo.country(CountryName(countryName)) ==== Russia.some
      }
    }
  }

  "UtilService.Geo.state" should {
    "not find a state if the country cannot be found" in {
      val countryName = CountryName("")

      UtilService.Geo.country(countryName) ==== None // sanity check

      UtilService.Geo.state(countryName)(StateName("California")) ==== None
    }

    "not find a state if the country is supported but the state name is empty" in {
      val country = UtilService.Geo.UnitedStates
      val countryName = CountryName(country.name)

      UtilService.Geo.country(countryName) ==== country.some // sanity check

      UtilService.Geo.state(countryName)(StateName("")) ==== None
    }

    "find US/California by typical names including full name and code case insensitively with trimming" in {
      val country = UtilService.Geo.UnitedStates
      val countryName = CountryName(country.name)

      UtilService.Geo.country(countryName) ==== country.some // sanity check

      val state = State(code = "CA", name = "California", country = country.some)

      mangled(Seq("CA", "California", "Cali")).map { stateName =>
        UtilService.Geo.state(countryName)(StateName(stateName)) ==== state.some
      }
    }

    "find US/California by typical names including full name and code case insensitively with trimming" in {
      val Russia = Country(code = "RU", name = "Russian Federation")
      val countryName = CountryName(Russia.name)

      UtilService.Geo.country(countryName) ==== Russia.some // sanity check

      def state(name: String) = State(code = "MiracleCode", name = name, country = Russia.some)

      mangled(Seq("MiracleName")).map { stateName =>
        UtilService.Geo.state(countryName)(StateName(stateName)) ==== state(stateName.trim).some
      }
    }
  }
}

object UtilServiceSuite {
  private val ValidCountries: Gen[Country] =
    Gen.oneOf(UtilService.Geo.countries)

  private def mangled(in: Seq[String]): Seq[String] =
    in.zipWithIndex
      .map(_.map(_ + 1)) // 1 based index
      .flatMap {
        case (name, index) =>
          val spaces = " " * index

          val capital = name.capitalize
          val lower = name.toLowerCase
          val upper = name.toUpperCase

          Seq(
            name,
            capital,
            lower,
            upper,
            s"$spaces$name$spaces",
            s"$spaces$capital$spaces",
            s"$spaces$lower$spaces",
            s"$spaces$upper$spaces",
          )
      }
}
