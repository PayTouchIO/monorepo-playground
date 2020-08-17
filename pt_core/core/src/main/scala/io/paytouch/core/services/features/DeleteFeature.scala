package io.paytouch.core.services.features

import java.util.UUID

import scala.concurrent._

import akka.actor.ActorRef

import cats.implicits._

import io.paytouch.core.async.trackers._
import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.model.SlickMerchantRecord
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.utils.Implicits
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.features.DeletionValidator
import io.paytouch.utils.Tagging.withTag

trait DeleteFeature extends Implicits {
  type Record <: SlickMerchantRecord
  type Dao <: SlickMerchantDao
  type Validator <: DeletionValidator[Record]

  protected def dao: Dao
  protected def validator: Validator

  def classShortName: ExposedName
  def eventTracker: ActorRef withTag EventTracker

  def bulkDelete(ids: Seq[UUID])(implicit user: UserContext): Future[ErrorsOr[Unit]] =
    validator.validateDeletion(ids).flatMapTraverse(validatedBulkDelete)

  protected def validatedBulkDelete(ids: Seq[UUID])(implicit user: UserContext) = {
    val merchantId = user.merchantId
    dao.deleteByIdsAndMerchantId(ids, merchantId).map { deletedIds =>
      deletedIds.foreach(id => eventTracker ! DeletedItem(id, merchantId, classShortName))
    }
  }
}
