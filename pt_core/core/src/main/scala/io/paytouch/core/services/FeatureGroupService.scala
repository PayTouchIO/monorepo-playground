package io.paytouch.core.services

import java.util.UUID

import cats.implicits._
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.FeatureGroupRecord
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.FeatureGroupValidator

import scala.concurrent._

class FeatureGroupService(implicit val ec: ExecutionContext, val daos: Daos) {

  type Record = FeatureGroupRecord
  type Validator = FeatureGroupValidator

  protected val dao = daos.featureGroupDao
  protected val validator = new FeatureGroupValidator

  def findAllRecords(): Future[Seq[Record]] = dao.findAll()

  val validateIds = validator.validateIds _

  def validateOptIds(maybeIds: Option[Seq[UUID]]): Future[ErrorsOr[Option[Seq[UUID]]]] =
    maybeIds match {
      case Some(ids) => validator.validateIds(ids).mapNested(r => Some(r))
      case _         => Multiple.success(None).pure[Future]
    }
}
