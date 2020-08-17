package io.paytouch.core.entities

import io.paytouch.core.entities.enums.ExposedName

final case class LegalDetails(
    businessName: Option[String],
    vatId: Option[String],
    address: Option[AddressImproved],
    invoicingCode: Option[String],
  ) extends ExposedEntity {
  val classShortName = ExposedName.LegalDetails

  final def country: Option[Country] =
    address.flatMap(_.countryData)
}

object LegalDetails {
  def empty: LegalDetails =
    LegalDetails(
      businessName = None,
      vatId = None,
      address = None,
      invoicingCode = None,
    )
}

final case class LegalDetailsUpsertion(
    businessName: Option[String],
    vatId: Option[String],
    address: Option[AddressImprovedUpsertion],
    invoicingCode: Option[String],
  )

object LegalDetailsUpsertion {
  def empty: LegalDetailsUpsertion =
    LegalDetailsUpsertion(
      businessName = None,
      vatId = None,
      address = None,
      invoicingCode = None,
    )
}
