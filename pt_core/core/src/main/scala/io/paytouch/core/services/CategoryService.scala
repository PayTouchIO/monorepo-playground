package io.paytouch.core.services

import java.util.UUID

import akka.actor.ActorRef
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.clients.paytouch.ordering.PtOrderingClient
import io.paytouch.core.data.daos._
import io.paytouch.core.entities.UserContext
import io.paytouch.core.entities.enums.MerchantSetupSteps
import io.paytouch.core.utils._
import io.paytouch.core.validators.CategoryValidator
import io.paytouch.core.withTag

import scala.concurrent.{ ExecutionContext, Future }

class CategoryService(
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

  type Dao = CategoryDao
  type State = Unit
  type Validator = CategoryValidator

  protected val dao = daos.categoryDao
  protected val validator = new CategoryValidator(ptOrderingClient)

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
