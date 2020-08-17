package io.paytouch.ordering.services

import java.util.UUID

import scala.concurrent._

import cats.data._
import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.ordering._
import io.paytouch.ordering.clients._
import io.paytouch.ordering.clients.paytouch.core.entities.{ Order, OrderUpsertion, PaymentTransactionUpsertion }
import io.paytouch.ordering.clients.paytouch.core.entities.enums.AcceptanceStatus
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.conversions.OrderConversions
import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.data.model.{ CartRecord, CartUpdate }
import io.paytouch.ordering.entities.{ GiftCardPassApplied, StoreContext, Cart => CartEntity }
import io.paytouch.ordering.entities.enums.CartStatus
import io.paytouch.ordering.errors.ClientError
import io.paytouch.ordering.errors.Error
import io.paytouch.ordering.UpsertionResult

class CartSyncService(
    cartService: => CartService,
    val coreClient: PtCoreClient,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends OrderConversions {
  protected val cartDao = daos.cartDao

  def validatedSync(
      cartRecord: CartRecord,
      f: (CartEntity, OrderUpsertion) => OrderUpsertion,
    )(implicit
      store: StoreContext,
    ): Future[UpsertionResult[CartRecord]] =
    cartService.enrich(cartRecord).flatMap { cart =>
      val orderId = cartRecord.orderId.getOrElse(UUID.randomUUID)

      implicit val authHeader = coreClient.generateAuthHeaderForCore

      coreClient.ordersGet(orderId).map(_.asOption).flatMap { maybeExistingOrder =>
        val orderUpsertion = f(cart, toOrderUpsertion(cart, maybeExistingOrder))

        coreClient.ordersValidatedSync(orderId, orderUpsertion).flatMap { result =>
          result.asValidatedData match {
            case Validated.Valid(order) =>
              upsertCart(cart.id)(order).map(Validated.Valid(_))

            case Validated.Invalid(i) =>
              val errors: List[Error] =
                i.toList.flatMap {
                  case clientError: ClientError => clientError.errors.flatMap(_.convert)
                  case _                        => Seq.empty
                }

              Future.successful {
                NonEmptyList.fromList(errors.toList) match {
                  case Some(l) => Validated.Invalid(l)
                  case _       => Validated.Invalid(i)
                }
              }
          }
        }
      }
    }

  def sync(
      cartRecord: CartRecord,
      f: (CartEntity, OrderUpsertion) => OrderUpsertion,
    )(implicit
      store: StoreContext,
    ): Future[UpsertionResult[CartRecord]] =
    cartService.enrich(cartRecord).flatMap { cart =>
      val orderId = cartRecord.orderId.getOrElse(UUID.randomUUID)

      implicit val authHeader = coreClient.generateAuthHeaderForCore

      coreClient
        .ordersGet(orderId)
        .map(_.asOption)
        .flatMap { maybeExistingOrder =>
          val orderUpsertion = f(cart, toOrderUpsertion(cart, maybeExistingOrder))

          coreClient
            .ordersSync(orderId, orderUpsertion)
            .map(_.asValidatedData)
            .flatMapValid(upsertCart(cart.id))
        }
    }

  def storePaymentTransaction(
      cartRecord: CartRecord,
      orderId: UUID,
      upsertion: PaymentTransactionUpsertion,
    )(implicit
      store: StoreContext,
    ): Future[UpsertionResult[CartRecord]] =
    coreClient
      .orderStorePaymentTransaction(
        id = orderId,
        upsertion = upsertion,
      )(coreClient.generateAuthHeaderForCore)
      .map(_.asValidatedData)
      .flatMapValid(upsertCart(cartRecord.id))

  def upsertCart(
      cartId: UUID,
      appliedGiftCardPasses: Option[Seq[GiftCardPassApplied]] = None,
    )(
      order: Order,
    ): Future[Result[CartRecord]] =
    cartDao.upsert(
      CartUpdate
        .empty
        .copy(
          id = cartId.some,
          orderId = order.id.some,
          orderNumber = order.number.some,
          status = order
            .onlineOrderAttribute
            .map(_.acceptanceStatus)
            .fold[CartStatus](CartStatus.New) {
              case AcceptanceStatus.Pending => CartStatus.Paid
              case _                        => CartStatus.New
            }
            .some,
          appliedGiftCardPasses = appliedGiftCardPasses,
        ),
    )
}
