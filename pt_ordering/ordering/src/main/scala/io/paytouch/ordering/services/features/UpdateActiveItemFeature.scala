package io.paytouch.ordering.services.features

import io.paytouch.ordering.data.daos.features.SlickToggleableItemDao
import io.paytouch.ordering.data.model.SlickRecord
import io.paytouch.ordering.entities.{ UpdateActiveItem, UserContext }
import io.paytouch.ordering.utils.Implicits
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData
import io.paytouch.ordering.validators.features.ValidatorWithExtraFields

import scala.concurrent.Future

trait UpdateActiveItemFeature extends Implicits { self =>

  type Dao <: SlickToggleableItemDao
  type Record <: SlickRecord
  type Validator <: ValidatorWithExtraFields { type Context = UserContext; type Record = self.Record }

  protected def dao: Dao
  protected def validator: Validator

  def updateActive(items: Seq[UpdateActiveItem])(implicit user: UserContext): Future[ValidatedData[Unit]] =
    validator.accessByIds(items.map(_.itemId)).mapValid { records =>
      for {
        _ <- dao.bulkUpdateActiveField(user.locationIds, items)
        _ <- processingAfterUpdateActive(items, records)
      } yield ()
    }

  protected def processingAfterUpdateActive(
      items: Seq[UpdateActiveItem],
      records: Seq[Record],
    )(implicit
      user: UserContext,
    ): Future[Unit] = Future.unit
}
