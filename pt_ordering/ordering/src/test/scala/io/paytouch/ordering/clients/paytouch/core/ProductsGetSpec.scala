package io.paytouch.ordering.clients.paytouch.core

import java.util.UUID

import akka.http.scaladsl.model._

import cats.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.expansions._
import io.paytouch.ordering.clients.paytouch.core.entities.enums.{ ArticleScope, ArticleType }
import io.paytouch.ordering.entities.enums.{ ModifierSetType, UnitType }
import io.paytouch.ordering.entities.MonetaryAmount._
import io.paytouch.ordering.errors.ClientError

class ProductsGetSpec extends PtCoreClientSpec {
  abstract class ProductsGetSpecContext extends CoreClientSpecContext {
    val id: UUID = "63f291ce-da3f-35da-a050-9bc51f80eee6"

    val expansions =
      ProductExpansions
        .empty
        .withModifiers
        .withStockLevel
        .withTaxRates
        .withVariants

    val params = {
      val expectedExpansions =
        "expand[]=modifier_ids,modifier_positions,modifiers,stock_level,tax_rate_locations,tax_rates,variants"
      s"product_id=$id&$expectedExpansions"
    }

    def assertProductsGet(implicit request: HttpRequest, authToken: HttpHeader) =
      assertUserAuthRequest(HttpMethods.GET, "/v1/products.get", authToken, queryParams = Some(params))
  }

  "CoreClient" should {
    "call products.get" should {
      "parse a product" in new ProductsGetSpecContext with ProductFixture {
        val response = when(productsGet(id, expansions))
          .expectRequest(implicit request => assertProductsGet)
          .respondWith(productFileName)
        response.await.map(_.data) ==== Right(expectedProduct)
      }

      "parse rejection" in new ProductsGetSpecContext {
        val endpoint =
          completeUri(s"/v1/products.get?$params")
        val expectedRejection = ClientError(endpoint, "The supplied authentication is invalid")
        val response = when(productsGet(id, expansions))
          .expectRequest(implicit request => assertProductsGet)
          .respondWithRejection("/core/responses/auth_rejection.json")
        response.await ==== Left(expectedRejection)
      }
    }
  }

  trait ProductFixture { self: ProductsGetSpecContext =>
    val productFileName = "/core/responses/products_get.json"

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
            value = 59.125,
            applyToPrice = false,
            locationOverrides = Map(
              toUUID("a07c7e10-713e-3050-8907-072c36eec741") -> ItemLocation(active = true),
            ),
          ),
        ),
        stock = Some(
          Stock(
            quantity = BigDecimal(1234),
            sellOutOfStock = false,
          ),
        ),
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
        maximumCount = 1337.some,
        position = 1,
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

    private val bundleSets: Seq[BundleSet] =
      Seq(
        BundleSet(
          id = "a508c3c2-9fae-34d6-83bb-bdd97413db93",
          name = Some("Chicken Aliquaexercitation"),
          position = 1,
          minQuantity = 1,
          maxQuantity = 1,
          options = Seq(
            BundleOption(
              id = "1b163c2e-7ca9-4500-b7ac-70541129f5f6",
              article = ArticleInfo(
                id = "63f291ce-da3f-35da-a050-9bc51f80eee6",
                name = "Bundle Product",
                sku = Some("sku"),
                upc = Some("upc"),
                options = Some(Seq.empty),
              ),
              position = 1,
              priceAdjustment = 1.0,
            ),
          ),
        ),
      )

    val priceRange = MonetaryRange(1 USD, 1 USD).some

    val expectedProduct = Product(
      id = "63f291ce-da3f-35da-a050-9bc51f80eee6",
      `type` = ArticleType.Variant,
      scope = ArticleScope.Product,
      name = "Incididunt Sirloin",
      isCombo = false,
      description =
        Some("Sed nostrud swine jowl velit quisaliqua bone proident round pariatur pork cupim mignon ipsumenim bacon"),
      price = 93.00 USD,
      variants = Some(Seq.empty),
      variantProducts = Some(Seq.empty),
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
      avatarImageUrls = Seq.empty,
      categoryOptions = None,
      categoryPositions = Seq.empty,
      bundleSets = bundleSets,
      priceRange = priceRange,
    )

  }

}
