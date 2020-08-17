package io.paytouch.core.entities

trait Metadata[SS, TS] {

  def count: Int

  def salesSummary: Option[SS]

  def typeSummary: Option[TS]
}
