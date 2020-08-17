package io.paytouch.ordering.resources

import java.util.UUID

import scala.concurrent._

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route

import io.paytouch._

import io.paytouch.ordering.data.redis.ConfiguredRedis
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.ExposedName
import io.paytouch.ordering.resources.features.StandardStoreResource
import io.paytouch.ordering.services._
import io.paytouch.ordering.UpsertionResult
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData

final class CartResource(
    override val authenticationService: AuthenticationService,
    cartService: CartService,
    cartCheckoutService: CartCheckoutService,
  )(implicit
    val ec: ExecutionContext,
    val redis: ConfiguredRedis,
    implicit
    val system: ActorSystem,
  ) extends StandardStoreResource
       with Locking {
  import CartResource._

  override val resourcePath: String =
    "carts"

  override val paramName: String =
    "cart_id"

  override lazy val routes: Route =
    concat(
      applyGiftCard(implicit store => cartService.applyGiftCard),
      unapplyGiftCard(implicit store => cartService.unapplyGiftCard),
      checkoutCartRoute(implicit store => cartCheckoutService.checkout),
      createCartRoute(implicit store => cartService.create),
      getRoute(implicit store => cartService.findById),
      syncRoute(implicit store => cartService.sync),
      updateCartRoute(implicit store => cartService.update),
    )

  private[this] def createCartRoute(f: Context => (UUID, CartCreation) => Future[UpsertionResult[Cart]]) =
    (path(s"$resourcePath.create") & post) {
      parameters(s"$paramName".as[UUID]) { id =>
        entity(as[CartCreation]) { creation =>
          lockEntity(ExposedName.Cart, id) {
            contextAuthenticationFromStore(creation.storeId) { context =>
              onSuccess(f(context)(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    }

  private[this] def applyGiftCard(f: Context => (UUID, GiftCardPass.OnlineCode.Raw) => Future[UpsertionResult[Cart]]) =
    (path(s"$resourcePath.apply_gift_card") & post) {
      parameters(s"$paramName".as[UUID]) { cartId =>
        entity(as[ApplyGiftCard]) { body =>
          lockEntity(ExposedName.Cart, cartId) {
            contextAuthenticationFromCart(cartId) { context =>
              onSuccess(f(context)(cartId, body.onlineCode))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    }

  private[this] def unapplyGiftCard(
      f: Context => (UUID, GiftCardPass.OnlineCode.Raw) => Future[UpsertionResult[Cart]],
    ) =
    (path(s"$resourcePath.unapply_gift_card") & post) {
      parameters(s"$paramName".as[UUID]) { cartId =>
        entity(as[UnapplyGiftCard]) { body =>
          lockEntity(ExposedName.Cart, cartId) {
            contextAuthenticationFromCart(cartId) { context =>
              onSuccess(f(context)(cartId, body.onlineCode))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    }

  private[this] def updateCartRoute(f: Context => (UUID, CartUpdate) => Future[UpsertionResult[Cart]]) =
    (path(s"$resourcePath.update") & post) {
      parameters(s"$paramName".as[UUID]) { cartId =>
        entity(as[CartUpdate]) { update =>
          lockEntity(ExposedName.Cart, cartId) {
            contextAuthenticationFromCart(cartId) { context =>
              onSuccess(f(context)(cartId, update))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    }

  private[this] def checkoutCartRoute(f: Context => (UUID, CartUpdate) => Future[UpsertionResult[Cart]]) =
    (path(s"$resourcePath.checkout") & post) {
      parameters(s"$paramName".as[UUID]) { cartId =>
        entity(as[CartUpdate]) { update =>
          lockEntity(ExposedName.Cart, cartId) {
            contextAuthenticationFromCart(cartId) { context =>
              onSuccess(f(context)(cartId, update))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    }

  private[this] def syncRoute(f: Context => UUID => Future[UpsertionResult[Cart]]) =
    (path(s"$resourcePath.sync") & post) {
      parameters(s"$paramName".as[UUID]) { cartId =>
        lockEntity(ExposedName.Cart, cartId) {
          contextAuthenticationFromCart(cartId) { context =>
            onSuccess(f(context)(cartId))(result => completeAsApiResponse(result))
          }
        }
      }
    }
}

object CartResource {
  final case class ApplyGiftCard(onlineCode: GiftCardPass.OnlineCode.Raw)
  final case class UnapplyGiftCard(onlineCode: GiftCardPass.OnlineCode.Raw)
}
