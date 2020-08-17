package io.paytouch.ordering.entities

import java.net.URLEncoder

final case class Address(
    line1: Option[String],
    line2: Option[String],
    city: Option[String],
    state: Option[String],
    country: Option[String],
    postalCode: Option[String],
  ) {

  def encodedString: String =
    List(line1, line2, city, state, country, postalCode).flatMap(_.map(encoded)).mkString(",")

  private def encoded(s: String): String = URLEncoder.encode(s, "UTF-8")
}

final case class AddressUpsertion(
    line1: ResettableString = None,
    line2: ResettableString = None,
    city: ResettableString = None,
    state: ResettableString = None,
    country: ResettableString = None,
    postalCode: ResettableString = None,
  ) {
  def isEmpty = line1.isEmpty && city.isEmpty && postalCode.isEmpty

  def toAddress: Option[Address] =
    if (!isEmpty) {
      val address =
        Address(line1 = line1, line2 = line2, city = city, state = state, country = country, postalCode = postalCode)
      Some(address)
    }
    else None
}
