package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.conversions.BundleOptionConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities.BundleOption

import scala.concurrent._

class BundleOptionService(articleService: => ArticleService)(implicit val ec: ExecutionContext, val daos: Daos)
    extends BundleOptionConversions {

  val dao = daos.bundleOptionDao

  def findAllPerBundleSet(bundleSetIds: Seq[UUID]): Future[Map[UUID, Seq[BundleOption]]] =
    for {
      bundleSets <- dao.findByBundleSetIds(bundleSetIds)
      articleInfo <- articleService.getArticleInfoPerArticleId(bundleSets.map(_.articleId))
    } yield {
      val articleInfoPerArticleId = articleInfo.map(ai => ai.id -> ai).toMap
      bundleSets
        .groupBy(_.bundleSetId)
        .transform((_, v) => fromRecordsAndOptionsToEntities(v, articleInfoPerArticleId))
    }
}
