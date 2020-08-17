package io.paytouch.core.entities

import io.paytouch._
import io.paytouch.implicits._

import cats.implicits._

/**
  * state and country needs to stay for compatibility reasons
  */
final case class Address(
    line1: Option[String],
    line2: Option[String],
    city: Option[String],
    state: Option[String],
    country: Option[String],
    stateData: Option[AddressState],
    countryData: Option[Country],
    postalCode: Option[String],
  )

object Address {
  def empty: Address =
    Address(
      line1 = None,
      line2 = None,
      city = None,
      state = None,
      country = None,
      stateData = None,
      countryData = None,
      postalCode = None,
    )

  sealed trait LossyIso[WeaklyTyped] {
    def weakToStrong(origin: WeaklyTyped): LossyIso.StronglyTyped[WeaklyTyped]
    def strongToWeak(stronglyTyped: LossyIso.StronglyTyped[WeaklyTyped]): WeaklyTyped
  }

  object LossyIso extends SummonerB[LossyIso] {
    final case class StronglyTyped[+WeaklyTyped] private[LossyIso] (
        countryCode: Option[CountryCode],
        stateCode: Option[StateCode],
        countryName: Option[CountryName],
        stateName: Option[StateName],
        origin: WeaklyTyped,
      ) {
      final def withDataFrom(country: Country, state: AddressState): StronglyTyped[WeaklyTyped] =
        copy(
          countryCode = country.code.some.map(CountryCode),
          stateCode = state.code.some.map(StateCode),
          countryName = country.name.some.map(CountryName),
          stateName = state.name.map(StateName),
        )

      // Manual definition makes sure that origin remains preserved
      final private[this] def copy(
          countryCode: Option[CountryCode] = this.countryCode,
          stateCode: Option[StateCode] = this.stateCode,
          countryName: Option[CountryName] = this.countryName,
          stateName: Option[StateName] = this.stateName,
        ): StronglyTyped[WeaklyTyped] =
        StronglyTyped(countryCode, stateCode, countryName, stateName, origin)
    }

    implicit val IsoAddress: LossyIso[Address] = new LossyIso[Address] {
      def weakToStrong(origin: Address): StronglyTyped[Address] =
        StronglyTyped(
          countryCode = origin.countryData.map(_.code.pipe(CountryCode)),
          stateCode = origin.stateData.map(_.code.pipe(StateCode)),
          countryName = origin.countryData.map(_.name.pipe(CountryName)),
          stateName = origin.stateData.flatMap(_.name.map(StateName)),
          origin = origin,
        )

      def strongToWeak(stronglyTyped: StronglyTyped[Address]): Address = {
        val countryData = (
          stronglyTyped.countryCode.map(_.value),
          stronglyTyped.countryName.map(_.value),
        ).mapN(Country)

        stronglyTyped
          .origin
          .copy(
            state = stronglyTyped.stateName.map(_.value),
            country = stronglyTyped.countryName.map(_.value),
            stateData = (
              stronglyTyped.stateName.map(_.value).some,
              stronglyTyped.stateCode.map(_.value),
              countryData.some,
            ).mapN(AddressState),
            countryData = countryData,
          )
      }
    }

    implicit val IsoAddressImproved: LossyIso[AddressImproved] = new LossyIso[AddressImproved] {
      def weakToStrong(origin: AddressImproved): StronglyTyped[AddressImproved] =
        StronglyTyped(
          countryCode = origin.countryData.map(_.code.pipe(CountryCode)),
          stateCode = origin.stateData.map(_.code.pipe(StateCode)),
          countryName = origin.countryData.map(_.name.pipe(CountryName)),
          stateName = origin.stateData.flatMap(_.name.map(StateName)),
          origin = origin,
        )

      def strongToWeak(stronglyTyped: StronglyTyped[AddressImproved]): AddressImproved = {
        val countryData = (
          stronglyTyped.countryCode.map(_.value),
          stronglyTyped.countryName.map(_.value),
        ).mapN(Country)

        stronglyTyped
          .origin
          .copy(
            stateData = (
              stronglyTyped.stateName.map(_.value).some,
              stronglyTyped.stateCode.map(_.value),
              countryData.some,
            ).mapN(AddressState),
            countryData = countryData,
          )
      }
    }

    implicit val IsoAddressImprovedUpsertion: LossyIso[AddressImprovedUpsertion] =
      new LossyIso[AddressImprovedUpsertion] {
        def weakToStrong(origin: AddressImprovedUpsertion): StronglyTyped[AddressImprovedUpsertion] =
          StronglyTyped(
            countryCode = origin.countryCode.map(CountryCode),
            stateCode = origin.stateCode.map(StateCode),
            countryName = None, // that's why it's Lossy...
            stateName = None, // that's why it's Lossy...
            origin = origin,
          )

        def strongToWeak(stronglyTyped: StronglyTyped[AddressImprovedUpsertion]): AddressImprovedUpsertion =
          stronglyTyped
            .origin
            .copy(
              countryCode = stronglyTyped.countryCode.map(_.value),
              stateCode = stronglyTyped.stateCode.map(_.value),
            )
      }

