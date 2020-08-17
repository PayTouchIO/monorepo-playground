package io.paytouch.ordering.graphql.categories

import java.util.UUID

import cats.implicits._

import org.json4s.JsonAST._

import org.scalacheck.Arbitrary

import sangria.macros._

import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums.ImageSize
import io.paytouch.ordering.stubs.PtCoreStubData

@scala.annotation.nowarn("msg=Auto-application")
class CategoriesProductsSpec extends CategoriesSchemaSpec {
  abstract class CategoriesProductsSpecContext extends CategoriesSchemaSpecContext {
    val category = random[Category].copy(catalogId = catalogId, merchantId = merchant.id)
    PtCoreStubData.recordCategory(category)

    val categoryOptions = Seq(CategoryOption(category.id, true, false))
    val categoryPositions = Seq(CategoryPosition(category.id, 0))
  }

  "GraphQL Schema" should {

    "allow to fetch categories products information" in new CategoriesProductsSpecContext {
      val expectedBundledProduct = random[Product]
      val expectedArticleInfo = random[ArticleInfo].copy(
        id = expectedBundledProduct.id,
        name = expectedBundledProduct.name,
        options = Some(expectedBundledProduct.options),
      )
      val expectedProduct = random[Product].copy(
        categoryOptions = Some(categoryOptions),
        categoryPositions = categoryPositions,
        locationOverrides = Map(locationId -> random[ProductLocation]),
        priceRange = MonetaryRange(genMonetaryAmount.instance, genMonetaryAmount.instance).some,
        bundleSets = Seq(
          random[BundleSet].copy(
            options = Seq(
              random[BundleOption].copy(
                article = expectedArticleInfo,
              ),
            ),
          ),
        ),
      )
      PtCoreStubData.recordProduct(expectedProduct)
      PtCoreStubData.recordProduct(expectedBundledProduct)

      val query =
        graphql"""
      query FetchSomeCategory($$catalog_id: UUID!, $$location_id: UUID!) {
        categories(catalog_id: $$catalog_id, location_id: $$location_id) {
          products {
            id
            type
            scope
            is_combo
            name
            description
            price { amount, currency }
            variants { id, name, position, options { id, name, position} }
            unit
            is_variant_of_product_id
            has_variant
            track_inventory
            active
            options { id, name, type_name, position, type_position }
            avatar_bg_color
            has_parts
            price_range {
              min { amount, currency }
              max { amount, currency }
            }
            category_options { category_id, delivery_enabled, take_away_enabled }
            category_positions { category_id, position }
            bundle_sets {
              options {
                article {
                  unit
                }
              }
            }
          }
        }
      }
      """

      val vars = JObject("catalog_id" -> JString(catalogId.toString), "location_id" -> JString(locationId.toString))
      val result = executeQuery(query, vars = vars)

      val wrapper = { s: String => s"""{ "categories": [{ "products": $s }] }""" }

      val expectedBundleSets: JValue = JArray(
        List(
          JObject(
            JField(
              "options",
              JArray(
                List(
                  JObject(
                    JField(
                      "article",
                      JObject(
                        JField("unit", JString(expectedBundledProduct.unit.entryName)),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      )

      val expectation = parseAsEntityWithWrapper(
        wrapper,
        Seq(expectedProduct),
        fieldsToRemove = Seq(
          "variant_products",
          "location_overrides",
          "modifier_ids",
          "modifiers",
          "modifier_positions",
          "avatar_image_urls",
        ),
      ).mapField {
        case ("bundle_sets", _) => ("bundle_sets", expectedBundleSets)
        case x                  => x
      }
      result ==== expectation
    }

    "allow to fetch categories products avatar image urls information" in new CategoriesProductsSpecContext {
      val expectedProductImageUrls = Seq(
        ImageUrls(
          imageUploadId = UUID.fromString("c7987cca-6463-453d-9806-1940a3aa4d6e"),
          urls = ImageSize.values.map(value => value -> s"my-url-${value.entryName}").toMap,
        ),
      )
      val product =
        random[Product].copy(
          categoryOptions = Some(categoryOptions),
          locationOverrides = Map(locationId -> random[ProductLocation]),
          categoryPositions = categoryPositions,
          avatarImageUrls = expectedProductImageUrls,
        )
      PtCoreStubData.recordProduct(product)

      val query =
        graphql"""
      query FetchSomeCategory($$catalog_id: UUID!, $$location_id: UUID!) {
        categories(catalog_id: $$catalog_id, location_id: $$location_id) {
          products {
            avatar_image_urls {
              image_upload_id
              urls {
                original
                thumbnail
                small
                medium
                large
              }
            }
          }
        }
      }
      """

      val vars = JObject("catalog_id" -> JString(catalogId.toString), "location_id" -> JString(locationId.toString))
      val result = executeQuery(query, vars = vars)

      val jsonAsString =
        """{
          "data": {
            "categories": [{
              "products": [{
                "avatar_image_urls": [{
                  "image_upload_id": "c7987cca-6463-453d-9806-1940a3aa4d6e",
                  "urls": {
                    "original": "my-url-original",
                    "thumbnail": "my-url-thumbnail",
                    "small": "my-url-small",
                    "medium": "my-url-medium",
                    "large": "my-url-large"
                  }
                }]
              }]
            }]
          }
        }""".stripMargin

      result ==== parseAsSnakeCase(jsonAsString)
    }

    "allow to fetch categories products variant products information" in new CategoriesProductsSpecContext {
      val products = random[Product](2)
      val variantProduct = products(1)
      val expectedProduct = products
        .head
        .copy(
          categoryPositions = categoryPositions,
          locationOverrides = Map(locationId -> random[ProductLocation]),
          variantProducts = Some(Seq(variantProduct)),
        )
      PtCoreStubData.recordProduct(expectedProduct)

      val query =
        graphql"""
      query FetchSomeCategory($$catalog_id: UUID!, $$location_id: UUID!) {
        categories(catalog_id: $$catalog_id, location_id: $$location_id) {
          products {
            variant_products {
              id
            }
          }
        }
      }
      """

      val vars = JObject("catalog_id" -> JString(catalogId.toString), "location_id" -> JString(locationId.toString))
      val result = executeQuery(query, vars = vars)

      val jsonAsString = s"""{
        "data": {
          "categories": [{
            "products": [{
              "variant_products": [{
                "id": "${variantProduct.id}"
              }]
            }]
          }]
        }
      }""".stripMargin

      result ==== parseAsSnakeCase(jsonAsString)
    }

    "allow to fetch categories products location overrides" in new CategoriesProductsSpecContext {
      val expectedProductLocation = random[ProductLocation].copy(taxRates = Seq.empty)
      val product = random[Product]
        .copy(categoryPositions = categoryPositions, locationOverrides = Map(locationId -> expectedProductLocation))
      PtCoreStubData.recordProduct(product)

      val query =
        graphql"""
      query FetchSomeCategory($$catalog_id: UUID!, $$location_id: UUID!) {
        categories(catalog_id: $$catalog_id, location_id: $$location_id) {
          products {
            location_overrides {
              location_id
              product_location {
                price { amount, currency }
                cost { amount, currency }
                unit
                active
                stock {
                  quantity
                  sell_out_of_stock
                }
              }
            }
          }
        }
      }
      """

      val vars = JObject("catalog_id" -> JString(catalogId.toString), "location_id" -> JString(locationId.toString))
      val result = executeQuery(query, vars = vars)

      val wrapper = { s: String =>
        s"""{
          "categories": [{
            "products": [{
              "location_overrides": [{
                "location_id": "$locationId", "product_location": $s
              }]
            }]
          }]
        }""".stripMargin
      }

      result ==== parseAsEntityWithWrapper(wrapper, expectedProductLocation, fieldsToRemove = Seq("tax_rates"))
    }

    "allow to fetch categories products tax rates location overrides" in new CategoriesProductsSpecContext {
      val itemLocation = random[ItemLocation]
      val taxRate = random[TaxRate].copy(locationOverrides = Map(locationId -> itemLocation))
      val productLocation = random[ProductLocation].copy(taxRates = Seq(taxRate))
      val product = random[Product]
        .copy(categoryPositions = categoryPositions, locationOverrides = Map(locationId -> productLocation))
      PtCoreStubData.recordProduct(product)

      val query =
        graphql"""
      query FetchSomeCategory($$catalog_id: UUID!, $$location_id: UUID!) {
        categories(catalog_id: $$catalog_id, location_id: $$location_id) {
          products {
            location_overrides {
              product_location {
                tax_rates {
                  id
                  name
                  value
                  apply_to_price
                  location_overrides {
                    location_id
                    item_location {
                      active
                    }
                  }
                }
              }
            }
          }
        }
      }
      """

      val vars = JObject("catalog_id" -> JString(catalogId.toString), "location_id" -> JString(locationId.toString))
      val result = executeQuery(query, vars = vars)

      val jsonAsString =
        s"""{
          "data": {
            "categories": [{
              "products": [{
                "location_overrides": [{
                  "product_location": {
                    "tax_rates": [{
                      "id": "${taxRate.id}",
                      "name": "${taxRate.name}",
                      "value": "${taxRate.value.toString}",
                      "apply_to_price": ${taxRate.applyToPrice},
                      "location_overrides": [{
                        "location_id": "$locationId",
                        "item_location": {
                          "active": ${itemLocation.active}
                        }
                      }]
                    }]
                  }
                }]
              }]
            }]
          }
        }""".stripMargin

      result ==== parseAsSnakeCase(jsonAsString)
    }

    "allow to fetch categories products modifiers information" in new CategoriesProductsSpecContext {
      val modifierOption = random[ModifierOption]
      val itemLocation = random[ItemLocation]
      val modifierSet =
        random[ModifierSet].copy(locationOverrides = Map(locationId -> itemLocation), options = Seq(modifierOption))
      val modifierPosition = random[ModifierPosition].copy(modifierSetId = modifierSet.id)
      val product = random[Product].copy(
        categoryPositions = categoryPositions,
        locationOverrides = Map(locationId -> random[ProductLocation]),
        modifierIds = Seq(modifierSet.id).some,
        modifierPositions = Some(Seq(modifierPosition)),
      )
      PtCoreStubData.recordProduct(product)
      PtCoreStubData.recordModifierSet(modifierSet)

      val query =
        graphql"""
      query FetchSomeCategory($$catalog_id: UUID!, $$location_id: UUID!) {
        categories(catalog_id: $$catalog_id, location_id: $$location_id) {
          products {
            modifiers {
              id
              type
              name
              minimum_option_count
              maximum_option_count
              single_choice
              force
              options {
                id,
                name,
                price { amount, currency }
                maximum_count
                position
                active
              }
              location_overrides {
                location_id
                item_location { active }
              }
            }
            modifier_positions { modifier_set_id, position }
          }
        }
      }
      """

      val vars = JObject("catalog_id" -> JString(catalogId.toString), "location_id" -> JString(locationId.toString))
      val result = executeQuery(query, vars = vars)

      val jsonAsString =
        s"""{
          "data":{
            "categories":[{
              "products":[{
                "modifiers":[{
                  "id":"${modifierSet.id}",
                  "type":"${modifierSet.`type`.entryName}",
                  "name":"${modifierSet.name}",
                  "minimum_option_count":${modifierSet.minimumOptionCount},
                  "maximum_option_count":${modifierSet.maximumOptionCount.getOrElse(null)},
                  "single_choice":${modifierSet.singleChoice},
                  "force":${modifierSet.force},
                  "options":[{
                    "id":"${modifierOption.id}",
                    "name":"${modifierOption.name}",
                    "price":{
                      "amount":"${modifierOption.price.amount}",
                      "currency":"${modifierOption.price.currency}"
                    },
                    "maximum_count":${modifierOption.maximumCount.getOrElse(null)},
                    "position":${modifierOption.position}
                    "active":${modifierOption.active}
                  }],
                  "location_overrides":[{
                    "location_id":"$locationId",
                    "item_location":{
                      "active":${itemLocation.active}
                    }
                  }]
                }],
                "modifier_positions":[{
                  "modifier_set_id":"${modifierSet.id}",
                  "position":${modifierPosition.position.getOrElse("null")}
                }]
              }]
            }]
          }
        }""".stripMargin

      result ==== parseAsSnakeCase(jsonAsString)
    }

    "allow to fetch categories products template info" in new CategoriesProductsSpecContext {
      val template = random[Product].copy(
        id = UUID.randomUUID,
        name = "Template",
        categoryPositions = Seq.empty,
        locationOverrides = Map(locationId -> random[ProductLocation]),
        isVariantOfProductId = None,
      )
      val variant = random[Product].copy(
        id = UUID.randomUUID,
        name = "Variant",
        categoryPositions = categoryPositions,
        locationOverrides = Map(locationId -> random[ProductLocation]),
        isVariantOfProductId = Some(template.id),
      )
      PtCoreStubData.recordProduct(template)
      PtCoreStubData.recordProduct(variant)

      val query =
        graphql"""
      query FetchSomeCategory($$catalog_id: UUID!, $$location_id: UUID!) {
        categories(catalog_id: $$catalog_id, location_id: $$location_id) {
          products {
            id
            template {
              id
              name
            }
          }
        }
      }
      """

      val vars = JObject("catalog_id" -> JString(catalogId.toString), "location_id" -> JString(locationId.toString))
      val result = executeQuery(query, vars = vars)

      val jsonAsString =
        s"""{
          "data":{
            "categories":[{
              "products":[{
                "id": "${variant.id}",
                "template":{
                  "id":"${template.id}",
                  "name":"${template.name}"
                }
              }]
            }]
          }
        }""".stripMargin

      result ==== parseAsSnakeCase(jsonAsString)
    }
  }
}
