package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ Daos, ImportDao }
import io.paytouch.core.data.model.ImportRecord
import io.paytouch.core.errors.{ InvalidImportIds, NonAccessibleImportIds }
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent.ExecutionContext

class ImportValidator(implicit val ec: ExecutionContext, val daos: Daos) extends DefaultValidator[ImportRecord] {

  type Dao = ImportDao
  type Record = ImportRecord

  protected val dao = daos.importDao
  val validationErrorF = InvalidImportIds(_)
  val accessErrorF = NonAccessibleImportIds(_)
}
