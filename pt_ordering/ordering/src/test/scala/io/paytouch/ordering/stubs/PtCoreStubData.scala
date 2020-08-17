package io.paytouch.ordering.stubs

import java.time.Instant
import java.util.UUID

import scala.collection.mutable

import akka.http.scaladsl.model.headers.Authorization

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.CoreErrorResponse
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums.ImageType
import io.paytouch.ordering.clients.paytouch.core.JwtSupport

object PtCoreStubData extends JwtSupport {
  def constantInstant = Instant.parse("2007-12-03T10:15:30.00Z")

  final case class OnlineCodeWithGiftCardPass(
      onlineCode: io.paytouch.GiftCardPass.OnlineCode.Raw,
      giftCardPass: GiftCardPass,
    )

  private var catalogIdsMap: mutable.Map[Authorization, Seq[UUID]] =
    mutable.Map.empty
  private var catalogsMap: mutable.Map[Authorization, Seq[Catalog]] =
    mutable.Map.empty
  private var categoriesMap: mutable.Map[Authorization, Seq[Category]] =
    mutable.Map.empty
  private var giftCardsMap: mutable.Map[Authorization, Seq[GiftCard]] =
    mutable.Map.empty
  private var imageIdsMap: mutable.Map[Authorization, mutable.Map[ImageType, Seq[UUID]]] =
    mutable.Map.empty
  private var locationIdsMap: mutable.Map[Authorization, Seq[UUID]] =
    mutable.Map.empty
  private var locationsMap: mutable.Map[Authorization, Seq[Location]] =
    mutable.Map.empty
  private var merchantsMap: mutable.Map[Authorization, Seq[CoreMerchant]] =
    mutable.Map.empty
  private var ordersMap: mutable.Map[Authorization, Seq[Order]] =
    mutable.Map.empty
  private var productsMap: mutable.Map[Authorization, Seq[Product]] =
    mutable.Map.empty
  private var modifierSetsMap: mutable.Map[Authorization, Seq[ModifierSet]] =
    mutable.Map.empty
  private var giftCardPassesMap: mutable.Map[Authorization, Seq[OnlineCodeWithGiftCardPass]] =
    mutable.Map.empty
  private var tokensMap: mutable.Map[Authorization, CoreUserContext] =
    mutable.Map.empty
  private var validatedSyncErrorResponseMap: mutable.Map[Authorization, CoreErrorResponse] =
    mutable.Map.empty

  def recordToken(token: Authorization, coreContext: CoreUserContext) =
    synchronized {
      tokensMap += token -> coreContext
    }

  def recordLocationIds(locationIds: Seq[UUID])(implicit token: Authorization) =
    synchronized {
      recordEntities(locationIdsMap, locationIds)
    }

  def recordCatalogIds(catalogIds: Seq[UUID])(implicit token: Authorization) =
    synchronized {
      recordEntities(catalogIdsMap, catalogIds)
    }

  def recordImageIds(imageIds: mutable.Map[ImageType, Seq[UUID]])(implicit token: Authorization) =
    synchronized {
      // TODO - merge with existing maps rather than replacing it
      imageIdsMap += token -> imageIds
    }

  def recordCatalog(catalog: Catalog)(implicit token: Authorization) =
    synchronized {
      recordEntity(catalogsMap, catalog)
    }

  def recordCategory(category: Category)(implicit token: Authorization) =
    synchronized {
      recordEntity(categoriesMap, category)
    }

  def recordLocation(location: Location)(implicit token: Authorization) =
    synchronized {
      recordEntity(locationsMap, location)
    }

  def recordGiftCard(giftCard: GiftCard)(implicit token: Authorization) =
    synchronized {
      recordEntity(giftCardsMap, giftCard)
    }

  def recordGiftCardPass(data: OnlineCodeWithGiftCardPass)(implicit token: Authorization) =
    synchronized {
      recordEntity(giftCardPassesMap, data)
    }

  def recordMerchant(merchant: CoreMerchant)(implicit token: Authorization) =
    synchronized {
      recordEntity(merchantsMap, merchant)
    }

  def recordOrder(order: Order)(implicit token: Authorization) =
    synchronized {
      recordEntity(ordersMap, order)
    }

  def recordOrders(orders: Seq[Order])(implicit token: Authorization) =
    synchronized {
      recordEntities(ordersMap, orders)
    }

  def recordProduct(product: Product)(implicit token: Authorization) =
    synchronized {
      recordEntity(productsMap, product)
    }

  def recordModifierSet(modifierSet: ModifierSet)(implicit token: Authorization) =
    synchronized {
      recordEntity(modifierSetsMap, modifierSet)
    }

  def stubValidatedSyncErrorResponse(response: CoreErrorResponse)(implicit token: Authorization) =
    synchronized {
      validatedSyncErrorResponseMap += token -> response
    }

  private def recordEntity[T](map: mutable.Map[Authorization, Seq[T]], entity: T)(implicit token: Authorization): Unit =
    recordEntities(map, Seq(entity))

