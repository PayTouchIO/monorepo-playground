package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.conversions.BundleSetConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.OrderBundleUpdate
import io.paytouch.core.data.model.upsertions.BundleSetUpsertion
import io.paytouch.core.entities.{ ArticleUpdate, BundleSet, UserContext }
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple

import io.paytouch.core.validators.{
  ArticleValidator,
  BundleOptionValidator,
  BundleSetValidator,
  RecoveredOrderUpsertion,
}

import scala.concurrent._

class BundleSetService(val bundleOptionService: BundleOptionService)(implicit val ec: ExecutionContext, val daos: Daos)
    extends BundleSetConversions {

  val dao = daos.bundleSetDao
  val bundleOptionDao = daos.bundleOptionDao

  val validator = new BundleSetValidator
  val bundleOptionValidator = new BundleOptionValidator
  val articleValidator = new ArticleValidator

  def convertToBundleSetUpdates(
      productId: UUID,
      productUpsertion: ArticleUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[BundleSetUpsertion]]]] =
    productUpsertion.bundleSets match {
      case Some(bundleSets) =>
        val bundleSetIds = bundleSets.map(_.id)
        val bundleOptionIds = bundleSets.flatMap(_.options.getOrElse(Seq.empty).map(_.id))
        val articleIds = bundleSets.flatMap(_.options.getOrElse(Seq.empty).map(_.articleId))
        for {
          validBundleSet <- validator.validateByIds(bundleSetIds)
          validBundleOptions <- bundleOptionValidator.validateByIds(bundleOptionIds)
          validArticleIds <- articleValidator.validateStorableByIds(articleIds)
        } yield Multiple.combine(validBundleSet, validBundleOptions, validArticleIds) {
          case (_, _, _) =>
            val result = bundleSets.map { bundleSet =>
              val options = bundleSet
                .options
                .getOrElse(Seq.empty)
                .zipWithIndex
                .map { case (bo, index) => bundleOptionService.toBundleOptionUpdate(bundleSet.id, bo, index) }
              toBundleSetUpsertion(productId, bundleSet, options)
            }
            Some(result)
        }
      case None => Future.successful(Multiple.empty)
    }

  def findAllPerProduct(productIds: Seq[UUID]): Future[Map[UUID, Seq[BundleSet]]] =
    for {
      bundleSets <- dao.findByProductIds(productIds)
      bundleOptions <- bundleOptionService.findAllPerBundleSet(bundleSets.map(_.id))
    } yield bundleSets.groupBy(_.bundleId).transform { (_, v) =>
      fromRecordsAndOptionsToEntities(v, bundleOptions)
    }

}
