package io.paytouch.core.validators

import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.core.data.daos.{ Daos, OrderItemDao }
import io.paytouch.core.data.model.OrderItemRecord
import io.paytouch.core.entities._
import io.paytouch.core.errors.{ InvalidOrderItemIds, InvalidOrderOrderItemsAssociation, NonAccessibleOrderItemIds }
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

class OrderItemValidator(implicit val ec: ExecutionContext, val daos: Daos) extends DefaultValidator[OrderItemRecord] {

  type Dao = OrderItemDao
  type Record = OrderItemRecord

  protected val dao = daos.orderItemDao
  val validationErrorF = InvalidOrderItemIds(_)
  val accessErrorF = NonAccessibleOrderItemIds(_)

  def accessByIdsAndOrderId(
      ids: Seq[UUID],
      orderId: UUID,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[Record]]] =
    accessByIds(ids).map {
      case Valid(orderItems) if orderItems.forall(_.orderId == orderId) => Multiple.success(orderItems)
      case Valid(orderItems) =>
        val invalidOrderItemIds = orderItems.filterNot(_.orderId == orderId).map(_.id)
        Multiple.failure(InvalidOrderOrderItemsAssociation(invalidOrderItemIds, orderId))
      case i @ Invalid(_) => i
    }

}
