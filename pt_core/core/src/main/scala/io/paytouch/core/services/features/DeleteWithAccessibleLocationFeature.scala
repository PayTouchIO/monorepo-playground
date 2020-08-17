package io.paytouch.core.services.features

import java.util.UUID

import akka.actor.ActorRef

import cats.implicits._

import io.paytouch.core.async.trackers.{ DeletedItem, EventTracker }
import io.paytouch.core.data.daos.features.SlickDeleteAccessibleLocationsDao
import io.paytouch.core.data.model.SlickMerchantRecord
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.utils.Implicits
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.features.DeletionValidator
import io.paytouch.core.withTag

import scala.concurrent._

trait DeleteWithAccessibleLocationFeature extends Implicits {
  type Record <: SlickMerchantRecord
  type Dao <: SlickDeleteAccessibleLocationsDao
  type Validator <: DeletionValidator[Record]

  protected def dao: Dao
  protected def validator: Validator

  def classShortName: ExposedName
  def eventTracker: ActorRef withTag EventTracker

  def bulkDelete(ids: Seq[UUID])(implicit user: UserContext): Future[ErrorsOr[Unit]] = {
    val merchantId = user.merchantId
    validator.validateDeletion(ids).flatMapTraverse { _ =>
      dao.deleteByIdsAndMerchantIdAndLocationIds(ids, merchantId, user.locationIds).map { deletedIds =>
        deletedIds.foreach(id => eventTracker ! DeletedItem(id, merchantId, classShortName))
      }
    }
  }
}
