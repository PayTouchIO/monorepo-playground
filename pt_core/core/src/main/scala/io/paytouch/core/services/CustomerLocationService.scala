package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.conversions.CustomerLocationConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ CustomerLocationRecord, CustomerLocationUpdate }
import io.paytouch.core.entities.{ CustomerTotals, MerchantContext, MonetaryAmount, UserContext }
import io.paytouch.core.utils.PaytouchLogger
import io.paytouch.core.validators.RecoveredOrderUpsertion

import scala.concurrent._

class CustomerLocationService(
    customerMerchantService: => CustomerMerchantService,
    locationService: => LocationService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends CustomerLocationConversions {

  protected val dao = daos.customerLocationDao

  def findAllByCustomerIds(customerIds: Seq[UUID])(implicit user: UserContext): Future[Seq[CustomerLocationRecord]] =
    dao.findByItemIdsAndLocationIds(customerIds, user.locationIds)

  def getTotalsPerCustomer(
      customerIds: Seq[UUID],
      locationId: Option[UUID],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, CustomerTotals]] = {
    implicit val merchant = user.toMerchantContext
    val locationIds = user.accessibleLocations(locationId)
    getTotalsPerCustomer(customerIds = customerIds, locationIds = locationIds)
  }

  def getTotalsPerCustomerForMerchant(
      customerIds: Seq[UUID],
      locationId: Option[UUID],
    )(implicit
      merchantContext: MerchantContext,
    ): Future[Map[UUID, CustomerTotals]] =
    for {
      allLocationIds <- locationService.findAll.map(_.map(_.id))
      locationIds = locationId.fold(allLocationIds)(lId => allLocationIds.intersect(Seq(lId)))
      totals <- getTotalsPerCustomer(customerIds = customerIds, locationIds = locationIds)
    } yield totals

  private def getTotalsPerCustomer(
      customerIds: Seq[UUID],
      locationIds: Seq[UUID],
    )(implicit
      merchantContext: MerchantContext,
    ): Future[Map[UUID, CustomerTotals]] =
    dao
      .getTotalsPerCustomer(customerIds = customerIds, locationIds = locationIds)
      .map(_.transform((_, v) => toCustomerLocation(v)))

  def getTotalSpendPerGroup(
      groupIds: Seq[UUID],
      customerIds: Seq[UUID],
      locationId: Option[UUID],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[MonetaryAmount]]] =
    dao.getTotalSpendPerGroup(
      groupIds = groupIds,
      customerIds = customerIds,
      locationIds = user.accessibleLocations(locationId),
    )

  def getTotalVisitsPerGroup(
      groupIds: Seq[UUID],
      customerIds: Seq[UUID],
      locationId: Option[UUID],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Int]] =
    dao.getTotalVisitsPerGroup(
      groupIds = groupIds,
      customerIds = customerIds,
      locationIds = user.accessibleLocations(locationId),
    )

  def recoverCustomerLocationUpdate(
      upsertion: RecoveredOrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Option[CustomerLocationUpdate]] =
    Future.successful {
      (upsertion.customerId, upsertion.locationId) match {
        case (Some(_), None) =>
          logger.recoverLog("Impossible to link customerId=$cId to invalid location.", upsertion)
          None
        case (None, _) =>
          None
        case (Some(cId), Some(locationId)) =>
          val update = CustomerLocationUpdate(
            id = None,
            merchantId = Some(user.merchantId),
            customerId = Some(cId),
            locationId = Some(locationId),
            totalVisits = None,
            totalSpendAmount = None,
          )
          Some(update)
      }
    }

  def updateSpendingActivity(
      locationId: Option[UUID],
      customerId: Option[UUID],
    )(implicit
      user: UserContext,
    ): Future[Option[CustomerLocationRecord]] = {
    val update = CustomerLocationUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      customerId = customerId,
      locationId = locationId,
      totalVisits = None,
      totalSpendAmount = None,
    )

    val result = for {
      _ <- update.locationId
      customerId <- update.customerId
    } yield for {
      cl <- dao.upsertSpendingActivityByCustomerIdAndLocationId(update)
      _ <- customerMerchantService.markAsUpdatedById(customerId)
    } yield cl

    result match {
      case Some(res) => res.map(r => Some(r))
      case None      => Future.successful(None)
    }
  }
}
