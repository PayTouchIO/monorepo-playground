package io.paytouch.ordering.resources.features

import java.util.UUID

import scala.concurrent._

import akka.http.scaladsl.server._

import io.paytouch.ordering.entities.StoreContext

trait StandardStoreResource extends StandardResource {
  final type Context = StoreContext

  protected def contextAuthentication(f: Context => Route): Route =
    parameters("cart_id".as[UUID])(contextAuthenticationFromCart(_)(f))

  protected def contextAuthenticationFromStore(storeId: UUID)(f: Context => Route): Route =
    genericContextAuthentication(storeId, authenticationService.getStoreContextFromStoreId)(f, "store id")

  protected def contextAuthenticationFromCart(cartId: UUID)(f: Context => Route): Route =
    genericContextAuthentication(cartId, authenticationService.getStoreContextFromCartId)(f, "cart id")

  private[this] def genericContextAuthentication[T](
      id: T,
      retriever: T => Future[Option[Context]],
    )(
      f: Context => Route,
      description: String,
    ): Route =
    appAuthenticate(_ => retrieveContext(id, retriever)(f, description))

  private[this] def retrieveContext[T](
      id: T,
      retriever: T => Future[Option[Context]],
    )(
      f: Context => Route,
      description: String,
    ): RequestContext => Future[RouteResult] =
    onSuccess(retriever(id)) {
      case Some(storeContext) => f(storeContext)
      case None               => reject(ValidationRejection(s"$description does not exist"))
    }
}
