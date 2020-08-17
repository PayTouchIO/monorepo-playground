package io.paytouch.ordering.resources

import java.util.UUID

import scala.concurrent._

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route

import io.paytouch.ordering.data.redis.ConfiguredRedis
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.ExposedName
import io.paytouch.ordering.resources.features.StandardStoreResource
import io.paytouch.ordering.services._
import io.paytouch.ordering.UpsertionResult

final class CartItemResource(
    override val authenticationService: AuthenticationService,
    cartService: CartService,
  )(implicit
    val ec: ExecutionContext,
    val redis: ConfiguredRedis,
    val system: ActorSystem,
  ) extends StandardStoreResource
       with Locking {
  override val resourcePath: String =
    "carts"

  override val paramName: String =
    "cart_item_id"

  override lazy val routes: Route =
    concat(
      addGiftCard(implicit store => cartService.addItem),
      addProduct(implicit store => cartService.addItem),
      removeItem(implicit store => cartService.removeItem),
      updateGiftCard(implicit store => cartService.updateItem),
      // update_item is maintained for backwards compatibility
      updateProduct(implicit store => cartService.updateItem)(suffix = "item"),
      updateProduct(implicit store => cartService.updateItem)(suffix = "product_item"),
    )

  private[this] def addProduct(f: Context => CartItemCreation => Future[UpsertionResult[Cart]]) =
    (path(s"$resourcePath.add_product") & post) {
      entity(as[ProductCartItemCreation]) { creation =>
        lockEntity(ExposedName.Cart, creation.cartId) {
          contextAuthenticationFromCart(creation.cartId) { context =>
            onSuccess(f(context)(creation.toCartItemCreation))(result => completeAsApiResponse(result))
          }
        }
      }
    }

  private[this] def addGiftCard(f: Context => CartItemCreation => Future[UpsertionResult[Cart]]) =
    (path(s"$resourcePath.add_gift_card") & post) {
      entity(as[GiftCardCartItemCreation]) { creation =>
        lockEntity(ExposedName.Cart, creation.cartId) {
          contextAuthenticationFromCart(creation.cartId) { context =>
            onSuccess(f(context)(creation.toCartItemCreation))(result => completeAsApiResponse(result))
          }
        }
      }
    }

  private[this] def updateProduct(
      f: Context => (UUID, UUID, CartItemUpdate) => Future[UpsertionResult[Cart]],
    )(
      suffix: String,
    ) =
    (path(s"$resourcePath.update_${suffix}") & post) {
      parameters(s"cart_id".as[UUID], paramName.as[UUID]) { (cartId, cartItemId) =>
        entity(as[ProductCartItemUpdate]) { update =>
          lockEntity(ExposedName.Cart, cartId) {
            contextAuthenticationFromCart(cartId) { context =>
              onSuccess(f(context)(cartId, cartItemId, update.toCartItemUpdate))(result =>
                completeAsApiResponse(result),
              )
            }
          }
        }
      }
    }

  private[this] def updateGiftCard(f: Context => (UUID, UUID, CartItemUpdate) => Future[UpsertionResult[Cart]]) =
    (path(s"$resourcePath.update_gift_card_item") & post) {
      parameters(s"cart_id".as[UUID], paramName.as[UUID]) { (cartId, cartItemId) =>
        entity(as[GiftCardCartItemUpdate]) { update =>
          lockEntity(ExposedName.Cart, cartId) {
            contextAuthenticationFromCart(cartId) { context =>
              onSuccess(f(context)(cartId, cartItemId, update.toCartItemUpdate))(result =>
                completeAsApiResponse(result),
              )
            }
          }
        }
      }
    }

  private[this] def removeItem(f: Context => (UUID, UUID) => Future[UpsertionResult[Cart]]) =
    (path(s"$resourcePath.remove_item") & post) {
      parameters(s"cart_id".as[UUID], paramName.as[UUID]) { (cartId, cartItemId) =>
        lockEntity(ExposedName.Cart, cartId) {
          contextAuthenticationFromCart(cartId) { context =>
            onSuccess(f(context)(cartId, cartItemId))(result => completeAsApiResponse(result))
          }
        }
      }
    }
}
