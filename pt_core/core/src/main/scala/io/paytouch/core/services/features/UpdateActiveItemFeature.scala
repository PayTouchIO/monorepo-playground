package io.paytouch.core.services.features

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.data.daos.features.SlickToggleableItemDao
import io.paytouch.core.data.model.SlickToggleableRecord
import io.paytouch.core.entities._
import io.paytouch.core.utils.Implicits
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.features.ValidatorWithExtraFields

trait UpdateActiveItemFeature extends Implicits { self =>
  type Record <: SlickToggleableRecord
  type Dao <: SlickToggleableItemDao
  type Validator <: ValidatorWithExtraFields[Record]

  protected def dao: Dao
  protected def validator: Validator

  def updateActiveItems(items: Seq[UpdateActiveItem])(implicit user: UserContext): Future[ErrorsOr[Unit]] =
    validator
      .accessByIds(items.map(_.itemId))
      .asNested(dao.bulkUpdateActiveField(user.merchantId, items))
}
