package io.paytouch.core.async.trackers

import java.time.ZonedDateTime
import java.util.UUID

import akka.actor.Actor
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.EventRecord
import io.paytouch.core.entities.ExposedEntity
import io.paytouch.core.entities.enums.{ ExposedName, TrackableAction }
import io.paytouch.core.json.JsonSupport
import io.paytouch.core.utils.UtcTime

sealed trait TrackableItem[T] {
  implicit def toOpt(t: T): Option[T] = Some(t)

  def id: UUID
  def merchantId: UUID
  def action: TrackableAction
  def `object`: ExposedName
  def dataAsJson: Option[JsonSupport#JValue]
  val receivedAt: ZonedDateTime = UtcTime.now

  def toEvent =
    EventRecord(
      id = id,
      merchantId = merchantId,
      action = action,
      `object` = `object`,
      data = dataAsJson,
      receivedAt = receivedAt,
    )
}

final case class DeletedItem(
    id: UUID,
    merchantId: UUID,
    `object`: ExposedName,
  ) extends TrackableItem[AnyRef] {
  val dataAsJson = None
  val action = TrackableAction.Deleted
}

final case class CreatedItem[T <: ExposedEntity](
    id: UUID,
    merchantId: UUID,
    data: T,
  ) extends TrackableItem[T] {
  val dataAsJson = Some(JsonSupport.fromEntityToJValue(data))
  val action = TrackableAction.Created
  val `object` = data.classShortName
}

final case class UpdatedItem[T <: ExposedEntity](
    id: UUID,
    merchantId: UUID,
    data: T,
  ) extends TrackableItem[T] {
  val dataAsJson = Some(JsonSupport.fromEntityToJValue(data))
  val action = TrackableAction.Updated
  val `object` = data.classShortName
}

class EventTracker(implicit daos: Daos) extends Actor {

  lazy val eventDao = daos.eventDao

  def receive: Receive = {
    case item: TrackableItem[_] if needsTracking(item) => eventDao.insert(item.toEvent)
  }

  private def needsTracking(item: TrackableItem[_]) =
    ExposedName.toTrack.contains(item.`object`)

}
