package io.paytouch.seeds

import io.paytouch.core.data.model.{ SupplierRecord, SupplierUpdate, UserRecord }
import io.paytouch.seeds.IdsProvider._

import scala.concurrent._

object SupplierSeeds extends Seeds {

  lazy val supplierDao = daos.supplierDao

  def load(implicit user: UserRecord): Future[Seq[SupplierRecord]] = {
    val supplierIds = supplierIdsPerEmail(user.email)

    val suppliers = supplierIds.map { supplierId =>
      SupplierUpdate(
        id = Some(supplierId),
        merchantId = Some(user.merchantId),
        name = Some(randomWords),
        contact = Some(s"Mr $randomWords"),
        address = Some(randomWords),
        secondaryAddress = Some(randomWords),
        email = Some(s"$supplierId@supplier.paytouch.io"),
        phoneNumber = Some(s"+$randomNumericString"),
        secondaryPhoneNumber = Some(s"+$randomNumericString"),
        accountNumber = Some(randomNumericString),
        notes = Some(randomWords(n = 10, allCapitalized = false)),
        deletedAt = None,
      )
    }

    supplierDao.bulkUpsert(suppliers).extractRecords
  }
}
