package io.paytouch.core.async.trackers

import java.util.UUID

import akka.actor.Props
import io.paytouch.core.data.model.EventRecord
import io.paytouch.core.entities.ExposedEntity
import io.paytouch.core.entities.enums.{ ExposedName, TrackableAction }
import io.paytouch.core.utils.{ DefaultFixtures, FSpec, MockedRestApi }

final case class DataToTrack(text: String, classShortName: ExposedName) extends ExposedEntity

final case class DataToIgnore(text: String, classShortName: ExposedName) extends ExposedEntity

class EventTrackerSpec extends FSpec {

  val testAsyncSystem = MockedRestApi.testAsyncSystem

  lazy val objectToTrack = ExposedName.toTrack.head
  lazy val objectToIgnore = (ExposedName.values diff ExposedName.toTrack).head

  lazy val dataToTrack = DataToTrack("yo, track", objectToTrack)
  lazy val dataToIgnore = DataToIgnore("yo, ignore", objectToIgnore)

  class EventTrackerSpecContext extends FSpecContext with DefaultFixtures {

    val eventTracker = testAsyncSystem.actorOf(Props(new EventTracker))
    val eventDao = daos.eventDao

    val id = UUID.randomUUID

    def assertMsgRecorded(item: TrackableItem[_], action: TrackableAction) =
      afterAWhile {
        val maybeEvent = eventDao.findById(item.id).await
        maybeEvent should beSome[EventRecord]
        val event = maybeEvent.get
        event.id ==== item.id
        event.merchantId ==== item.merchantId
        event.action ==== action
        event.`object` ==== item.`object`
        event.data ==== item.dataAsJson
        event.receivedAt ==== item.receivedAt
      }

    def assertMsgIgnored(item: TrackableItem[_]) =
      afterAWhile {
        eventDao.findById(item.id).await should beNone
      }
  }

  "EventTracker" should {

    "when an item that needs tracking" in {
      "insert DeletedItem event" in new EventTrackerSpecContext {
        val msg = DeletedItem(id = id, merchantId = merchant.id, `object` = objectToTrack)
        eventTracker ! msg
        assertMsgRecorded(msg, TrackableAction.Deleted)
      }

      "insert CreatedItem event" in new EventTrackerSpecContext {
        val msg = CreatedItem(id = id, merchantId = merchant.id, data = dataToTrack)
        eventTracker ! msg
        assertMsgRecorded(msg, TrackableAction.Created)
      }

      "insert UpdatedItem event" in new EventTrackerSpecContext {
        val msg = UpdatedItem(id = id, merchantId = merchant.id, data = dataToTrack)
        eventTracker ! msg
        assertMsgRecorded(msg, TrackableAction.Updated)
      }
    }

    "when an item that does NOT need tracking" in {
      "ignore DeletedItem event" in new EventTrackerSpecContext {
        val msg = DeletedItem(id = id, merchantId = merchant.id, `object` = objectToIgnore)
        eventTracker ! msg
        assertMsgIgnored(msg)
      }

      "ignore CreatedItem event" in new EventTrackerSpecContext {
        val msg = CreatedItem(id = id, merchantId = merchant.id, data = dataToIgnore)
        eventTracker ! msg
        assertMsgIgnored(msg)
      }

      "ignore UpdatedItem event" in new EventTrackerSpecContext {
        val msg = UpdatedItem(id = id, merchantId = merchant.id, data = dataToIgnore)
        eventTracker ! msg
        assertMsgIgnored(msg)
      }
    }
  }

}
