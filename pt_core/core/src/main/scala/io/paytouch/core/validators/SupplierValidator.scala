package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ Daos, SupplierDao }
import io.paytouch.core.data.model.SupplierRecord
import io.paytouch.core.errors.{ InvalidSupplierIds, NonAccessibleSupplierIds }
import io.paytouch.core.validators.features.{ DefaultValidator, EmailValidator }

import scala.concurrent.ExecutionContext

class SupplierValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[SupplierRecord]
       with EmailValidator {

  type Record = SupplierRecord
  type Dao = SupplierDao

  protected val dao = daos.supplierDao
  val validationErrorF = InvalidSupplierIds(_)
  val accessErrorF = NonAccessibleSupplierIds(_)
}
