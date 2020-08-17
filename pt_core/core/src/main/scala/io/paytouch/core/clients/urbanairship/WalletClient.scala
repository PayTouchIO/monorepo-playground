package io.paytouch.core.clients.urbanairship

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.client.RequestBuilding._
import io.paytouch.core.{ UrbanAirshipHost, UrbanAirshipPassword, UrbanAirshipUsername }
import io.paytouch.core.clients.urbanairship.entities._
import io.paytouch.core.logging.MdcActor
import io.paytouch.logging.BaseMdcActor
import io.paytouch.utils.Tagging._

import scala.concurrent._

class WalletClient(
    val host: String withTag UrbanAirshipHost,
    val username: String withTag UrbanAirshipUsername,
    val password: String withTag UrbanAirshipPassword,
  )(implicit
    val mdcActor: ActorRef withTag BaseMdcActor,
    val system: ActorSystem,
  ) extends UAClient {

  def createPass(
      templateId: String,
      externalId: String,
      upsertion: PassUpsertion,
    ): Future[UAResponse[Pass]] =
    sendAndReceive[Pass](Post(s"/v1/pass/$templateId/id/$externalId", upsertion))

  def updatePass(externalId: String, upsertion: PassUpsertion): Future[UAResponse[PassUpdateResponse]] =
    sendAndReceive[PassUpdateResponse](Put(s"/v1/pass/id/$externalId", upsertion))

  def createTemplateWithProjectIdAndExternalId(
      projectId: String,
      externalId: String,
      creation: JValue,
    ): Future[UAResponse[TemplateUpserted]] =
    sendAndReceive[TemplateUpserted](Post(s"/v1/template/$projectId/id/$externalId", creation))

  def updateTemplateWithExternalId(externalId: String, update: JValue): Future[UAResponse[TemplateUpserted]] =
    sendAndReceive[TemplateUpserted](Put(s"/v1/template/id/$externalId", update))
}
