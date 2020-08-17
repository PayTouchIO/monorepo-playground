package io.paytouch.core.validators

import java.util.UUID

import io.paytouch.core.data.daos.{ Daos, FeatureGroupDao }
import io.paytouch.core.data.model.FeatureGroupRecord
import io.paytouch.core.errors.InvalidFeatureGroupIds
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr

import scala.concurrent.{ ExecutionContext, Future }

class FeatureGroupValidator(implicit val ec: ExecutionContext, val daos: Daos) {

  type Record = FeatureGroupRecord
  type Dao = FeatureGroupDao

  protected val dao = daos.featureGroupDao

  def validateIds(ids: Seq[UUID]): Future[ErrorsOr[Seq[UUID]]] =
    dao.findAll().map { allFeatures =>
      val allExistingIds = allFeatures.map(_.id).toSet
      val intersection = allExistingIds.intersect(ids.toList.toSet)

      if (ids.toList.toSet == intersection)
        Multiple.failure(InvalidFeatureGroupIds(intersection.toSeq))
      else
        Multiple.success(ids)
    }
}
