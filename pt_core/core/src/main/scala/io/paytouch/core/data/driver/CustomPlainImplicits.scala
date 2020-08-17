package io.paytouch.core.data.driver

import java.sql.JDBCType
import java.time.LocalDateTime
import java.util.{ Currency, UUID }

import enumeratum._

import slick.jdbc._

import io.paytouch.core.data.model.enums.PaymentStatus
import io.paytouch.core.utils.Formatters

trait CustomPlainImplicits {

  implicit def toPgPositionedResult(r: PositionedResult): PgPositionedResult = new PgPositionedResult(r)

  class PgPositionedResult(r: PositionedResult) {
    def nextUUID() = nextUUIDOption().orNull
    def nextUUIDOption() = r.nextStringOption().map(UUID.fromString)

    def nextCurrency() = nextCurrencyOption().orNull
    def nextCurrencyOption() = r.nextStringOption().map(Currency.getInstance)

    def nextEnumOption[E <: EnumEntry](enum: Enum[E]) = r.nextStringOption().map(enum.withName)
  }

  implicit object SetPaymentStatus extends SetParameter[PaymentStatus] {
    def apply(v: PaymentStatus, pp: PositionedParameters): Unit =
      pp.setInt(PaymentStatus.indexOf(v))
  }

  implicit object SetUUID extends SetParameter[UUID] {
    def apply(v: UUID, pp: PositionedParameters): Unit =
      pp.setObject(v, JDBCType.BINARY.getVendorTypeNumber)
  }

  final implicit class RichSeq[A](seq: collection.Iterable[A]) {
    def asInParametersList: String = seq.map(id => s"'$id'").mkString(",")
  }

  def localDateTimeAsString(localDateTime: LocalDateTime) = {
    val dateAsString = Formatters.LocalDateTimeFormatter.format(localDateTime)
    s"'$dateAsString'"
  }
}

object CustomPlainImplicits extends CustomPlainImplicits
