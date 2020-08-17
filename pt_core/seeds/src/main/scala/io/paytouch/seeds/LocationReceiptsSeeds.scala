package io.paytouch.seeds

import io.paytouch.core.data.model._

object LocationReceiptsSeeds extends Seeds {

  lazy val locationEmailReceiptDao = daos.locationEmailReceiptDao
  lazy val locationPrintReceiptDao = daos.locationPrintReceiptDao
  lazy val locationReceiptDao = daos.locationReceiptDao

  def load(locations: Seq[LocationRecord])(implicit user: UserRecord) = {

    val locationEmailReceipts = locations.map { location =>
      LocationEmailReceiptUpdate.defaultUpdate(user.merchantId, location.id)
    }
    locationEmailReceiptDao.bulkUpsert(locationEmailReceipts)

    val locationPrintReceipts = locations.map { location =>
      LocationPrintReceiptUpdate.defaultUpdate(user.merchantId, location.id)
    }
    locationPrintReceiptDao.bulkUpsert(locationPrintReceipts)

    val locationReceipts = locations.map { location =>
      LocationReceiptUpdate.defaultUpdate(user.merchantId, location.id)
    }
    locationReceiptDao.bulkUpsert(locationReceipts)
  }
}
