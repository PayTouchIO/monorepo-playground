package io.paytouch.core.validators

import java.util.UUID

import cats.data.OptionT
import cats.implicits._
import io.paytouch.core.data.daos.{ Daos, OnlineOrderAttributeDao }
import io.paytouch.core.data.model.enums.AcceptanceStatus
import io.paytouch.core.data.model.enums.AcceptanceStatus._
import io.paytouch.core.data.model.{ OnlineOrderAttributeRecord, OrderRecord }
import io.paytouch.core.entities.UserContext
import io.paytouch.core.errors.{
  InvalidAcceptanceStatusChange,
  InvalidOnlineOrderAttributeIds,
  NonAccessibleOnlineOrderAttributeIds,
  NonOnlineOrder,
}
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple._

import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

class OnlineOrderAttributeValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[OnlineOrderAttributeRecord] {

  type Record = OnlineOrderAttributeRecord
  type Dao = OnlineOrderAttributeDao

  protected val dao = daos.onlineOrderAttributeDao
  val validationErrorF = InvalidOnlineOrderAttributeIds(_)
  val accessErrorF = NonAccessibleOnlineOrderAttributeIds(_)

  val orderValidator = new OrderValidator

  def validateAccept(orderId: UUID)(implicit user: UserContext): Future[ErrorsOr[Option[OrderRecord]]] =
    validateTransition(orderId, Accepted)

  def validateReject(orderId: UUID)(implicit user: UserContext): Future[ErrorsOr[Option[OrderRecord]]] =
    validateTransition(orderId, Rejected)

  private def validateTransition(
      orderId: UUID,
      targetStatus: AcceptanceStatus,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[OrderRecord]]] =
    for {
      order <- orderValidator.accessOneById(orderId)
      onlineOrderAttributeId = order.toOption.flatMap(_.onlineOrderAttributeId)
      orderOnlineAttribute <- validateStatusTransition(orderId, onlineOrderAttributeId, targetStatus)
    } yield Multiple.combine(order, orderOnlineAttribute) { case (o, _) => Some(o) }

  private def validateStatusTransition(
      orderId: UUID,
      orderAttributeId: Option[UUID],
      targetStatus: AcceptanceStatus,
    ): Future[ErrorsOr[Option[OnlineOrderAttributeRecord]]] = {
    val optT = for {
      oaId <- OptionT.fromOption[Future](orderAttributeId)
      orderAttribute <- OptionT(dao.findById(oaId))
    } yield orderAttribute

    val validCurrentStatuses = Seq(Pending, targetStatus)
    optT.value.map {
      case Some(orderAttribute) if validCurrentStatuses.contains(orderAttribute.acceptanceStatus) =>
        Multiple.successOpt(orderAttribute)
      case Some(orderAttribute) =>
        Multiple.failure(InvalidAcceptanceStatusChange(orderId, orderAttribute, targetStatus))
      case None => Multiple.failure(NonOnlineOrder(orderId))
    }
  }
}
