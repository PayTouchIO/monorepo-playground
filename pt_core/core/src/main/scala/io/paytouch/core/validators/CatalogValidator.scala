package io.paytouch.core.validators

import java.util.UUID

import cats.implicits._

import io.paytouch.core.clients.paytouch.ordering.PtOrderingClient
import io.paytouch.core.clients.paytouch.ordering.entities.{ IdsToCheck, IdsUsage }
import io.paytouch.core.data.daos.{ CatalogDao, Daos }
import io.paytouch.core.data.model.CatalogRecord
import io.paytouch.core.entities.{ ApiResponse, UserContext }
import io.paytouch.core.entities.enums.CatalogType
import io.paytouch.core.errors.{
  DefaultMenuDeletionIsNotAllowed,
  DeletionCatalogsInUse,
  InvalidCatalogIds,
  NonAccessibleCatalogIds,
}
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

class CatalogValidator(val ptOrderingClient: PtOrderingClient)(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[CatalogRecord] {

  type Record = CatalogRecord
  type Dao = CatalogDao

  protected val dao = daos.catalogDao
  val validationErrorF = InvalidCatalogIds(_)
  val accessErrorF = NonAccessibleCatalogIds(_)

  override def validateDeletion(ids: Seq[UUID])(implicit user: UserContext): Future[ErrorsOr[Seq[UUID]]] =
    filterValidByIds(ids).flatMap { records =>
      records.find(_.`type` == CatalogType.DefaultMenu) match {
        case Some(defaultMenu) =>
          Multiple.failure(DefaultMenuDeletionIsNotAllowed(defaultMenu.id)).pure[Future]
        case _ =>
          val idsToCheck = IdsToCheck(catalogIds = records.map(_.id))
          ptOrderingClient.idsCheckUsage(idsToCheck).map {
            case Left(error) => Multiple.failure(error)
            case Right(ApiResponse(usage, _)) if catalogIdsInUse(usage).nonEmpty =>
              Multiple.failure(DeletionCatalogsInUse(catalogIdsInUse(usage)))
            case _ => Multiple.success(ids)
          }
      }
    }

  private def catalogIdsInUse(idsUsage: IdsUsage): Seq[UUID] =
    idsUsage.accessible.catalogIds
}
