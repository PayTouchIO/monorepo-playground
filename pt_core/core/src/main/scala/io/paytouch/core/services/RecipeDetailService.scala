package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.conversions.RecipeDetailConversions
import io.paytouch.core.data.daos.{ Daos, RecipeDetailDao }
import io.paytouch.core.data.model.{ RecipeDetailRecord, RecipeDetailUpdate }
import io.paytouch.core.entities.{ ArticleUpdate, RecipeDetail, UserContext }
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple

import scala.concurrent._

class RecipeDetailService(implicit val ec: ExecutionContext, val daos: Daos) extends RecipeDetailConversions {
  type Dao = RecipeDetailDao
  type Entity = RecipeDetail
  type Record = RecipeDetailRecord

  protected val dao = daos.recipeDetailDao

  def findPerProductIds(productIds: Seq[UUID])(implicit user: UserContext): Future[Map[UUID, Seq[Entity]]] =
    dao.findByProductIds(productIds).map(_.groupBy(_.productId).transform((_, v) => toSeqEntity(v)))

  def convertToRecipeDetailUpdate(
      recipeId: UUID,
      upsertion: ArticleUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[RecipeDetailUpdate]]] =
    Future.successful {
      Multiple.success(upsertion.makesQuantity.map(q => toUpdate(recipeId, q)))
    }
}
