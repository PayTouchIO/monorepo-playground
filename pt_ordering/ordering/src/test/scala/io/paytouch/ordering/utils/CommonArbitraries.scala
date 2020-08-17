package io.paytouch.ordering.utils

import java.time.{ LocalTime, ZoneId, ZonedDateTime }
import java.util.{ Currency, UUID }

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums._
import io.paytouch.ordering.clients.paytouch.core.entities.{ ImageUrls, _ }
import io.paytouch.ordering.entities.ekashu.SuccessPayload
import io.paytouch.ordering.entities.enums.OrderType
import io.paytouch.ordering.entities.jetdirect.CallbackPayload
import io.paytouch.ordering.entities.{ AddressUpsertion, Cart, CartItem, MonetaryAmount }
import org.scalacheck.{ Arbitrary, Gen }

trait CommonArbitraries extends RandomDataGenerator with Generators {
  implicit val arbitraryWord: Arbitrary[String] = Arbitrary(genString)
  implicit val arbitraryUUID: Arbitrary[UUID] = Arbitrary(Gen.uuid)
  implicit val arbitraryInt: Arbitrary[Int] = Arbitrary(genInt)
  implicit val arbitraryBoolean: Arbitrary[Boolean] = Arbitrary(genBoolean)

  implicit val arbitraryBigDecimal: Arbitrary[BigDecimal] = Arbitrary(genBigDecimal)
  implicit val arbitraryCurrency: Arbitrary[Currency] = Arbitrary(genCurrency)

  implicit val arbitraryMonetaryAmount: Arbitrary[MonetaryAmount] = Arbitrary {
    for {
      amount <- genBigDecimal
      curr <- genCurrency
    } yield MonetaryAmount(amount, curr)
  }

  implicit val arbitraryZonedDateTime: Arbitrary[ZonedDateTime] = Arbitrary(genZonedDateTime)
  implicit val arbitraryLocalTime: Arbitrary[LocalTime] = Arbitrary(genLocalTime)
  implicit val arbitraryZoneId: Arbitrary[ZoneId] = Arbitrary(genZoneId)

  implicit val arbitraryAddressUpsertion: Arbitrary[AddressUpsertion] = Arbitrary(genAddressUpsertion)

  // Empty Sequences //
  implicit def arbSeq[T](implicit a: Arbitrary[T]): Arbitrary[Seq[T]] = Arbitrary(Seq.empty[T])

  @scala.annotation.nowarn("msg=Auto-application")
  implicit val arbitraryCategories: Arbitrary[Seq[Category]] =
    toArbitraryEmptySeq[Category]

  @scala.annotation.nowarn("msg=Auto-application")
  implicit val arbitraryProducts: Arbitrary[Seq[Product]] =
    toArbitraryEmptySeq[Product]

  private def toArbitraryEmptySeq[T: Arbitrary]: Arbitrary[Seq[T]] =
    Arbitrary(Seq.empty[T])

  implicit val arbitraryAcceptanceStatus: Arbitrary[AcceptanceStatus] =
    Arbitrary(genAcceptanceStatus)
}
