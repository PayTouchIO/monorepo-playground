package io.paytouch.core.services

import java.util.UUID

import akka.actor.ActorRef
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.conversions.GroupConversions
import io.paytouch.core.data.daos.{ Daos, GroupDao }
import io.paytouch.core.data.model.upsertions.{ GroupUpsertion => GroupUpsertionModel }
import io.paytouch.core.data.model.{ GroupRecord, GroupUpdate => GroupUpdateModel }
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities.{ Group => GroupEntity, GroupUpdate => GroupUpdateEntity, _ }
import io.paytouch.core.expansions.GroupExpansions
import io.paytouch.core.filters.GroupFilters
import io.paytouch.core.services.features._
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.GroupValidator
import io.paytouch.core.withTag

import scala.concurrent._
import io.paytouch.core.RichMap
import io.paytouch.core.utils.Multiple

class GroupService(
    val customerGroupService: CustomerGroupService,
    val customerMerchantService: CustomerMerchantService,
    val eventTracker: ActorRef withTag EventTracker,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends GroupConversions
       with CreateAndUpdateFeature
       with DeleteFeature
       with FindByIdFeature
       with FindAllFeature {

  type Creation = GroupCreation
  type Dao = GroupDao
  type Entity = GroupEntity
  type Expansions = GroupExpansions
  type Filters = GroupFilters
  type Model = GroupUpsertionModel
  type Record = GroupRecord
  type Update = GroupUpdateEntity
  type Validator = GroupValidator

  protected val dao = daos.groupDao
  protected val validator = new GroupValidator
  val defaultFilters = GroupFilters()

  val classShortName = ExposedName.Group

  def enrich(groups: Seq[Record], filters: Filters)(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] =
    for {
      customersPerGroup <- getOptionalCustomersPerGroup(groups, filters)(e.withCustomers)
      customersCountPerGroup <- getOptionalCustomersCountPerGroup(groups)(e.withCustomersCount)
      revenuesPerGroup <- getOptionalRevenuesPerGroup(groups, filters)(e.withRevenue)
      visitsPerGroup <- getOptionalVisitsPerGroup(groups, filters)(e.withVisits)
    } yield fromRecordsAndOptionsToEntities(
      groups,
      customersPerGroup,
      customersCountPerGroup,
      revenuesPerGroup,
      visitsPerGroup,
    )

  private def getOptionalCustomersPerGroup(
      groups: Seq[Record],
      f: Filters,
    )(
      withCustomers: Boolean,
    )(implicit
      userContext: UserContext,
    ): Future[DataSeqByRecord[CustomerMerchant]] =
    if (withCustomers) customerMerchantService.findByGroupIds(groups.map(_.id), f.locationId, f.from, f.to).map {
      recordsPerGroup => Some(recordsPerGroup.mapKeysToRecords(groups))
    }
    else Future.successful(None)

  private def getOptionalCustomersCountPerGroup(
      groups: Seq[Record],
    )(
      withCustomersCount: Boolean,
    )(implicit
      userContext: UserContext,
    ): Future[DataByRecord[Int]] =
    if (withCustomersCount) customerGroupService.countAllByGroupIds(groups.map(_.id)).map { recordsPerGroup =>
      Some(recordsPerGroup.mapKeysToRecords(groups))
    }
    else Future.successful(None)

  private def getOptionalRevenuesPerGroup(
      groups: Seq[Record],
      f: Filters,
    )(
      withRevenues: Boolean,
    )(implicit
      userContext: UserContext,
    ): Future[DataSeqByRecord[MonetaryAmount]] =
    if (withRevenues) customerMerchantService.getRevenueByGroupIds(groups.map(_.id), f.locationId, f.from, f.to).map {
      recordsPerGroup => Some(recordsPerGroup.mapKeysToRecords(groups))
    }
    else Future.successful(None)

  private def getOptionalVisitsPerGroup(
      groups: Seq[Record],
      f: Filters,
    )(
      withVisits: Boolean,
    )(implicit
      userContext: UserContext,
    ): Future[DataByRecord[Int]] =
    if (withVisits) customerMerchantService.getTotalVisitsPerGroup(groups.map(_.id), f.locationId, f.from, f.to).map {
      recordsPerGroup => Some(recordsPerGroup.mapKeysToRecords(groups))
    }
    else Future.successful(None)

  protected def convertToUpsertionModel(
      groupId: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Model]] =
    for {
      group <- convertToGroupUpdate(groupId, update)
      customerGroups <- customerGroupService.convertToCustomerGroupUpdates(update, group.toOption)
    } yield Multiple.combine(group, customerGroups)(GroupUpsertionModel)

  private def convertToGroupUpdate(
      groupId: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[GroupUpdateModel]] =
    Future.successful {
      Multiple.success(fromUpsertionToUpdate(groupId, update))
    }

}
