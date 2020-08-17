package io.paytouch.ordering.stubs

import java.util.UUID

import scala.concurrent._
import scala.collection.mutable

import akka.actor._
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.Uri
import akka.stream.Materializer

import cats.implicits._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.logging.BaseMdcActor
import io.paytouch.ordering._
import io.paytouch.ordering.clients._
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.expansions._
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.entities.ApiResponse
import io.paytouch.ordering.errors.ClientError
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.logging.MdcActor
import io.paytouch.ordering.stubs.PtCoreStubData._

class PtCoreStubClient(
    implicit
    override val mdcActor: ActorRef withTag BaseMdcActor,
    override val system: ActorSystem,
    override val materializer: Materializer,
  ) extends PtCoreClient(Uri("http://example.com").taggedWith[PtCoreClient])
       with JsonSupport {
  override def instantNow = constantInstant

  override def usersContext(implicit token: Authorization): Future[CoreApiResponse[CoreUserContext]] =
    simulateGetResponse(getUserContext, ops = "users.context")

  override def idsValidate(coreIds: CoreIds)(implicit token: Authorization): Future[PaytouchResponse[Unit]] =
    Future.successful {
      val isRequestValid =
        Boolean.and(
          isValidToken,
          areValidLocationIds(coreIds.locationIds),
          areValidCatalogIds(coreIds.catalogIds),
          areValidImageIds(mutable.Map(coreIds.imageUploadIds.toSeq: _*)),
        )

      if (isRequestValid)
        Right(())
      else
        Left(ClientError("core.stub.idsValidate", "nope"))
    }

  override def catalogsGet(id: UUID)(implicit authToken: Authorization): Future[CoreApiResponse[Catalog]] =
    simulateGetResponse(getCatalog(id), ops = "catalogs.get")

  override def catalogsListByIds(
      ids: Seq[UUID],
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Seq[Catalog]]] =
    simulateListResponse(listCatalogsPerId(ids))

  override def catalogCategoriesList(
      catalogId: UUID,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Seq[Category]]] =
    simulateListResponse(listCatalogCategoriesPerCatalogId(catalogId))

  override def productsList(
      catalogIds: Seq[UUID],
      locationId: UUID,
      expansions: ProductExpansions,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Seq[Product]]] =
    simulateListResponse(listProductsPerCatalogId(catalogIds).map(muteUnexpandedFields(_, expansions)))

  override def productsGet(
      id: UUID,
      expansions: ProductExpansions,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Product]] =
    simulateGetResponse(getProduct(id).map(muteUnexpandedFields(_, expansions)), ops = "products.get")

  private def muteUnexpandedFields(product: Product, e: ProductExpansions): Product = {
    import ProductExpansion._
    product.copy(
      categoryOptions = if (e.contains(CatalogCategoryOptions)) product.categoryOptions else None,
      categoryPositions = if (e.contains(CategoryPositions)) product.categoryPositions else Seq.empty,
      variantProducts = if (e.contains(Variants)) product.variantProducts else None,
      priceRange = if (e.contains(PriceRanges)) product.priceRange else None,
      modifiers = if (e.contains(Modifiers)) product.modifiers else None,
      modifierIds = if (e.contains(ModifierIds)) product.modifierIds else None,
      modifierPositions = if (e.contains(ModifierPositions)) product.modifierPositions else None,
      locationOverrides = product.locationOverrides.transform { (_, locationOverride) =>
        locationOverride.copy(
          taxRates =
            if (e.contains(TaxRates))
              locationOverride.taxRates.map { taxRate =>
                taxRate.copy(
                  locationOverrides =
                    if (e.contains(TaxRateLocations))
                      taxRate.locationOverrides
                    else
                      Map.empty,
                )
              }
            else
              Seq.empty,
          stock =
            if (e.contains(StockLevel))
              locationOverride.stock
            else
              None,
        )
      },
    )
  }

  override def productsListByIds(
      ids: Seq[UUID],
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Seq[Product]]] =
    simulateListResponse(listProductsPerId(ids))

  override def modifierSetsListByIds(
      ids: Seq[UUID],
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Seq[ModifierSet]]] =
    simulateListResponse(listModifierSetsPerId(ids))

  override def locationsGet(id: UUID)(implicit authToken: Authorization): Future[CoreApiResponse[Location]] =
    simulateGetResponse(getLocation(id), ops = "locations.get")

  override def locationsList(implicit authToken: Authorization): Future[CoreApiResponse[Seq[Location]]] =
    simulateListResponse(listLocations)

  override def giftCardPassesLookup(
      onlineCode: io.paytouch.GiftCardPass.OnlineCode.Raw,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[GiftCardPass]] =
    simulateGetResponse(lookupGiftCardPass(onlineCode), ops = "gift_card_passes.lookup")

  override def giftCardPassesBulkCharge(
      orderId: OrderId,
      bulkCharge: Seq[GiftCardPassCharge],
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Order]] =
    simulateGetResponse(getOneOrder, ops = "gift_card_passes.bulk_charge")

  override def ordersSync(
      id: UUID,
      upsertion: OrderUpsertion,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Order]] =
    simulateGetResponse(getOneOrder, ops = "orders.sync")

  override def ordersValidatedSync(
      id: UUID,
      upsertion: OrderUpsertion,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Order]] = {
    val ops = "orders.validated_sync"
    getValidatedSyncErrorResponse(authToken) match {
      case None => simulateGetResponse(getOneOrder, ops)
      case Some(errorResponse) =>
        Future.successful(Left(ClientError(s"core.stub.$ops", errorResponse)))
    }
  }

  override def merchantsMe(
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[CoreMerchant]] =
    simulateGetResponse(getOneMerchant, ops = "merchants.me")

  override def ordersGet(
      id: UUID,
      open: Option[Boolean] = None,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Order]] =
    simulateGetResponse(getOrder(id), ops = "orders.get")

  override def ordersListByTable(
      tableId: UUID,
      open: Option[Boolean] = None,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Seq[Order]]] =
    simulateListResponse(listOrders())

  override def orderStorePaymentTransaction(
      id: UUID,
      upsertion: OrderServiceStorePaymentTransactionUpsertion,
    )(implicit
      authToken: Authorization,
    ): Future[CoreApiResponse[Order]] =
    simulateGetResponse(getOrder(id), ops = "orders.store_payment_transaction")

  override def giftCardsList(implicit authToken: Authorization): Future[CoreApiResponse[Seq[GiftCard]]] =
    simulateListResponse(listGiftCards)

  private def simulateGetResponse[T](
      f: => Option[T],
      ops: String,
    )(implicit
      m: Manifest[T],
      token: Authorization,
    ): Future[CoreApiResponse[T]] =
    Future.successful {
      f match {
        case Some(t) => Right(ApiResponse(t, m.getClass.getSimpleName))
        case _       => Left(ClientError(s"core.stub.$ops", "nope"))
      }
    }

  private def simulateListResponse[T](
      f: => Seq[T],
    )(implicit
      m: Manifest[T],
      token: Authorization,
    ): Future[CoreApiResponse[Seq[T]]] =
    Future.successful(Right(ApiResponse(f, m.getClass.getSimpleName)))
}
