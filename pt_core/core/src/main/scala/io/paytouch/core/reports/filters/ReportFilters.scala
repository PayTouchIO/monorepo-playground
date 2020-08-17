package io.paytouch.core.reports.filters

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.OrderType
import io.paytouch.core.reports.entities.enums.ReportInterval
import io.paytouch.core.reports.views.ReportView
import io.paytouch.core.utils.EnumEntrySnake

trait ReportFilters {

  def view: ReportView
  def from: LocalDateTime
  def to: LocalDateTime
  def locationIds: Seq[UUID]
  def ids: Option[Seq[UUID]]
  def orderTypes: Option[Seq[OrderType]]
  def categoryIds: Option[Seq[UUID]]
  def interval: ReportInterval
  def merchantId: UUID

  def fieldsInCsv: Seq[String] = Seq.empty[String]

  def toFieldHeader(f: EnumEntrySnake, name: Option[String] = None): String = ""

  def toGroupByHeader(name: Option[String] = None): String = ""

  def toFieldOnDemand[T](f: EnumEntrySnake, value: T): Option[T] = None

  protected def toHumanReadable(enum: EnumEntrySnake) = toPretty(enum.entryName)

  private def toPretty(text: String): String = text.split("_").map(_.capitalize).mkString(" ")
}
