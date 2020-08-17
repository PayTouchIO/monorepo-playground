package io.paytouch.ordering.data.model

import java.util.UUID

import io.paytouch.ordering.data.model.upsertions.UpsertionModel
import io.paytouch.ordering.entities.Default
import io.paytouch.ordering.utils.UtcTime

trait SlickUpdate[E <: SlickRecord] extends UpsertionModel[E] {
  def id: Default[UUID]

  def merge(existing: Option[E]): E = existing.fold(toRecord)(updateRecord)

  def toRecord: E

  def updateRecord(record: E): E

  def now = UtcTime.now

  protected def requires(fields: (String, Option[Any])*): Unit =
    fields.foreach {
      case (k, v) =>
        if (v.isEmpty)
          require(
            requirement = v.isDefined,
            message = s"Impossible to convert $getClass without a $k. [$this]",
          )
    }
}
