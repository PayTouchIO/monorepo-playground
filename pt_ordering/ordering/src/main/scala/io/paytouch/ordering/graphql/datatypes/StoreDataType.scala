package io.paytouch.ordering.graphql.datatypes

import sangria.macros.derive._
import sangria.schema._

import io.paytouch.ordering._
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.entities.{ Merchant, SimpleStore, Store }
import io.paytouch.ordering.entities.enums.PaymentMethodType
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.graphql.schema.Fetchers
import io.paytouch.ordering.utils._

trait StoreDataType {
  self: AvailabilitiesDataType
    with BigDecimalDataType
    with CatalogDataType
    with CategoryDataType
    with ImageUrlsDataType
    with LocationDataType
    with MerchantDataType
    with MonetaryAmountDataType
    with UUIDDataType
    with PaymentMethodDataType =>
  implicit private lazy val availabilitiesT = AvailabilitiesType
  implicit private lazy val bigDecimalT = BigDecimalType
  implicit private lazy val catalogT = CatalogType
  implicit private lazy val categoryT = CategoryType
  implicit private lazy val imageUrlsT = ImageUrlsType
  implicit private lazy val locationT = LocationType
  implicit private lazy val merchantT = MerchantType
  implicit private lazy val monetaryAmountT = MonetaryAmountType
  implicit private lazy val UUIDT = UUIDType
  implicit private lazy val paymentMethodTypeT = PaymentMethodDataType

  lazy val StoreType = deriveObjectType[GraphQLContext, Store](
    TransformFieldNames(_.underscore),
    ReplaceField(
      "paymentMethods",
      Field("payment_methods", ListType(paymentMethodTypeT), resolve = _.value.paymentMethods),
    ),
    AddFields(
      Field(
        "location",
        OptionType(locationT),
        resolve = { ctx =>
          val locationId = ctx.value.locationId
          val merchantId = ctx.value.merchantId
          ctx.ctx.services.locationService.findById(id = locationId, merchantId = merchantId)
        },
      ),
      Field(
        "merchant",
        OptionType(merchantT),
        resolve = { ctx =>
          val merchantUrlSlug = ctx.value.merchantUrlSlug
          ctx.ctx.services.merchantService.findBySlug(merchantUrlSlug)
        },
      ),
      Field(
        "opening_hours",
        OptionType(availabilitiesT),
        resolve = { ctx =>
          implicit val ec = ctx.ctx.ec
          val catalogId = ctx.value.catalogId
          val merchantId = ctx.value.merchantId
          val locationId = ctx.value.locationId

          val relId = (catalogId.taggedWith[Catalog], merchantId.taggedWith[Merchant])
          val catalogByCatalogId = Fetchers.catalogs.defer(relId)
          DeferredValue(catalogByCatalogId).map {
            case (_, catalog) => catalog.locationOverrides.flatMap(_.get(locationId).map(_.availabilities))
          }
        },
      ),
      Field(
        "catalog",
        OptionType(catalogT),
        resolve = { ctx =>
          implicit val ec = ctx.ctx.ec

          val catalogId = ctx.value.catalogId
          val merchantId = ctx.value.merchantId

          val relId = (catalogId.taggedWith[Catalog], merchantId.taggedWith[Merchant])
          val catalogByCatalogId = Fetchers.catalogs.defer(relId)
          DeferredValue(catalogByCatalogId).map { case (_, catalog) => Some(catalog) }
        },
      ),
      Field(
        "categories",
        ListType(categoryT),
        resolve = { ctx =>
          val catalogId = ctx.value.catalogId
          val locationId = ctx.value.locationId
          val merchantId = ctx.value.merchantId
          ctx
            .ctx
            .services
            .categoryService
            .findAll(
              catalogId = catalogId,
              locationId = locationId,
              merchantId = merchantId,
            )
        },
      ),
    ),
  )

  lazy val SimpleStoreType = deriveObjectType[GraphQLContext, SimpleStore](
    TransformFieldNames(_.underscore),
    AddFields(
      Field(
        "location",
        OptionType(locationT),
        resolve = { ctx =>
          implicit val ec = ctx.ctx.ec
          val cachedStoreLocations = Fetchers.storeLocations
          val locationId = ctx.value.locationId.taggedWith[Location]
          val merchantId = ctx.value.merchantId.taggedWith[Merchant]
          val relId = (locationId, merchantId)
          val storeLocations = cachedStoreLocations.defer(relId)
          DeferredValue(storeLocations).map { case (_, location) => location }
        },
      ),
    ),
  )
}
