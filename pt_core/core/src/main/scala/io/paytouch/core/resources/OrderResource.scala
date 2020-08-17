package io.paytouch.core.resources

import java.time.{ LocalDateTime, ZonedDateTime }
import java.util.UUID

import akka.http.scaladsl.server.Route

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.{ ExposedName, View }
import io.paytouch.core.expansions.OrderExpansions
import io.paytouch.core.filters.OrderFilters
import io.paytouch.core.services._

trait OrderResource extends JsonResource {
  def orderService: OrderService
  def onlineOrderAttributeService: OnlineOrderAttributeService

  val orderRoutes: Route =
    concat(
      path("orders.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters(
              "location_id".as[UUID].?,
              "customer_id".as[UUID].?,
              "table_id".as[UUID].?,
              "payment_type".as[OrderPaymentType].?,
              "view".as[View].?,
              "from".as[LocalDateTime].?,
              "to".as[LocalDateTime].?,
              "q".?,
              "invoice".as[Boolean].?(false),
              "is_open".as[Boolean].?,
              "status[]".as[Seq[OrderStatus]].?,
              "acceptance_status".as[AcceptanceStatus].?,
              "payment_status".as[PaymentStatus].?,
              "source".as[Source].?,
              "source[]".as[Seq[Source]].?,
              "delivery_provider".as[DeliveryProvider].?,
              "delivery_provider[]".as[Seq[DeliveryProvider]].?,
              "updated_since".as[ZonedDateTime].?,
            ) {
              case (
                    locationId,
                    customerId,
                    tableId,
                    paymentType,
                    view,
                    from,
                    to,
                    q,
                    isInvoice,
                    isOpen,
                    orderStatus,
                    acceptanceStatus,
                    paymentStatus,
                    source,
                    sources,
                    deliveryProvider,
                    deliveryProviders,
                    updatedSince,
                  ) =>
                expandParameters(
                  "gift_card_passes",
                  "payment_transactions",
                  "sales_summary",
                  "type_summary",
                  "tickets",
                )(
                  OrderExpansions.withoutOrderItems,
                ) { expansions =>
                  userOrAppAuthenticate { implicit user =>
                    val overriddenOrderStatus: Option[Seq[OrderStatus]] =
                      isOpen.map {
                        case true  => OrderStatus.open.toSeq
                        case false => OrderStatus.notOpen.toSeq
                      }

                    val sourcesOrDeliveryProviders = {
                      val ss = sources.combineWithOne(source)
                      val dp = deliveryProviders.combineWithOne(deliveryProvider)

                      ss.map(_.asLeft).orElse(dp.map(_.asRight))
                    }

                    val filters = OrderFilters.withAccessibleLocations(
                      locationId,
                      customerId,
                      tableId,
                      paymentType,
                      view,
                      from,
                      to,
                      q,
                      isInvoice,
                      overriddenOrderStatus.orElse(orderStatus),
                      acceptanceStatus,
                      paymentStatus,
                      sourcesOrDeliveryProviders,
                      updatedSince,
                    )

                    onSuccess(orderService.findAllWithMetadata(filters)(expansions)) { (orders, metadata) =>
                      completeAsApiResponseWithMetadata(orders, metadata)
                    }
                  }
                }
            }
          }
        }
      },
      path("orders.get") {
        get {
          expandParameters("gift_card_passes", "payment_transactions", "loyalty_points", "tickets")(
            OrderExpansions.withOrderItems,
          ) { expansions =>
            concat(
              parameter("order_id".as[UUID]) { orderId =>
                userOrAppAuthenticate { implicit user =>
                  onSuccess(orderService.findOpenById(orderId)(expansions))(result => completeAsOptApiResponse(result))
                }
              },
              parameters("delivery_provider".as[DeliveryProvider], "delivery_provider_id".as[String]) {
                case (deliveryProvider, deliveryProviderId) =>
                  userOrAppAuthenticate { implicit user =>
                    onSuccess(
                      orderService
                        .findByDeliveryProviderId(deliveryProvider, deliveryProviderId)(expansions),
                    )(result => completeAsOptApiResponse(result))
                  }
              },
            )
          }
        }
      },
      path("orders.sync") {
        post {
          parameter("order_id".as[UUID]) { id =>
            entity(as[OrderUpsertion]) { upsertion =>
              userOrAppAuthenticate { implicit user =>
                lockEntity(ExposedName.Order, id) {
                  onSuccess(orderService.syncById(id, upsertion))(result => completeAsApiResponse(result))
                }
              }
            }
          }
        }
      },
      path("orders.validated_sync") {
        post {
          parameter("order_id".as[UUID]) { id =>
            entity(as[OrderUpsertion]) { upsertion =>
              userOrAppAuthenticate { implicit user =>
                lockEntity(ExposedName.Order, id) {
                  onSuccess(orderService.validatedSyncById(id, upsertion))(result => completeAsApiResponse(result))
                }
              }
            }
          }
        }
      },
      path("orders.send_receipt") {
        post {
          parameters("order_id".as[UUID], "payment_transaction_id".as[UUID].?) { (id, paymentTransactionId) =>
            entity(as[SendReceiptData]) { sendReceiptData =>
              authenticate { implicit user =>
                onSuccess(orderService.sendReceipt(id, paymentTransactionId, sendReceiptData))(completeAsEmptyResponse)
              }
            }
          }
        }
      },
      path("orders.store_payment_transaction") {
        post {
          parameters("order_id".as[UUID]) { id =>
            entity(as[OrderService.PaymentTransactionUpsertion]) { paymentTransactionUpsertion =>
              appAuthenticate { implicit user =>
                onSuccess(orderService.storePaymentTransaction(id, paymentTransactionUpsertion))(result =>
                  completeAsApiResponse(result),
                )
              }
            }
          }
        }
      },
      path("orders.accept") {
        post {
          parameters("order_id".as[UUID]) { id =>
            entity(as[OrderAcception]) { orderAcception =>
              authenticate { implicit user =>
                onlineOrderAttributeService
                  .acceptAndThenSendAcceptanceStatusMessage(id, orderAcception)
                  .pipe(onSuccess(_)(completeAsValidatedOptApiResponse))
              }
            }
          }
        }
      },
      path("orders.reject") {
        post {
          parameters("order_id".as[UUID]) { id =>
            entity(as[OrderRejection]) { orderRejection =>
              authenticate { implicit user =>
                onSuccess(onlineOrderAttributeService.reject(id, orderRejection)) { result =>
                  completeAsValidatedOptApiResponse(result)
                }
              }
            }
          }
        }
      },
    )
}
