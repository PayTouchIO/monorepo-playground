package io.paytouch.core.services

import java.util.UUID

import akka.actor.ActorRef
import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.clients.paytouch.ordering.PtOrderingClient
import io.paytouch.core.data.daos._
import io.paytouch.core.data.model.CatalogRecord
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.MerchantSetupSteps
import io.paytouch.core.utils._

import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.CatalogCategoryValidator
import io.paytouch.core.withTag

import scala.concurrent._

class CatalogCategoryService(
    val availabilityService: CategoryAvailabilityService,
    val locationAvailabilityService: CategoryLocationAvailabilityService,
    val eventTracker: ActorRef withTag EventTracker,
    val categoryLocationService: CategoryLocationService,
    val imageUploadService: ImageUploadService,
    val productCategoryService: ProductCategoryService,
    val ptOrderingClient: PtOrderingClient,
    val setupStepService: SetupStepService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends GenericCategoryService {

  type Dao = CatalogCategoryDao
  type State = Unit
  type Validator = CatalogCategoryValidator

  protected val dao = daos.catalogCategoryDao
  protected val validator = new CatalogCategoryValidator(ptOrderingClient)

  def create(
      id: UUID,
      creation: CatalogCategoryCreation,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[Entity]]] =
    create(id, CatalogCategoryCreation.convert(creation))

  def update(
      id: UUID,
      categoryUpdate: CatalogCategoryUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[Entity]]] =
    update(id, CatalogCategoryUpdate.convert(categoryUpdate))

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
    setupStepService.checkStepCompletion(entity, MerchantSetupSteps.SetupMenus)
}