  private def recordEntities[T](
      map: mutable.Map[Authorization, Seq[T]],
      entities: Seq[T],
    )(implicit
      token: Authorization,
    ): Unit = {
    val existingEntities = map.getOrElse(token, Seq.empty)
    map += token -> (existingEntities ++ entities)
  }

  def getUserContext(implicit token: Authorization): Option[CoreUserContext] =
    tokensMap.get(token)

  def isValidToken(implicit token: Authorization): Boolean =
    getUserContext(token).isDefined

  def areValidLocationIds(locationIds: Seq[UUID])(implicit token: Authorization): Boolean =
    areValidIds(locationIdsMap, locationIds)

  def areValidCatalogIds(catalogIds: Seq[UUID])(implicit token: Authorization): Boolean =
    areValidIds(catalogIdsMap, catalogIds)

  def areValidImageIds(imageIds: mutable.Map[ImageType, Seq[UUID]])(implicit token: Authorization): Boolean = {
    val userImageIds: mutable.Map[ImageType, Seq[UUID]] =
      imageIdsMap.getOrElse(token, mutable.Map.empty)

    imageIds.forall {
      case (imageType, imageIds) =>
        val validIds = userImageIds.getOrElse(imageType, Seq.empty).toSet
        imageIds.toSet subsetOf validIds
    }
  }

  private def areValidIds(
      map: mutable.Map[Authorization, Seq[UUID]],
      ids: Seq[UUID],
    )(implicit
      token: Authorization,
    ): Boolean = {
    val validIds = map.getOrElse(token, Seq.empty).toSet
    ids.toSet subsetOf validIds
  }

  def getCatalog(id: UUID)(implicit token: Authorization): Option[Catalog] =
    getEntity(catalogsMap)(_.id == id)

  def listCatalogsPerId(ids: Seq[UUID])(implicit token: Authorization): Seq[Catalog] =
    listEntities(catalogsMap)(c => ids.contains(c.id))

  def getLocation(id: UUID)(implicit token: Authorization): Option[Location] =
    getEntity(locationsMap)(_.id == id)

  def getProduct(id: UUID)(implicit token: Authorization): Option[Product] =
    getEntity(productsMap)(_.id == id)

  def lookupGiftCardPass(
      onlineCode: io.paytouch.GiftCardPass.OnlineCode.Raw,
    )(implicit
      token: Authorization,
    ): Option[GiftCardPass] =
    getEntity(giftCardPassesMap)(_.onlineCode === onlineCode).map(_.giftCardPass)

  def getOneMerchant(implicit token: Authorization): Option[CoreMerchant] =
    getEntity(merchantsMap)(_ => true)

  def getOneOrder(implicit token: Authorization): Option[Order] =
    getEntity(ordersMap)(_ => true)

  def getOrder(id: UUID)(implicit token: Authorization): Option[Order] =
    getEntity(ordersMap)(_.id == id)

  def listCatalogCategoriesPerCatalogId(catalogId: UUID)(implicit token: Authorization): Seq[Category] =
    listEntities(categoriesMap)(_.catalogId == catalogId)

  def listGiftCards(implicit token: Authorization): Seq[GiftCard] =
    listEntities(giftCardsMap)(_ => true)

  def listOrders()(implicit token: Authorization): Seq[Order] =
    listEntities(ordersMap)(_ => true)

  def listProductsPerCatalogId(catalogIds: Seq[UUID])(implicit token: Authorization): Seq[Product] =
    listEntities(productsMap)(_.categoryPositions.exists { cp =>
      catalogIds
        .foldLeft(Seq.empty[Category])((acc, cId) => acc ++ listCatalogCategoriesPerCatalogId(cId))
        .map(_.id)
        .contains(cp.categoryId)
    })

  def listProductsPerId(ids: Seq[UUID])(implicit token: Authorization): Seq[Product] =
    listEntities(productsMap)(p => ids.contains(p.id))

  def listModifierSetsPerId(ids: Seq[UUID])(implicit token: Authorization): Seq[ModifierSet] =
    listEntities(modifierSetsMap)(m => ids.contains(m.id))

  def listLocations(implicit token: Authorization): Seq[Location] =
    listEntities(locationsMap)(_ => true)

  def getValidatedSyncErrorResponse(implicit token: Authorization): Option[CoreErrorResponse] =
    validatedSyncErrorResponseMap.get(token)

  private def getEntity[T](
      map: mutable.Map[Authorization, Seq[T]],
    )(
      predicate: T => Boolean,
    )(implicit
      token: Authorization,
    ): Option[T] =
    listEntities(map)(predicate).headOption

  private def listEntities[T](
      map: mutable.Map[Authorization, Seq[T]],
    )(
      predicate: T => Boolean,
    )(implicit
      token: Authorization,
    ): Seq[T] =
    map.getOrElse(token, Seq.empty).filter(predicate)
}
