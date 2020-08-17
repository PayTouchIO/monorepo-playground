package io.paytouch.ordering.messages

import java.util.UUID

import akka.actor.{ ActorRef, ActorSystem }
import io.paytouch.ordering.async.sqs.{ SQSMessageSender, SendMsgWithRetry }
import io.paytouch.ordering.entities.{ UpdateActiveItem, UserContext }
import io.paytouch.ordering.messages.entities.{ ImagesAssociated, ImagesDeleted, StoreCreated }
import io.paytouch.ordering.withTag

class SQSMessageHandler(val asyncSystem: ActorSystem, val messageSender: ActorRef withTag SQSMessageSender) {

  def sendStoreCreated(locationId: UUID)(implicit user: UserContext): Unit = {
    val msg = StoreCreated(user.merchantId, locationId)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendImagesDeleted(imageIds: Seq[UUID])(implicit user: UserContext): Unit =
    if (imageIds.nonEmpty) {
      val msg = ImagesDeleted(imageIds)
      messageSender ! SendMsgWithRetry(msg)
    }

  def sendImageIdsAssociated(objectId: UUID, imageIds: Seq[UUID])(implicit user: UserContext): Unit =
    if (imageIds.nonEmpty) {
      val msg = ImagesAssociated(objectId, imageIds)
      messageSender ! SendMsgWithRetry(msg)
    }
}
