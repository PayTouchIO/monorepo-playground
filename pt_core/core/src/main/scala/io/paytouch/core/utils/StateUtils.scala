package io.paytouch.core.utils

import cats._
import cats.implicits._

import io.bartholomews.iso_country.CountryCodeAlpha2

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.entities._

trait StateUtils {
  final val UnitedStates: Country =
    toCountry(CountryCodeAlpha2.UNITED_STATES_OF_AMERICA)

  final val Canada: Country =
    toCountry(CountryCodeAlpha2.CANADA)

  final private def toCountry(country: CountryCodeAlpha2): Country =
    Country(code = country.value, name = country.name)

  final val countries: Seq[Country] =
    CountryCodeAlpha2.values.map(toCountry)

  final val countriesWithSupportedStates: Seq[Country] =
    countries.filter(country => supportedStates(CountryCode(country.code)).nonEmpty)

  final val countriesWithUnsupportedStates: Seq[Country] =
    countries.filterNot { country =>
      countriesWithSupportedStates.exists { countryWithSupportedStates =>
        country.code === countryWithSupportedStates.code
      }
    }

  final def country(countryCode: Option[CountryCode], countryName: => Option[CountryName]): Option[Country] =
    countryCode.flatMap(country) orElse countryName.flatMap(country)

  final def addressState(
      countryCode: Option[CountryCode],
      stateCode: Option[StateCode],
      countryName: => Option[CountryName],
      stateName: Option[StateName],
    ): Option[AddressState] =
    (for { // lookup by codes
      countryCode <- countryCode
      stateCode <- stateCode
      state <- addressState(countryCode, fallbackStateName = stateName)(stateCode)
    } yield state) orElse {
      for { // lookup by names
        countryName <- countryName
        stateName <- stateName
        state <- state(countryName)(stateName)
      } yield state.toAddressState
    }

  final def country(countryCode: CountryCode): Option[Country] =
    countries.find(_.code.pipe(CountryCode) === countryCode)

  private[this] def belongsToCountryWithSupportedStates(countryCode: CountryCode): Boolean =
    countriesWithSupportedStates.exists(_.code.pipe(CountryCode) === countryCode)

  private[this] def belongsToCountryWithoutSupportedStates(countryCode: CountryCode): Boolean =
    !belongsToCountryWithSupportedStates(countryCode)

  final def states(countryCode: CountryCode): Seq[State] =
    supportedStates(countryCode).toSeq.flatten

  final def supportedStates(countryCode: CountryCode): Option[Seq[State]] =
    supportedStatesPartial.get(countryCode.map(_.toUpperCase))

  private[this] lazy val supportedStatesPartial: Map[CountryCode, Seq[State]] =
    Map(
      UnitedStates.code -> UnitedStatesStates,
      Canada.code -> CanadaStates,
    ).map(_.leftMap(CountryCode))

  final def state(countryCode: CountryCode)(stateCode: StateCode): Option[State] =
    states(countryCode).find(_.code.pipe(StateCode) === stateCode)

  final def supportedState(countryCode: CountryCode)(stateCode: StateCode): Option[State] =
    supportedStates(countryCode).flatMap(_.find(_.code.pipe(StateCode) === stateCode))

  final def addressState(
      countryCode: CountryCode,
      fallbackStateName: => Option[StateName],
    )(
      stateCode: StateCode,
    ): Option[AddressState] =
    if (belongsToCountryWithSupportedStates(countryCode))
      state(countryCode)(stateCode).map(_.toAddressState)
    else
      AddressState(
        name = fallbackStateName.map(_.value.trim),
        code = stateCode.value,
        country = country(countryCode),
      ).some

  final def country(countryName: CountryName): Option[Country] =
    code(countryName).flatMap(country)

  /**
    * Uses stateName as stateCode for countries with unsupported states.
    */
  final def state(countryName: CountryName)(stateName: StateName): Option[State] =
    code(countryName).flatMap { countryCode =>
      addressState(countryCode, fallbackStateName = stateName.some)(
        stateCode = code(countryCode)(stateName)
          .getOrElse(stateName.value.trim.pipe(StateCode)), // using stateCode as stateName
      ).map(_.toState(fallbackName = stateName.value.trim))
    }

  private[this] def code(countryName: CountryName): Option[CountryCode] =
    CountryNameLookupTable
      .collectFirst {
        case (name, code) if name.value ~= countryName.value => code
      }

  final implicit private[this] class LocalStringOps(private val self: String) {
    @inline def ~=(other: String): Boolean =
      self.trim equalsIgnoreCase other.trim
  }

  private[this] def code(countryCode: CountryCode)(stateName: StateName): Option[StateCode] =
    StateNameLookupTable(countryCode).flatMap {
      _.collectFirst {
        case (name, code) if name.value ~= stateName.value => code
      }
    }

  private[this] val CountryNameLookupTable: Map[CountryName, CountryCode] =
    CountryNamesFoundInDb.combine(DefaultCountryNameLookupTable).flatMap {
      case (countryCode, countryNames) =>
        countryNames.map(_ -> countryCode)
    }

  private[this] lazy val CountryNamesFoundInDb: Map[CountryCode, Set[CountryName]] =
    Map(
      CountryCodeAlpha2.RUSSIAN_FEDERATION.value -> Set(
        "Russia",
      ),
      CountryCodeAlpha2.UNITED_STATES_OF_AMERICA.value -> Set(
        "United States",
        "USA",
      ),
    ).map(liftedCountry)

