package io.paytouch.ordering.clients.paytouch.core

import java.util.UUID

import akka.http.scaladsl.model._

import cats.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums.{ ArticleScope, ArticleType, ImageSize }
import io.paytouch.ordering.clients.paytouch.core.expansions._
import io.paytouch.ordering.entities.enums.{ ModifierSetType, UnitType }
import io.paytouch.ordering.entities.MonetaryAmount._
import io.paytouch.ordering.errors.ClientError

class ProductsListSpec extends PtCoreClientSpec {
  abstract class ProductsListSpecContext extends CoreClientSpecContext {
    private val catalogAId: UUID = "5a3b6c93-77f1-4c9a-a664-54d19c6d2961"
    private val catalogBId: UUID = "63f291ce-da3f-35da-a050-9bc51f80eee6"

    val locationId: UUID = "478a122b-37fc-318c-a70f-f95cdd699b78"

    val productId: UUID = "ba35febf-934e-401f-96db-2f18d85f6ee1"

    val catalogIds = Seq(catalogAId, catalogAId, catalogBId) // catalogAId is repeated but then sent only once

    val expansions =
      ProductExpansions
        .empty
        .withCategoryData
        .withModifiers
        .withStockLevel
        .withTaxRates
        .withVariants

    val expectedExpansions =
      "expand[]=catalog_category_options,catalog_category_positions,category_positions,modifier_ids,modifier_positions,modifiers,stock_level,tax_rate_locations,tax_rates,variants"
    val paging = "per_page=100"

    val listFilters = s"catalog_id[]=$catalogAId,$catalogBId&location_id=$locationId"

    val listByIdsFilters = s"ids[]=$productId&type[]=simple,variant,template"

    def params(filters: String) = s"$filters&$expectedExpansions&$paging"

    def assertProductsList(implicit request: HttpRequest, authToken: HttpHeader) =
      assertUserAuthRequest(HttpMethods.GET, "/v1/products.list", authToken, queryParams = Some(params(listFilters)))

    def assertProductsListByIds(implicit request: HttpRequest, authToken: HttpHeader) =
      assertUserAuthRequest(
        HttpMethods.GET,
        "/v1/products.list",
        authToken,
        queryParams = Some(params(listByIdsFilters)),
      )

  }

  "CoreClient" should {

    "call productsList" should {

      "parse a product list" in new ProductsListSpecContext with ProductFixture {
        val response = when(productsList(catalogIds, locationId, expansions))
          .expectRequest(implicit request => assertProductsList)
          .respondWith(productFileName)
        response.await.map(_.data) ==== Right(expectedProducts)
      }

      "parse rejection" in new ProductsListSpecContext {
        val endpoint =
          completeUri(s"/v1/products.list?${params(listFilters)}")
        val expectedRejection = ClientError(endpoint, "The supplied authentication is invalid")
        val response = when(productsList(catalogIds, locationId, expansions))
          .expectRequest(implicit request => assertProductsList)
          .respondWithRejection("/core/responses/auth_rejection.json")
        response.await ==== Left(expectedRejection)
      }
    }

    "call productsListByIds" should {

      "parse a product list" in new ProductsListSpecContext with ProductFixture {
        val response = when(productsListByIds(Seq(productId)))
          .expectRequest(implicit request => assertProductsListByIds)
          .respondWith(productFileName)
        response.await.map(_.data) ==== Right(expectedProducts)
      }

      "parse rejection" in new ProductsListSpecContext {
        val endpoint =
          completeUri(s"/v1/products.list?${params(listByIdsFilters)}")
        val expectedRejection = ClientError(endpoint, "The supplied authentication is invalid")
        val response = when(productsListByIds(Seq(productId)))
          .expectRequest(implicit request => assertProductsListByIds)
          .respondWithRejection("/core/responses/auth_rejection.json")
        response.await ==== Left(expectedRejection)
      }
    }
  }

  trait ProductFixture { self: ProductsListSpecContext =>
    val productFileName = "/core/responses/products_list.json"

    private val imageUrls = Seq(
      ImageUrls(
        imageUploadId = "152444e7-168f-4b1b-8e28-a33fc3f5f92a",
        urls = ImageSize.values.map(size => size -> s"http://my-image-url-${size.entryName}").toMap,
      ),
    )

    private val locationOverrides: Map[UUID, ProductLocation] = Map(
      toUUID("a07c7e10-713e-3050-8907-072c36eec741") -> ProductLocation(
        price = 1 USD,
        cost = Some(1 USD),
        unit = UnitType.`Unit`,
        active = true,
        taxRates = Seq(
          TaxRate(
            id = "061cedf1-b7ed-38d6-a32c-f926032b5409",
            name = "Jowl Eiusmod",
            value = 59,
            applyToPrice = false,
            locationOverrides = Map(
              toUUID("a07c7e10-713e-3050-8907-072c36eec741") -> ItemLocation(active = true),
            ),
          ),
        ),
        stock = None,
      ),
    )

