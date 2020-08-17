package io.paytouch.ordering.clients.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed trait CardType extends EnumEntrySnake

case object CardType extends Enum[CardType] {

  case object Visa extends CardType
  case object MasterCard extends CardType
  case object Maestro extends CardType
  case object Amex extends CardType
  case object Jcb extends CardType
  case object Diners extends CardType
  case object Discover extends CardType
  case object CarteBleue extends CardType
  case object CarteBlanc extends CardType
  case object Voyager extends CardType
  case object Wex extends CardType
  case object ChinaUnionPay extends CardType
  case object Style extends CardType
  case object ValueLink extends CardType
  case object Interac extends CardType
  case object Laser extends CardType
  case object Other extends CardType

  val values = findValues

  final lazy val ekashuNamesToValuesMap: Map[String, CardType] =
    values.map(v => v.entryName.toLowerCase.replace("_", "") -> v).toMap

  def withEkashuNameOption(name: String): Option[CardType] = ekashuNamesToValuesMap.get(name.toLowerCase)

  def withEkashuName(name: String) =
    withEkashuNameOption(name).getOrElse(throw new NoSuchElementException(buildNotFoundMessage(name)))

  final lazy val worldpayNamesToValuesMap: Map[String, CardType] = Map(
    "Visa" -> Visa,
    "Mastercard" -> MasterCard,
    "Discover" -> Discover,
    "Amex" -> Amex,
    "Diners Club" -> Diners,
    "JCB" -> Jcb,
    "Carte Blanche" -> CarteBlanc,
    "Union Pay" -> ChinaUnionPay,
    "Other" -> Other,
  )

  def withWorldpayName(name: String) =
    worldpayNamesToValuesMap.get(name).getOrElse(throw new NoSuchElementException(buildNotFoundMessage(name)))

  final lazy val jetdirectNamesToValuesMap: Map[String, CardType] = Map(
    "VS" -> Visa,
    "MC" -> MasterCard,
    "AX" -> Amex,
    "DC" -> Discover,
  )

  def withJetdirectNameOption(name: String): Option[CardType] = jetdirectNamesToValuesMap.get(name)

  def withJetdirectName(name: String) =
    withJetdirectNameOption(name).getOrElse(throw new NoSuchElementException(buildNotFoundMessage(name)))

  final lazy val stripeNamesToValuesMap: Map[String, CardType] = Map(
    "amex" -> Amex,
    "diners" -> Diners,
    "discover" -> Discover,
    "jcb" -> Jcb,
    "mastercard" -> MasterCard,
    "unionpay" -> ChinaUnionPay,
    "visa" -> Visa,
    "unknown" -> Other,
  )

  def withStripeNameOption(name: String): Option[CardType] = stripeNamesToValuesMap.get(name)

  private def buildNotFoundMessage(notFoundName: String): String =
    s"$notFoundName is not a member of Enum ($existingEntriesString)"

  private lazy val existingEntriesString =
    values.map(_.entryName).mkString(", ")
}