    implicit val IsoAddressUpsertion: LossyIso[AddressUpsertion] = new LossyIso[AddressUpsertion] {
      def weakToStrong(origin: AddressUpsertion): StronglyTyped[AddressUpsertion] =
        StronglyTyped(
          countryCode = origin.countryCode.map(CountryCode),
          stateCode = origin.stateCode.map(StateCode),
          countryName = origin.country.map(CountryName),
          stateName = origin.state.map(StateName),
          origin = origin,
        )

      def strongToWeak(stronglyTyped: StronglyTyped[AddressUpsertion]): AddressUpsertion =
        stronglyTyped
          .origin
          .copy(
            state = stronglyTyped.stateName.map(_.value),
            country = stronglyTyped.countryName.map(_.value),
            stateCode = stronglyTyped.stateCode.map(_.value),
            countryCode = stronglyTyped.countryCode.map(_.value),
          )
    }

    implicit val IsoAddressImprovedSync: LossyIso[AddressImprovedSync] =
      new LossyIso[AddressImprovedSync] {
        def weakToStrong(origin: AddressImprovedSync): StronglyTyped[AddressImprovedSync] =
          StronglyTyped(
            countryCode = origin.countryCode.map(CountryCode),
            stateCode = origin.stateCode.map(StateCode),
            countryName = None, // that's why it's Lossy...
            stateName = None, // that's why it's Lossy...
            origin = origin,
          )

        def strongToWeak(stronglyTyped: StronglyTyped[AddressImprovedSync]): AddressImprovedSync =
          stronglyTyped
            .origin
            .copy(
              countryCode = stronglyTyped.countryCode.map(_.value),
              stateCode = stronglyTyped.stateCode.map(_.value),
            )
      }

    implicit val IsoAddressSync: LossyIso[AddressSync] = new LossyIso[AddressSync] {
      def weakToStrong(origin: AddressSync): StronglyTyped[AddressSync] =
        StronglyTyped(
          countryCode = origin.countryCode.map(CountryCode),
          stateCode = origin.stateCode.map(StateCode),
          countryName = origin.country.map(CountryName),
          stateName = origin.state.map(StateName),
          origin = origin,
        )

      def strongToWeak(stronglyTyped: StronglyTyped[AddressSync]): AddressSync =
        stronglyTyped
          .origin
          .copy(
            state = stronglyTyped.stateName.map(_.value),
            country = stronglyTyped.countryName.map(_.value),
            stateCode = stronglyTyped.stateCode.map(_.value),
            countryCode = stronglyTyped.countryCode.map(_.value),
          )
    }
  }
}

/**
  * state and country (names) need to stay for compatibility reasons
  */
final case class AddressUpsertion(
    line1: Option[String],
    line2: Option[String],
    city: Option[String],
    state: Option[String],
    country: Option[String],
    stateCode: Option[String],
    countryCode: Option[String],
    postalCode: Option[String],
  ) {
  def toAddressSync: AddressSync =
    AddressSync(
      line1 = line1,
      line2 = line2,
      city = city,
      state = state,
      country = country,
      stateCode = stateCode,
      countryCode = countryCode,
      postalCode = postalCode,
    )
}

object AddressUpsertion {
  def empty: AddressUpsertion =
    AddressUpsertion(
      line1 = None,
      line2 = None,
      city = None,
      state = None,
      country = None,
      stateCode = None,
      countryCode = None,
      postalCode = None,
    )
}

final case class AddressImproved(
    line1: Option[String],
    line2: Option[String],
    city: Option[String],
    stateData: Option[AddressState],
    countryData: Option[Country],
    postalCode: Option[String],
  )

object AddressImproved {
  def empty: AddressImproved =
    AddressImproved(
      line1 = None,
      line2 = None,
      city = None,
      stateData = None,
      countryData = None,
      postalCode = None,
    )
}

final case class AddressImprovedUpsertion(
    line1: Option[String],
    line2: Option[String],
    city: Option[String],
    stateCode: Option[String],
    countryCode: Option[String],
    postalCode: Option[String],
  ) {
  def toAddressImprovedSync: AddressImprovedSync =
    AddressImprovedSync(
      line1 = line1,
      line2 = line2,
      city = city,
      stateCode = stateCode,
      countryCode = countryCode,
      postalCode = postalCode,
    )
}

object AddressImprovedUpsertion {
  def empty: AddressImprovedUpsertion =
    AddressImprovedUpsertion(
      line1 = None,
      line2 = None,
      city = None,
      stateCode = None,
      countryCode = None,
      postalCode = None,
    )
}

/**
  * state and country (names) need to stay for compatibility reasons
  */
final case class AddressSync(
    line1: Option[String],
    line2: Option[String],
    city: Option[String],
    state: Option[String],
    country: Option[String],
    stateCode: Option[String],
    countryCode: Option[String],
    postalCode: Option[String],
  )

object AddressSync {
  def empty: AddressSync =
    AddressSync(
      line1 = None,
      line2 = None,
      city = None,
      state = None,
      country = None,
      stateCode = None,
      countryCode = None,
      postalCode = None,
    )
}

// We are not using this one yet
final case class AddressImprovedSync(
    line1: Option[String],
    line2: Option[String],
    city: Option[String],
    stateCode: Option[String],
    countryCode: Option[String],
    postalCode: Option[String],
  )

object AddressImprovedSync {
  def empty: AddressImprovedSync =
    AddressImprovedSync(
      line1 = None,
      line2 = None,
      city = None,
      stateCode = None,
      countryCode = None,
      postalCode = None,
    )
}
