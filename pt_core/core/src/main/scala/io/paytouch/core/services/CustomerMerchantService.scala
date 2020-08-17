package io.paytouch.core.services

import java.time._
import java.util.UUID

import scala.concurrent._

import akka.actor.ActorRef

import cats.data._
import cats.implicits._

import io.paytouch.core._
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.conversions.CustomerMerchantConversions
import io.paytouch.core.data.daos._
import io.paytouch.core.data.model._
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums._
import io.paytouch.core.expansions.CustomerExpansions
import io.paytouch.core.filters._
import io.paytouch.core.services.features._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.CustomerMerchantValidator

class CustomerMerchantService(
    val customerGroupService: CustomerGroupService,
    val customerLocationService: CustomerLocationService,
    val loyaltyMembershipService: LoyaltyMembershipService,
    val eventTracker: ActorRef withTag EventTracker,
    val globalCustomerService: GlobalCustomerService,
    val locationService: LocationService,
    val loyaltyProgramService: LoyaltyProgramService,
    orderService: => OrderService,
    val setupStepService: SetupStepService,
    val syncService: CustomerMerchantSyncService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends CustomerMerchantConversions
       with FindAllFeature
       with FindByIdFeature
       with DeleteFeature {

  type Dao = CustomerMerchantDao
  type Entity = CustomerMerchant
  type Expansions = CustomerExpansions
  type Filters = CustomerFilters
  type Model = upsertions.CustomerUpsertion
  type Record = CustomerMerchantRecord
  type Update = CustomerMerchantUpsertion
  type Validator = CustomerMerchantValidator

  protected val dao = daos.customerMerchantDao
  protected val validator = new CustomerMerchantValidator
  val defaultFilters = CustomerFilters()

  val classShortName = ExposedName.Customer

  def findByCustomerIds(
      customerIds: Seq[UUID],
      f: Filters = defaultFilters,
    )(
      e: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] =
    dao.findByCustomerIdsAndMerchantId(customerIds, user.merchantId).flatMap(records => enrich(records, f)(e))

  def enrich(
      customerMerchants: Seq[Record],
      f: Filters,
    )(
      e: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] = {
    implicit val u: MerchantContext = user.toMerchantContext
    val customerIds = customerMerchants.map(_.customerId)
    for {
      globalCustomers <- globalCustomerService.findByIds(customerIds)
      (totalSpendPerCustomer, totalVisitsPerCustomer) <- getOptionalTotalSpendAndVisitsPerCustomer(
        customerIds,
        f.locationId,
      )(e.withSpend, e.withVisits)
      locationsPerCustomer <- getOptionalLocationsPerCustomer(customerIds)(e.withLocations)
      loyaltyProgramsPerCustomer <- getOptionalLoyaltyProgramsPerCustomer(customerIds)(e.withLoyaltyPrograms)
      loyaltyMembershipsPerCustomer <- getOptionalLoyaltyMembershipsPerCustomer(
        customerIds,
        f.loyaltyProgramId,
        f.updatedSince,
      )(e.withLoyaltyMemberships)
      avgTipsPerCustomerMerchant <- getOptionalAvgTipsPerCustomer(customerIds, f.locationId)(e.withAvgTips)
      billingDetailsPerCustomerMerchant <- getOptionalBillingDetailsPerCustomer(customerMerchants)(e.withBillingDetails)
    } yield fromRecordsAndOptionsToEntities(
      customerMerchants,
      globalCustomers,
      totalVisitsPerCustomer,
      totalSpendPerCustomer,
      locationsPerCustomer,
      loyaltyProgramsPerCustomer,
      loyaltyMembershipsPerCustomer,
      avgTipsPerCustomerMerchant,
      billingDetailsPerCustomerMerchant,
    )
  }

  private def getOptionalTotalSpendAndVisitsPerCustomer(
      customerIds: Seq[UUID],
      locationId: Option[UUID],
    )(
      withSpend: Boolean,
      withVisits: Boolean,
    )(implicit
      user: UserContext,
    ): Future[(Option[Map[UUID, MonetaryAmount]], Option[Map[UUID, Int]])] =
    if (withSpend || withVisits)
      customerLocationService.getTotalsPerCustomer(customerIds, locationId).map { totals =>
        val totalSpend = if (withSpend) Some(totals.transform((_, v) => v.totalSpend)) else None
        val totalVisits = if (withVisits) Some(totals.transform((_, v) => v.totalVisits)) else None
        (totalSpend, totalVisits)
      }
    else Future.successful((None, None))

  private def getOptionalAvgTipsPerCustomer(
      customerIds: Seq[UUID],
      locationId: Option[UUID],
    )(
      withAvgTips: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[UUID, Seq[MonetaryAmount]]]] =
    if (withAvgTips)
      orderService.getAvgTipsPerCustomer(customerIds, locationId).map(Some(_))
    else
      Future.successful(None)

  private def getOptionalBillingDetailsPerCustomer(
      customers: Seq[Record],
    )(
      withBillingDetails: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[UUID, BillingDetails]]] =
    Future.successful {
      if (withBillingDetails)
        customers
          .flatMap(customer => customer.billingDetails.map(customer.id -> _))
          .toMap
          .some
      else
        None
    }

  private def getOptionalLocationsPerCustomer(
      customerIds: Seq[UUID],
    )(
      withLocations: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[UUID, Seq[Location]]]] =
    if (withLocations) locationService.findAllByCustomerIds(customerIds).map(Some(_))
    else Future.successful(None)

  private def getOptionalLoyaltyProgramsPerCustomer(
      customerIds: Seq[UUID],
    )(
      withLoyaltyPrograms: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[UUID, Seq[LoyaltyProgram]]]] =
    if (withLoyaltyPrograms) loyaltyProgramService.findAllByCustomerIds(customerIds).map(Some(_))
    else Future.successful(None)

  private def getOptionalLoyaltyMembershipsPerCustomer(
      customerIds: Seq[UUID],
      loyaltyProgramId: Option[UUID],
      updatedSince: Option[ZonedDateTime],
    )(
      withLoyaltyMemberships: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[UUID, Seq[LoyaltyMembership]]]] =
    if (withLoyaltyMemberships)
      loyaltyMembershipService
        .findAllByCustomerIds(customerIds, LoyaltyMembershipFilter(loyaltyProgramId, updatedSince))
        .map(statuses => Some(statuses.groupBy(_.customerId)))
    else
      Future.successful(None)

  def findByGroupIds(
      groupIds: Seq[UUID],
      locationId: Option[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    )(implicit
      userContext: UserContext,
    ): Future[Map[UUID, Seq[CustomerMerchant]]] =
    for {
      customerGroups <- customerGroupService.findAllByGroupIds(groupIds, locationId, from, to)
      customerMerchants <- findByCustomerIds(customerGroups.map(_.customerId))(CustomerExpansions.empty)
    } yield groupCustomerMerchantsPerGroup(customerGroups, customerMerchants)

  def getRevenueByGroupIds(
      groupIds: Seq[UUID],
      locationId: Option[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    )(implicit
      userContext: UserContext,
    ) =
    for {
      customerGroups <- customerGroupService.findAllByGroupIds(groupIds, locationId, from, to)
      revenuePerGroup <-
        customerLocationService
          .getTotalSpendPerGroup(groupIds, customerGroups.map(_.customerId), locationId)
    } yield revenuePerGroup

  def getTotalVisitsPerGroup(
      groupIds: Seq[UUID],
      locationId: Option[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    )(implicit
      userContext: UserContext,
    ) =
    for {
      customerGroups <- customerGroupService.findAllByGroupIds(groupIds, locationId, from, to)
      visitsPerGroup <-
        customerLocationService
          .getTotalVisitsPerGroup(groupIds, customerGroups.map(_.customerId), locationId)
    } yield visitsPerGroup

  def findByLoyaltyMembershipsLookupId(lookupId: String)(implicit user: UserContext): Future[Option[Entity]] =
    (for {
      loyaltyMembership <- OptionT(loyaltyMembershipService.findByLookupId(lookupId))
      customerId = loyaltyMembership.customerId
      filters = CustomerFilters(loyaltyProgramId = Some(loyaltyMembership.loyaltyProgramId))
      expansions = CustomerExpansions.all.copy(withLocations = false)
      entity <- OptionT(findById(customerId, filters)(expansions))
      filteredLoyaltyMemberships = entity.loyaltyMemberships.map(_.filter(_.lookupId == lookupId))
      updatedEntity = entity.copy(
        loyaltyMemberships = filteredLoyaltyMemberships,
        loyaltyStatuses = filteredLoyaltyMemberships,
      )
    } yield updatedEntity).value

  def findByCustomerId(customerId: UUID)(implicit merchant: MerchantContext): Future[Option[Entity]] =
    dao.findByCustomerIdsAndMerchantId(Seq(customerId), merchant.id).map(_.map(fromRecordToEntity).headOption)

  def create(creation: Update)(implicit user: UserContext): Future[ErrorsOr[Result[Entity]]] =
    createOrUpdate(None, creation)

  def update(id: UUID, update: Update)(implicit user: UserContext): Future[ErrorsOr[Result[Entity]]] =
    createOrUpdate(Some(id), update)

  private def createOrUpdate(
      id: Option[UUID],
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[Entity]]] =
    validator.validateCreateOrUpdate(id, update).flatMapTraverse {
      case (improvedUpdate, loyaltyProgram) =>
        syncService.convertAndUpsert(id, improvedUpdate, loyaltyProgram)
    }

  def sync(id: UUID, update: Update)(implicit user: UserContext): Future[ErrorsOr[Result[Entity]]] =
    validator
      .validateSync(Some(id), update)
      .flatMapTraverse {
        case (improvedUpdate, loyaltyProgram) =>
          syncService.convertAndUpsert(Some(id), improvedUpdate, loyaltyProgram)
      }

  def markAsUpdatedById(id: UUID) = dao.markAsUpdatedById(id)
}
