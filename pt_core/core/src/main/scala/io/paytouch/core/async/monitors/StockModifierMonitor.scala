package io.paytouch.core.async.monitors

import java.util.UUID

import scala.concurrent._

import akka.actor._

import cats.data._
import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.QuantityChangeReason
import io.paytouch.core.entities.UserContext
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.services.StockService
import io.paytouch.core.withTag

final case class StockModifierChange(
    locationQuantitiesPerArticle: Map[ArticleRecord, Map[UUID, BigDecimal]],
    orderId: Option[UUID],
    reason: Option[QuantityChangeReason],
    notes: Option[String],
    userContext: UserContext,
  )

final case class StockPartModifierChanges(
    locationQuantitiesPerArticle: Map[ArticleRecord, Map[UUID, BigDecimal]],
    orderId: Option[UUID],
    reason: Option[QuantityChangeReason],
    notes: Option[String],
    userContext: UserContext,
  )

class StockModifierMonitor(
    productQHMonitor: ActorRef withTag ProductQuantityHistoryMonitor,
    messageHandler: SQSMessageHandler,
    stockService: StockService,
  )(implicit
    daos: Daos,
  ) extends Actor {
  implicit val executionContext: ExecutionContext =
    context.dispatcher

  val articleDao = daos.articleDao
  val productLocationDao = daos.productLocationDao
  val productPartDao = daos.productPartDao
  val stockDao = daos.stockDao

  def receive: Receive = {
    case StockModifierChange(quantities, orderId, reason, notes, user) =>
      changeStockWithModifiers(quantities, orderId, reason, notes)(user)

    case StockPartModifierChanges(quantities, orderId, reason, notes, user) =>
      changePartStockWithModifiers(quantities, orderId, reason, notes)(user)
  }

  private def changeStockWithModifiers(
      quantities: Map[ArticleRecord, Map[UUID, BigDecimal]],
      orderId: Option[UUID],
      reason: Option[QuantityChangeReason],
      notes: Option[String],
    )(implicit
      userContext: UserContext,
    ) =
    for {
      (article, quantityPerLocation) <- quantities.view.filterKeys(_.trackInventory).toMap
      (locationId, quantity) <- quantityPerLocation
      if quantity != 0
    } yield stockDao
      .increaseStockQuantity(productId = article.id, locationId = locationId, quantity = quantity)
      .map { _ =>
        recordProductQuantityChange(
          productId = article.id,
          locationId = locationId,
          quantityDiff = quantity,
          orderId = orderId,
          reason = reason,
          notes = notes,
        )
      }

  private def changePartStockWithModifiers(
      quantities: Map[ArticleRecord, Map[UUID, BigDecimal]],
      orderId: Option[UUID],
      reason: Option[QuantityChangeReason],
      notes: Option[String],
    )(implicit
      userContext: UserContext,
    ) = {
    val articlePartsToTrack = quantities.keys.filter(_.trackInventoryParts).toSeq
    partsPerProduct(articlePartsToTrack).map { partsPerPrd =>
      val data = for {
        (article, partsWithProductParts) <- partsPerPrd
        quantitiesPerLocationId = quantities.getOrElse(article, Seq.empty).toMap
        (part, productPart) <- partsWithProductParts
      } yield part -> quantitiesPerLocationId.view.mapValues(_ * productPart.quantityNeeded).toMap

      self ! StockModifierChange(data, orderId, reason, notes, userContext)
    }
  }

  private def partsPerProduct(
      articles: Seq[ArticleRecord],
    ): Future[Map[ArticleRecord, Seq[(ArticleRecord, ProductPartRecord)]]] =
    for {
      productParts <- productPartDao.findByProductIds(articles.map(_.id))
      partIds = productParts.map(_.partId)
      parts <- articleDao.findByIds(partIds)
    } yield groupPartsAndProductParts(articles)(parts, productParts)

  private def groupPartsAndProductParts(
      articles: Seq[ArticleRecord],
    )(
      parts: Seq[ArticleRecord],
      productParts: Seq[ProductPartRecord],
    ): Map[ArticleRecord, Seq[(ArticleRecord, ProductPartRecord)]] =
    articles.map { article =>
      val partsWithProductParts = for {
        productPart <- productParts.filter(_.productId == article.id)
        part <- parts.filter(_.id == productPart.partId)
      } yield part -> productPart

      article -> partsWithProductParts
    }.toMap

  private def recordProductQuantityChange(
      productId: UUID,
      locationId: UUID,
      quantityDiff: BigDecimal,
      orderId: Option[UUID],
      reason: Option[QuantityChangeReason],
      notes: Option[String],
    )(implicit
      user: UserContext,
    ) =
    for {
      productLocation <- OptionT(productLocationDao.findByProductIdAndLocationId(productId, locationId))
      updatedStock <- OptionT(stockDao.findByProductIdAndLocationIds(user.merchantId, productId, Seq(locationId)))
    } yield {
      val inferredReason = reason.getOrElse {
        if (quantityDiff > 0) QuantityChangeReason.CustomerReturn
        else QuantityChangeReason.Sale
      }
      val msg = ProductQuantityIncrease(
        productLocation = productLocation,
        prevQuantity = updatedStock.quantity - quantityDiff,
        newQuantity = updatedStock.quantity,
        orderId = orderId,
        reason = inferredReason,
        notes = notes,
        userContext = user,
      )
      productQHMonitor ! msg

      val updatedStockEntity = stockService.fromRecordToEntity(updatedStock)
      messageHandler.sendEntitySynced(updatedStockEntity, locationId)
    }

}
