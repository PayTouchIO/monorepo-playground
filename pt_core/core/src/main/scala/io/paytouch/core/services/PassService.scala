package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.RequestContext

import cats.data._
import cats.implicits._

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums._

class PassService(
    val hmacService: HmacService,
    passLoaderFactory: => PassLoaderFactory,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) {
  def passLoaderForItemType(itemType: PassItemType): PassLoader =
    itemType match {
      case PassItemType.GiftCard          => passLoaderFactory.giftCardPassLoader
      case PassItemType.LoyaltyMembership => passLoaderFactory.loyaltyMembershipPassLoader
    }

  def install(
      itemId: UUID,
      `type`: PassType,
      itemType: PassItemType,
      orderId: Option[UUID],
    ): Future[Option[String]] = {
    val passLoader = passLoaderForItemType(itemType)

    (for {
      _ <- OptionT(passLoader.validate(itemId))
      updated <- OptionT(passLoader.updatedPassInstalledAtField(itemId, orderId))
      url <- OptionT.fromOption[Future](passLoader.urlForType(`type`, updated))
    } yield url).value
  }

  def generateUrl(
      itemId: UUID,
      `type`: PassType,
      itemType: PassItemType,
      orderId: Option[UUID] = None,
    ): String =
    generateUri(itemId, `type`, itemType, orderId).toString

  def generatePath(
      itemId: UUID,
      `type`: PassType,
      itemType: PassItemType,
      orderId: Option[UUID] = None,
    ): String =
    generateUri(itemId, `type`, itemType, orderId).toRelative.toString

  private def generateUri(
      itemId: UUID,
      `type`: PassType,
      itemType: PassItemType,
      orderId: Option[UUID],
    ): Uri = {
    val defaultParams = Map("id" -> itemId.toString, "type" -> `type`.entryName, "item_type" -> itemType.entryName)
    val params = orderId.fold[Map[String, String]](defaultParams)(oId => defaultParams + ("order_id" -> oId.toString))

    hmacService.generateUri("/v1/public/passes.install", params)
  }

  def verifyUrl(requestContext: RequestContext): Boolean =
    hmacService.verifyUrl(requestContext)
}
