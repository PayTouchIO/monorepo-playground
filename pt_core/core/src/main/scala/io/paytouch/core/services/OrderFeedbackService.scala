package io.paytouch.core.services

import io.paytouch.core.conversions.OrderFeedbackConversions
import io.paytouch.core.data.daos.{ Daos, OrderFeedbackDao }
import io.paytouch.core.data.model.OrderFeedbackRecord
import io.paytouch.core.entities._
import io.paytouch.core.expansions.{ CustomerExpansions, OrderFeedbackExpansions }
import io.paytouch.core.filters.OrderFeedbackFilters
import io.paytouch.core.services.features.FindAllFeature

import scala.concurrent._

class OrderFeedbackService(
    val customerMerchantService: CustomerMerchantService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends OrderFeedbackConversions
       with FindAllFeature {

  type Dao = OrderFeedbackDao
  type Entity = OrderFeedback
  type Expansions = OrderFeedbackExpansions
  type Filters = OrderFeedbackFilters
  type Record = OrderFeedbackRecord

  protected val dao = daos.orderFeedbackDao

  def enrich(records: Seq[Record], f: Filters)(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] =
    for {
      customersPerOrderFeedback <- getOptionalCustomersPerOrderFeedback(records)(e.withCustomers)
    } yield fromRecordsAndOptionsToEntities(records, customersPerOrderFeedback)

  private def getOptionalCustomersPerOrderFeedback(
      orderFeedbacks: Seq[OrderFeedbackRecord],
    )(
      withCustomers: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[OrderFeedbackRecord, CustomerMerchant]]] =
    if (withCustomers) {
      val customerIds = orderFeedbacks.map(_.customerId)
      customerMerchantService.findByCustomerIds(customerIds)(CustomerExpansions.empty).map { customers =>
        val orderCustomers = customers.flatMap { customer =>
          val customerOrderFeedbacks = orderFeedbacks.filter(_.customerId == customer.id)
          customerOrderFeedbacks.map((_, customer))
        }.toMap
        Some(orderCustomers)
      }
    }
    else Future.successful(None)

}
