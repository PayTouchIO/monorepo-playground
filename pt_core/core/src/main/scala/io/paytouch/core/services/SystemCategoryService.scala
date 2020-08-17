package io.paytouch.core.services

import java.util.UUID

import akka.actor.ActorRef
import cats.data.Validated.{ Invalid, Valid }
import cats.implicits._
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.clients.paytouch.ordering.PtOrderingClient
import io.paytouch.core.data.daos._
import io.paytouch.core.entities.{ SystemCategoryCreation, SystemCategoryUpdate, UserContext }
import io.paytouch.core.utils._

import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.SystemCategoryValidator
import io.paytouch.core.withTag

import scala.concurrent._

class SystemCategoryService(
    val availabilityService: CategoryAvailabilityService,
    val catalogService: CatalogService,
    val locationAvailabilityService: CategoryLocationAvailabilityService,
    val eventTracker: ActorRef withTag EventTracker,
    val categoryLocationService: CategoryLocationService,
    val imageUploadService: ImageUploadService,
    val productCategoryService: ProductCategoryService,
    val ptOrderingClient: PtOrderingClient,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends GenericCategoryService {

  type Dao = SystemCategoryDao
  type State = Unit
  type Validator = SystemCategoryValidator

  protected val dao = daos.systemCategoryDao
  protected val validator = new SystemCategoryValidator(ptOrderingClient)

  def create(
      id: UUID,
      creation: SystemCategoryCreation,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[Entity]]] =
    catalogService.findDefaultMenuCatalog().flatMap {
      case Valid(catalog) =>
        create(id, SystemCategoryCreation.convert(creation).copy(catalogId = catalog.id.some))
      case Invalid(errors) => Future.successful(UpsertionResult.invalid(errors))
    }

  def update(
      id: UUID,
      categoryUpdate: SystemCategoryUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[Entity]]] =
    update(id, SystemCategoryUpdate.convert(categoryUpdate))

  protected def saveCreationState(id: UUID, creation: Creation)(implicit user: UserContext): Future[Option[State]] =
    Future.successful(None)

  protected def saveCurrentState(record: Record)(implicit user: UserContext): Future[State] =
    Future.unit

  protected def processChangeOfState(
      state: Option[State],
      update: Update,
      resultType: ResultType,
      entity: Entity,
    )(implicit
      user: UserContext,
    ) =
    Future.unit
}
