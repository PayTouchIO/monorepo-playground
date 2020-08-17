package io.paytouch.core.services

import java.time.LocalDateTime
import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.conversions.CustomerGroupConversions
import io.paytouch.core.data._
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities._
import io.paytouch.core.utils.PaytouchLogger
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple

import io.paytouch.core.validators.CustomerMerchantValidator

class CustomerGroupService(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends CustomerGroupConversions {
  protected val dao = daos.customerGroupDao
  val customerMerchantValidator = new CustomerMerchantValidator

  def convertToCustomerGroupUpdates(
      update: GroupUpdate,
      groupUpdate: Option[model.GroupUpdate],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[model.CustomerGroupUpdate]]]] =
    update.customerIds match {
      case Some(customerIds) =>
        customerMerchantValidator.validateByIds(customerIds).mapNested { customers =>
          val groupId = groupUpdate.flatMap(_.id)
          val customerGroupUpdates = fromCustomerIdsToCustomerGroupUpdates(groupId, customers.map(_.id))
          Some(customerGroupUpdates)
        }
      case None => Future.successful(Multiple.empty)
    }

  def findAllByGroupIds(
      groupIds: Seq[UUID],
      locationId: Option[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    )(implicit
      userContext: UserContext,
    ): Future[Seq[model.CustomerGroupRecord]] =
    dao.findByGroupIdsAndMerchantId(
      groupIds,
      userContext.merchantId,
      userContext.accessibleLocations(locationId),
      from,
      to,
    )

  def countAllByGroupIds(groupIds: Seq[UUID])(implicit userContext: UserContext): Future[Map[UUID, Int]] =
    dao.countByGroupIdsAndMerchantId(groupIds, userContext.merchantId)
}
