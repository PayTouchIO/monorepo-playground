package io.paytouch.core.trackers

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.async.trackers.DeletedItem
import io.paytouch.core.data.daos.features.SlickDeleteAccessibleLocationsDao
import io.paytouch.core.data.model.SlickMerchantRecord
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.services.features.DeleteWithAccessibleLocationFeature
import io.paytouch.core.services.ServiceDaoSpec
import io.paytouch.core.validators.features.DeletionValidator

class TrackOnDeleteWithAccessibleLocationFeatureSpec extends ServiceDaoSpec { self =>
  class ServiceWithDeleteFeature extends ServiceDaoSpecContext with DeleteWithAccessibleLocationFeature {
    implicit val daos = self.daos

    type Record = SlickMerchantRecord
    type Dao = SlickDeleteAccessibleLocationsDao
    type Validator = DeletionValidator[Record]

    protected val dao = mock[SlickDeleteAccessibleLocationsDao]
    protected val validator = new DeletionValidator[Record] {}

    val classShortName: ExposedName = random[ExposedName]

    val idsToDelete: Seq[UUID] = (0 to 3).map(_ => UUID.randomUUID)
    dao.deleteByIdsAndMerchantIdAndLocationIds(any, any, any) returns idsToDelete.pure[Future]
  }

  "Tracking on DeleteWithAccessibleLocationFeature" should {

    "send the EventTracker a DeletedItem message per each deleted item" in new ServiceWithDeleteFeature {

      val ids: Seq[UUID] = (0 to 5).map(_ => UUID.randomUUID)
      bulkDelete(ids).await

      val merchantId = userCtx.merchantId
      eventTrackerMock.expectMsgAllOf(
        DeletedItem(idsToDelete.head, merchantId, classShortName),
        DeletedItem(idsToDelete(1), merchantId, classShortName),
        DeletedItem(idsToDelete(2), merchantId, classShortName),
      )
    }
  }
}
