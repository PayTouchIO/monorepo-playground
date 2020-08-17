package io.paytouch.core.reports.validators

import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.data.daos.ExportDao
import io.paytouch.core.reports.data.model.ExportRecord
import io.paytouch.core.reports.errors._
import io.paytouch.core.reports.filters.ReportFilters
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

class ExportValidator(implicit val ec: ExecutionContext, val daos: Daos) extends DefaultValidator[ExportRecord] {

  type Dao = ExportDao
  type Record = ExportRecord

  protected val dao = daos.exportDao
  val validationErrorF = InvalidExportIds(_)
  val accessErrorF = NonAccessibleExportIds(_)

  val filtersValidator = new FiltersValidator

  def validateDownloadBaseUrl(id: UUID)(implicit user: UserContext): Future[ErrorsOr[String]] =
    accessOneById(id).map {
      case Valid(record) if record.baseUrl.isDefined => Valid(record.baseUrl.get)
      case Valid(record)                             => Multiple.failure(ExportDownloadMissingBaseUrl(record))
      case i @ Invalid(_)                            => i
    }

  def validateFilters[Filters <: ReportFilters](filters: Filters)(implicit user: UserContext) =
    filtersValidator.validateFilters(filters)
}