    private val options: Seq[VariantOptionWithType] = Seq(
      VariantOptionWithType(
        id = "4011b496-08e0-480c-a917-e60c7288a321",
        name = "Anim Rump",
        typeName = "Filet",
        position = 1,
        typePosition = 1,
      ),
    )

    private val modifierLocationOverrides = Map(toUUID("478a122b-37fc-318c-a70f-f95cdd699b78") -> ItemLocation(true))
    private val modifierOptions = Seq(
      ModifierOption(
        id = "1b163c2e-7ca9-4500-b7ac-70541129f5f6",
        name = "Cow Chicken",
        price = 1 USD,
        position = 1,
        maximumCount = 1337.some,
        active = true,
      ),
    )

    private val modifiers: Seq[ModifierSet] =
      Seq(
        ModifierSet(
          id = "a508c3c2-9fae-34d6-83bb-bdd97413db93",
          `type` = ModifierSetType.Hold,
          name = "Chicken Aliquaexercitation",
          minimumOptionCount = 1,
          maximumOptionCount = None,
          singleChoice = false,
          force = false,
          locationOverrides = modifierLocationOverrides,
          options = modifierOptions,
        ),
      )

    private val modifierPositions: Seq[ModifierPosition] =
      Seq(
        ModifierPosition(
          modifierSetId = "a508c3c2-9fae-34d6-83bb-bdd97413db93",
          position = Some(3),
        ),
      )

    private val variantOptions = Seq(
      VariantOption(id = "70994ac6-ecf0-41cc-854d-7d9446b81daa", name = "Kheema", position = 1),
    )
    private val variants = Seq(
      VariantOptionType(
        id = "6e66cfd7-5a72-40d0-8a53-6bdc9cb26fec",
        name = "Filling",
        position = 1,
        options = variantOptions,
      ),
    )

    private val variantProducts = Seq(
      Product(
        id = "ff01a033-2bb8-4497-86b9-019ca760da3a",
        `type` = ArticleType.Variant,
        scope = ArticleScope.Product,
        isCombo = false,
        name = "Roasted Chips & Salsa",
        description = Some("Spicy lentil crackers served with mint chutney and homemade salsa."),
        price = 2 USD,
        variants = None,
        variantProducts = None,
        unit = UnitType.`Unit`,
        isVariantOfProductId = Some("deb046a9-5ff4-40f9-b057-233337f8e8b7"),
        hasVariant = false,
        trackInventory = true,
        active = true,
        locationOverrides = Map.empty,
        options = Seq.empty,
        modifierIds = None,
        modifiers = None,
        modifierPositions = None,
        avatarBgColor = Some("#546E7A"),
        avatarImageUrls = Seq.empty,
        hasParts = false,
        categoryOptions = None,
        categoryPositions = Seq.empty,
        bundleSets = Seq.empty,
        priceRange = None,
      ),
    )

    private val catalogCategoryOptions = Seq(
      CategoryOption(
        categoryId = "5a3b6c93-77f1-4c9a-a664-54d19c6d2961",
        deliveryEnabled = true,
        takeAwayEnabled = false,
      ),
    )

    private val catalogCategoryPositions = Seq(
      CategoryPosition(
        categoryId = "5a3b6c93-77f1-4c9a-a664-54d19c6d2961",
        position = 1,
      ),
    )

    private val systemCategoryPositions = Seq(
      CategoryPosition(
        categoryId = "5a3b6c93-77f1-4c9a-a664-54d19c6d2962",
        position = 1,
      ),
    )

    private val priceRange = MonetaryRange(1 USD, 1 USD).some

    private val product = Product(
      id = "63f291ce-da3f-35da-a050-9bc51f80eee6",
      `type` = ArticleType.Variant,
      scope = ArticleScope.Product,
      name = "Incididunt Sirloin",
      isCombo = false,
      description =
        Some("Sed nostrud swine jowl velit quisaliqua bone proident round pariatur pork cupim mignon ipsumenim bacon"),
      price = 93.00 USD,
      variants = Some(variants),
      variantProducts = Some(variantProducts),
      isVariantOfProductId = Some("63f291ce-da3f-35da-a050-9bc51f80eee6"),
      hasVariant = false,
      trackInventory = false,
      active = true,
      hasParts = false,
      unit = UnitType.`Unit`,
      locationOverrides = locationOverrides,
      options = options,
      modifierIds = Seq(UUID.fromString("a508c3c2-9fae-34d6-83bb-bdd97413db93")).some,
      modifiers = modifiers.some,
      modifierPositions = Some(modifierPositions),
      avatarBgColor = Some("9b2fae"),
      avatarImageUrls = imageUrls,
      categoryOptions = Some(catalogCategoryOptions),
      categoryPositions = catalogCategoryPositions,
      bundleSets = Seq.empty,
      priceRange = priceRange,
    )

    val expectedProducts = Seq(product)
  }

}
