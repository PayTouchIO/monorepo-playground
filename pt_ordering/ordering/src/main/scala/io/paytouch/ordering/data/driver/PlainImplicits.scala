package io.paytouch.ordering.data.driver

import java.sql.JDBCType
import java.time.LocalDateTime
import java.util.{ Currency, UUID }

import io.paytouch.ordering.utils.Formatters
import slick.jdbc.{ PositionedParameters, PositionedResult, SetParameter }

trait PlainImplicits {

  implicit def toPgPositionedResult(r: PositionedResult): PgPositionedResult = new PgPositionedResult(r)

  class PgPositionedResult(r: PositionedResult) {
    def nextUUID = nextUUIDOption.orNull

    def nextUUIDOption = r.nextStringOption().map(UUID.fromString)

    def nextCurrency = nextCurrencyOption.orNull

    def nextCurrencyOption = r.nextStringOption().map(Currency.getInstance)
  }

  implicit object SetUUID extends SetParameter[UUID] {
    def apply(v: UUID, pp: PositionedParameters): Unit =
      pp.setObject(v, JDBCType.BINARY.getVendorTypeNumber)
  }

  implicit def toRichSeq[A](seq: Seq[A]): RichSeq[A] = new RichSeq(seq)

  class RichSeq[A](seq: Seq[A]) {
    def asInParametersList: String = seq.map(id => s"'$id'").mkString(",")
  }

  def localDateTimeAsString(localDateTime: LocalDateTime) = {
    val dateAsString = Formatters.LocalDateTimeFormatter.format(localDateTime)
    s"'$dateAsString'"
  }
}

object PlainImplicits extends PlainImplicits
