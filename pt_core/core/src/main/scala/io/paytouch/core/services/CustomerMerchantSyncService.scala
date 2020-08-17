package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.conversions.CustomerMerchantConversions
import io.paytouch.core.data._
import io.paytouch.core.data.daos._
import io.paytouch.core.data.model.LoyaltyProgramRecord
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.{ CustomerSource, MerchantSetupSteps }
import io.paytouch.core.expansions.CustomerExpansions
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.PaytouchLogger
import io.paytouch.core.utils.ResultType
import io.paytouch.core.validators.CustomerMerchantValidator

class CustomerMerchantSyncService(
    customerMerchantService: => CustomerMerchantService,
    val loyaltyMembershipService: LoyaltyMembershipService,
    val loyaltyProgramService: LoyaltyProgramService,
    val globalCustomerService: GlobalCustomerService,
    val setupStepService: SetupStepService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends CustomerMerchantConversions {
  type Dao = CustomerMerchantDao
  type Entity = CustomerMerchant
  type Update = CustomerMerchantUpsertion
  type Model = model.upsertions.CustomerUpsertion

  protected val dao = daos.customerMerchantDao
  protected val validator = new CustomerMerchantValidator

  def validateSyncAndConvert(
      id: Option[UUID],
      update: Update,
      source: CustomerSource,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Model]]] =
    validator
      .validateSync(id, update)
      .flatMapTraverse(_ => convertToCustomerUpsertionModel(id, update, source).map(Some(_)))

  def convertAndUpsert(
      id: Option[UUID],
      update: Update,
      loyaltyProgram: Option[LoyaltyProgramRecord],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Entity)] = {
    implicit val u: MerchantContext = user.toMerchantContext
    val source = customerSourceFromContextSource(user.source)

    for {
      upsertionModel <- convertToCustomerUpsertionModel(id, update, source)
      result <- upsert(upsertionModel, loyaltyProgram)
    } yield result
  }

  def upsert(
      upsertion: Model,
      loyaltyProgram: Option[LoyaltyProgramRecord],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Entity)] = {
    implicit val u: MerchantContext = user.toMerchantContext

    for {
      (resultType, record) <- dao.upsert(upsertion)
      _ <- setupStepService.simpleCheckStepCompletion(user.merchantId, MerchantSetupSteps.ImportCustomers)
      _ <- loyaltyMembershipService.enrolViaMerchant(record.id, loyaltyProgram)
      filters = customerMerchantService.defaultFilters
      expansions = CustomerExpansions.all
      entity <- customerMerchantService.enrich(record, filters)(expansions)
    } yield (resultType, entity)
  }

  private def convertToCustomerUpsertionModel(
      id: Option[UUID],
      update: Update,
      source: CustomerSource,
    )(implicit
      user: UserContext,
    ): Future[Model] =
    globalCustomerService
      .convertToGlobalCustomerUpdate(id, update.email)
      .map { globalCustomer =>
        model
          .upsertions
          .CustomerUpsertion(
            globalCustomer,
            toCustomerMerchantUpdate(update, globalCustomer, source)(user.toMerchantContext),
          )
      }
}
