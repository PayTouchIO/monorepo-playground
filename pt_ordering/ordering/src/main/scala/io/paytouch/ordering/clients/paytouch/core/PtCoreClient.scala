package io.paytouch.ordering.clients.paytouch.core

import java.util.UUID

import scala.concurrent._

import akka.actor._
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.Uri
import akka.stream.Materializer

import io.paytouch._

import io.paytouch.logging.BaseMdcActor
import io.paytouch.ordering.clients._
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.expansions._
import io.paytouch.ordering.withTag

class PtCoreClient(
    val uri: Uri withTag PtCoreClient,
  )(implicit
    val mdcActor: ActorRef withTag BaseMdcActor,
    val system: ActorSystem,
    val materializer: Materializer,
  ) extends PaytouchClient
       with JwtSupport
       with Parameters {
  private val paging = "per_page=100"

  def usersContext(implicit authToken: Authorization): Future[CoreApiResponse[CoreUserContext]] =
    sendAndReceiveAsApiResponse[CoreUserContext](Get("/v1/users.context").withUserAuth)

  def idsValidate(ids: CoreIds)(implicit authToken: Authorization): Future[PaytouchResponse[Unit]] =
    sendAndReceiveEmpty(Post("/v1/ids.validate", ids).withUserAuth)

  def catalogsGet(id: UUID)(implicit authToken: Authorization): Future[CoreApiResponse[Catalog]] = {
    val expansions = expandParameter("products_count", "location_overrides")
    val filters = filterParameters("catalog_id" -> Seq(id))
    val url = s"/v1/catalogs.get?$filters&$expansions"
    sendAndReceiveAsApiResponse[Catalog](Get(url).withUserAuth)
  }

  def catalogsListByIds(
      ids: Seq[UUID],
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Seq[Catalog]]] = {
    val expansions = expandParameter("products_count", "location_overrides")
    val filters = filterParameters("id[]" -> ids.map(_.toString))
    val url =
      s"/v1/catalogs.list?$filters&$expansions&$paging"
    sendAndReceiveAsPaginatedApiResponse[Catalog](Get(url).withUserAuth)
  }

  def catalogCategoriesList(
      catalogId: UUID,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Seq[Category]]] = {
    val expansions =
      expandParameter("subcatalog_categories", "locations", "availabilities")
    val filters = filterParameters("catalog_id" -> Seq(catalogId))
    val url = s"/v1/catalog_categories.list?$filters&$expansions&$paging"
    sendAndReceiveAsPaginatedApiResponse[Category](Get(url).withUserAuth)
  }

  def locationsGet(id: UUID)(implicit authToken: Authorization): Future[CoreApiResponse[Location]] = {
    val expansions = expandParameter("opening_hours", "settings")
    val filters = filterParameters("location_id" -> Seq(id))
    val url = s"/v1/locations.get?$filters&$expansions"
    sendAndReceiveAsApiResponse[Location](Get(url).withUserAuth)
  }

  def locationsList(implicit authToken: Authorization): Future[CoreApiResponse[Seq[Location]]] = {
    val expansions = expandParameter("opening_hours")
    val url = s"/v1/locations.list?$expansions"
    sendAndReceiveAsPaginatedApiResponse[Location](Get(url).withUserAuth)
  }

  def giftCardsList(implicit authToken: Authorization): Future[CoreApiResponse[Seq[GiftCard]]] = {
    val url = s"/v1/gift_cards.list"

    sendAndReceiveAsPaginatedApiResponse[GiftCard](Get(url).withUserAuth)
  }

  def merchantsMe(
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[CoreMerchant]] = {
    val url = s"/v1/merchants.me"
    sendAndReceiveAsApiResponse[CoreMerchant](Get(url).withUserAuth)
  }

  def productsGet(
      id: UUID,
      expansions: ProductExpansions,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Product]] = {
    val filters = filterParameters("product_id" -> Seq(id))
    val url = s"/v1/products.get?$filters&${expansions.toParameter}"
    sendAndReceiveAsApiResponse[Product](Get(url).withUserAuth)
  }

  def giftCardPassesLookup(
      onlineCode: io.paytouch.GiftCardPass.OnlineCode.Raw,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[GiftCardPass]] = {
    val filters = filterParameters("online_code" -> Seq(onlineCode.value))
    val url = s"/v1/gift_card_passes.lookup?$filters"

    sendAndReceiveAsApiResponse[GiftCardPass](Get(url).withUserAuth)
  }

  def giftCardPassesBulkCharge(
      orderId: OrderId,
      bulkCharge: Seq[GiftCardPassCharge],
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Order]] = {
    val filters = filterParameters("order_id" -> Seq(orderId.value))
    val url = s"/v1/gift_card_passes.bulk_charge?$filters"

    sendAndReceiveAsApiResponse[Order](Post(url, bulkCharge).withUserAuth)
  }

  def productsList(
      catalogIds: Seq[UUID],
      locationId: UUID,
      expansions: ProductExpansions,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Seq[Product]]] = {
    val filters = filterParameters(
      "catalog_id[]" -> catalogIds.distinct,
      "location_id" -> Seq(locationId),
    )
    val url =
      s"/v1/products.list?$filters&${expansions.toParameter}&$paging"
    sendAndReceiveAsPaginatedApiResponse[Product](Get(url).withUserAuth)
  }

  def productsListByIds(ids: Seq[UUID])(implicit authToken: Authorization): Future[CoreApiResponse[Seq[Product]]] = {
    val expansions =
      ProductExpansions
        .empty
        .withCategoryData
        .withModifiers
        .withStockLevel
        .withTaxRates
        .withVariants
    val filters = filterParameters("ids[]" -> ids.map(_.toString), "type[]" -> Seq("simple", "variant", "template"))
    val url =
      s"/v1/products.list?$filters&${expansions.toParameter}&$paging"
    sendAndReceiveAsPaginatedApiResponse[Product](Get(url).withUserAuth)
  }

  def modifierSetsListByIds(
      ids: Seq[UUID],
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Seq[ModifierSet]]] = {
    val expansions = expandParameter("locations")
    val filters = filterParameters("id[]" -> ids.map(_.toString))
    val url =
      s"/v1/modifier_sets.list?$filters&$expansions&$paging"
    sendAndReceiveAsPaginatedApiResponse[ModifierSet](Get(url).withUserAuth)
  }

  def ordersSync(
      id: UUID,
      upsertion: OrderUpsertion,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Order]] = {
    val filters = filterParameters("order_id" -> Seq(id))
    val url = s"/v1/orders.sync?$filters"
    sendAndReceiveAsApiResponse[Order](Post(url, upsertion).withUserAuth)
  }

  def ordersValidatedSync(
      id: UUID,
      upsertion: OrderUpsertion,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Order]] = {
    val filters = filterParameters("order_id" -> Seq(id))
    val url = s"/v1/orders.validated_sync?$filters"
    sendAndReceiveAsApiResponse[Order](Post(url, upsertion).withUserAuth)
  }

  def ordersGet(
      id: UUID,
      open: Option[Boolean] = None,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Order]] = {
    val filters = filterParameters("order_id" -> Seq(id), "is_open" -> open)
    val url = s"/v1/orders.get?$filters"
    sendAndReceiveAsApiResponse[Order](Get(url).withUserAuth)
  }

  def ordersListByTable(
      tableId: UUID,
      open: Option[Boolean] = None,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Seq[Order]]] = {
    val filters = filterParameters(
      "table_id" -> Seq(tableId),
      "is_open" -> open,
    )

    val url = s"/v1/orders.list?$filters"
    sendAndReceiveAsApiResponse[Seq[Order]](Get(url).withUserAuth)
  }

  def orderStorePaymentTransaction(
      id: UUID,
      upsertion: OrderServiceStorePaymentTransactionUpsertion,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Order]] = {
    val filters = filterParameters("order_id" -> Seq(id))
    val url = s"/v1/orders.store_payment_transaction?$filters"

    sendAndReceiveAsApiResponse[Order](Post(url, upsertion).withUserAuth)
  }
}