  private[this] lazy val DefaultCountryNameLookupTable: Map[CountryCode, Set[CountryName]] =
    countries
      .map(country => country.code -> Set(country.code, country.name))
      .map(liftedCountry)
      .toMap

  private[this] def liftedCountry(raw: (String, Set[String])): (CountryCode, Set[CountryName]) =
    raw.bimap(CountryCode, _.map(CountryName))

  private[this] val StateNameLookupTable: CountryCode => Option[Map[StateName, StateCode]] =
    StateNamesFoundInDb
      .combine(DefaultStateNameLookupTable)
      .transform {
        case (_, v) =>
          v.flatMap {
            case (stateCode, stateNames) =>
              stateNames.map(_ -> stateCode)
          }
      }
      .get

  private[this] lazy val StateNamesFoundInDb: Map[CountryCode, Map[StateCode, Set[StateName]]] =
    Map(
      CountryCodeAlpha2.RUSSIAN_FEDERATION.value ->
        Map(
          "MiracleCode" -> Set("MiracleName"),
        ),
      CountryCodeAlpha2.UNITED_STATES_OF_AMERICA.value ->
        Map(
          "CA" -> Set("cali"),
        ),
    ).map(_.bimap(CountryCode, _.map(liftedState)))

  private[this] lazy val DefaultStateNameLookupTable: Map[CountryCode, Map[StateCode, Set[StateName]]] =
    supportedStatesPartial.map {
      case (countryCode, states) =>
        countryCode -> states
          .map(state => state.code -> Set(state.code, state.name))
          .map(liftedState)
          .toMap
    }

  private[this] def liftedState(raw: (String, Set[String])): (StateCode, Set[StateName]) =
    raw.bimap(StateCode, _.map(StateName))

  final lazy val CanadaStates: Seq[State] = {
    def state(name: String, code: String): State =
      State(name, code, Canada.some)

    Seq(
      state(name = "Alberta", code = "AB"),
      state(name = "British Columbia", code = "BC"),
      state(name = "Manitoba", code = "MB"),
      state(name = "New Brunswick", code = "NB"),
      state(name = "Newfoundland and Labrador", code = "NF"),
      state(name = "Northwest Territories", code = "NT"),
      state(name = "Nova Scotia", code = "NS"),
      state(name = "Nunavut", code = "NU"),
      state(name = "Ontario", code = "ON"),
      state(name = "Prince Edward Island", code = "PE"),
      state(name = "Quebec", code = "QC"),
      state(name = "Saskatchewan", code = "SK"),
      state(name = "Yukon", code = "YT"),
    )
  }

  final lazy val UnitedStatesStates: Seq[State] = {
    def state(name: String, code: String): State =
      State(name, code, UnitedStates.some)

    Seq(
      state(name = "Alabama", code = "AL"),
      state(name = "Alaska", code = "AK"),
      state(name = "American Samoa", code = "AS"),
      state(name = "Arizona", code = "AZ"),
      state(name = "Arkansas", code = "AR"),
      state(name = "California", code = "CA"),
      state(name = "Colorado", code = "CO"),
      state(name = "Connecticut", code = "CT"),
      state(name = "Delaware", code = "DE"),
      state(name = "District Of Columbia", code = "DC"),
      state(name = "Florida", code = "FL"),
      state(name = "Georgia", code = "GA"),
      state(name = "Guam", code = "GU"),
      state(name = "Hawaii", code = "HI"),
      state(name = "Idaho", code = "ID"),
      state(name = "Illinois", code = "IL"),
      state(name = "Indiana", code = "IN"),
      state(name = "Iowa", code = "IA"),
      state(name = "Kansas", code = "KS"),
      state(name = "Kentucky", code = "KY"),
      state(name = "Louisiana", code = "LA"),
      state(name = "Maine", code = "ME"),
      state(name = "Maryland", code = "MD"),
      state(name = "Massachusetts", code = "MA"),
      state(name = "Michigan", code = "MI"),
      state(name = "Minnesota", code = "MN"),
      state(name = "Mississippi", code = "MS"),
      state(name = "Missouri", code = "MO"),
      state(name = "Montana", code = "MT"),
      state(name = "Nebraska", code = "NE"),
      state(name = "Nevada", code = "NV"),
      state(name = "New Hampshire", code = "NH"),
      state(name = "New Jersey", code = "NJ"),
      state(name = "New Mexico", code = "NM"),
      state(name = "New York", code = "NY"),
      state(name = "North Carolina", code = "NC"),
      state(name = "North Dakota", code = "ND"),
      state(name = "Ohio", code = "OH"),
      state(name = "Oklahoma", code = "OK"),
      state(name = "Oregon", code = "OR"),
      state(name = "Pennsylvania", code = "PA"),
      state(name = "Puerto Rico", code = "PR"),
      state(name = "Rhode Island", code = "RI"),
      state(name = "South Carolina", code = "SC"),
      state(name = "South Dakota", code = "SD"),
      state(name = "Tennessee", code = "TN"),
      state(name = "Texas", code = "TX"),
      state(name = "Utah", code = "UT"),
      state(name = "Vermont", code = "VT"),
      state(name = "Virgin Islands", code = "VI"),
      state(name = "Virginia", code = "VA"),
      state(name = "Washington", code = "WA"),
      state(name = "West Virginia", code = "WV"),
      state(name = "Wisconsin", code = "WI"),
      state(name = "Wyoming", code = "WY"),
    )
  }
}
