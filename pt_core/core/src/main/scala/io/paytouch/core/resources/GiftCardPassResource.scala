package io.paytouch.core.resources

import java.time.LocalDateTime
import java.util.UUID

import scala.concurrent._

import akka.http.scaladsl.server._

import cats.implicits._

import io.paytouch._

import io.paytouch.core.entities._
import io.paytouch.core.expansions.GiftCardPassExpansions
import io.paytouch.core.filters.GiftCardPassSalesSummaryFilters
import io.paytouch.core.services._

trait GiftCardPassResource extends JsonResource {
  import GiftCardPassService._

  def giftCardPassService: GiftCardPassService

  val giftCardPassRoutes: Route =
    concat(
      path("gift_card_passes.charge") {
        post {
          parameters(
            "gift_card_pass_id".as[UUID],
            "amount".as[BigDecimal],
          )(decreaseBalance)
        }
      },
      path("gift_card_passes.bulk_charge") {
        post {
          parameters("order_id".as[OrderId]) { orderId =>
            entity(as[Seq[GiftCardPassCharge]]) { bulkCharge =>
              userOrAppAuthenticate { implicit userOrApp =>
                onSuccess(giftCardPassService.decreaseBalance(orderId, bulkCharge))(order =>
                  completeAsValidatedOptApiResponse(order),
                )
              }
            }
          }
        }
      },
      path("gift_card_passes.get") {
        get {
          parameter("gift_card_pass_id".as[UUID])(id => getGiftCardPass(giftCardPassService.findById(id)(_)(_)))
        }
      },
      path("gift_card_passes.lookup") {
        get {
          import io.paytouch.GiftCardPass.OnlineCode

          concat(
            parameter("lookup_id".as[String]) { lookupId =>
              getGiftCardPass(giftCardPassService.findByLookupId(lookupId)(_)(_))
            },
            parameter("online_code".as[OnlineCode.Raw]) { code =>
              getGiftCardPass(giftCardPassService.findByOnlineCode(code)(_)(_))
            },
          )
        }
      },
      path("gift_card_passes.refund") {
        post {
          parameters("gift_card_pass_id".as[UUID], "amount".as[BigDecimal]) { (id, amount) =>
            decreaseBalance(id, -amount)
          }
        }
      },
      path("gift_card_passes.sales_summary") {
        get {
          parameters("location_id".as[UUID].?, "from".as[LocalDateTime].?, "to".as[LocalDateTime].?) {
            case (locationId, from, to) =>
              authenticate { implicit user =>
                val filters = GiftCardPassSalesSummaryFilters(locationId, from, to)
                onSuccess(giftCardPassService.computeSalesSummary(filters)) { result =>
                  completeAsApiResponse(result, "gift_card_pass_sales_summary")
                }
              }
          }
        }
      },
      path("gift_card_passes.send_receipt") {
        post {
          parameters("order_item_id".as[UUID]) { id =>
            entity(as[SendReceiptData]) { sendReceiptData =>
              authenticate { implicit user =>
                onSuccess(giftCardPassService.sendReceipt(id, sendReceiptData)) { result =>
                  completeAsEmptyResponse(result)
                }
              }
            }
          }
        }
      },
    )

  private def getGiftCardPass(
      f: (GiftCardPassExpansions, UserContext) => Future[Option[GiftCardPass]],
    ): RequestContext => Future[RouteResult] =
    userOrAppAuthenticate { implicit user =>
      expandParameters("transactions")(GiftCardPassExpansions.apply) { expansions =>
        onSuccess(f(expansions, user))(result => completeAsOptApiResponse(result))
      }
    }

  private def decreaseBalance(id: UUID, amount: BigDecimal): Route =
    authenticate { implicit user =>
      onSuccess(giftCardPassService.decreaseBalance(id, amount))(result => completeAsValidatedOptApiResponse(result))
    }
}
