package io.paytouch.core.resources.products

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.enums.KitchenType
import io.paytouch.core.entities.{ Product => ProductEntity, _ }
import io.paytouch.core.utils.{ AppTokenFixtures, FixtureDaoFactory => Factory }

class ProductsGetFSpec extends ProductsFSpec {

  abstract class ProductsGetFSpecContext extends ProductResourceFSpecContext

  "GET /v1/products.get?product_id=<product-id>" in {

    "if request has valid token" in {

      "if product belongs to same merchant" in {

        "with no parameters" should {

          "return product, no relations and return 200" in new ProductsGetFSpecContext {
            val product = Factory.simpleProduct(merchant).create
            Factory.productLocation(product, rome).create

            Get(s"/v1/products.get?product_id=${product.id}").addHeader(authorizationHeader) ~> routes ~> check {
              val productResponse = responseAs[ApiResponse[ProductEntity]].data

              assertResponse(productResponse, product, locationIds = Seq(rome.id))
            }
          }

          "return product with image uploads, no relations and return 200" in new ProductsGetFSpecContext {
            val product = Factory.simpleProduct(merchant).create
            val imageUpload1 =
              Factory.imageUpload(merchant, Some(product.id), Some(Map("a" -> "1", "b" -> "2"))).create
            val imageUpload2 =
              Factory.imageUpload(merchant, Some(product.id), Some(Map("c" -> "3", "d" -> "4"))).create

            Get(s"/v1/products.get?product_id=${product.id}").addHeader(authorizationHeader) ~> routes ~> check {
              val productResponse = responseAs[ApiResponse[ProductEntity]].data

              assertResponse(productResponse, product, imageUploads = Seq(imageUpload1, imageUpload2))
            }
          }

          "return product with bundle sets no relations and return 200" in new ProductsGetFSpecContext {
            val productForBundleOption = Factory.simpleProduct(merchant).create
            val bundle = Factory.comboProduct(merchant).create
            val bundleSet = Factory.bundleSet(bundle).create
            val bundleOption = Factory.bundleOption(bundleSet, productForBundleOption).create

            Get(s"/v1/products.get?product_id=${bundle.id}").addHeader(authorizationHeader) ~> routes ~> check {
              val productResponse = responseAs[ApiResponse[ProductEntity]].data

              assertResponse(productResponse, bundle, bundleSets = Map(bundleSet -> Seq(bundleOption)))
            }
          }
        }

        "with expand parameters" should {

          "return template product, its relations and return 200" in new ProductsGetFSpecContext {
            val newYork = Factory.location(merchant).create
            Factory.userLocation(user, newYork).create

            val catalog = Factory.catalog(merchant).create
            val catalogCategory = Factory.catalogCategory(catalog).create

            val tshirtsCategory = Factory.systemCategory(defaultMenuCatalog, locations = Seq(rome)).create
            val modifierSet = Factory.modifierSet(merchant).create

            val product = Factory.templateProduct(merchant, categories = Seq(tshirtsCategory, catalogCategory)).create
            val variantType = Factory.variantOptionType(product).create
            val variantTypeOption1 = Factory.variantOption(product, variantType, "M").create
            val variantTypeOption2 = Factory.variantOption(product, variantType, "L").create

            val productVariant1 = Factory.variantProduct(merchant, product).create
            Factory.productVariantOption(productVariant1, variantTypeOption1).create
            val productVariant2 = Factory.variantProduct(merchant, product).create
            Factory.productVariantOption(productVariant2, variantTypeOption2).create

            Factory.modifierSetProduct(modifierSet, product, position = Some(3)).create
            val taxRate =
              Factory.taxRate(merchant, name = None, locations = Seq(london), products = Seq(product)).create

            val productVariant1AtLondon = Factory.productLocation(productVariant1, london).create
            val productVariant1AtNewYork = Factory.productLocation(productVariant1, newYork).create
            val productVariant2AtNewYork = Factory.productLocation(productVariant2, newYork).create

            val productVariant1AtLondonStock = Factory.stock(productVariant1AtLondon, Some(2.0)).create
            val productVariant1AtNewYorkStock = Factory.stock(productVariant1AtNewYork, Some(1.0)).create
            val productVariant2AtNewYorkStock = Factory.stock(productVariant2AtNewYork, Some(4.0)).create

            val supplier = Factory.supplier(merchant).create
            Factory.supplierProduct(supplier, product).create

            Get(
              s"/v1/products.get?product_id=${product.id}&expand[]=categories,category_ids,variants,modifiers,modifier_ids,tax_rates,tax_rate_locations,tax_rate_ids,stock_level,suppliers",
            ).addHeader(authorizationHeader) ~> routes ~> check {
              val productResponse = responseAs[ApiResponse[ProductEntity]].data

              assertResponse(
                productResponse,
                product,
                systemCategories = Some(Seq(tshirtsCategory)),
                systemCategoryIds = Some(Seq(tshirtsCategory.id)),
                locationIds = Seq(london.id),
                variantIds = Seq(productVariant1.id, productVariant2.id),
                modifierSets = Some(Seq(modifierSet)),
                modifierSetIds = Some(Seq(modifierSet.id)),
                modifierPositions = None,
                taxRatesMap = Some(Map(london.id -> Seq(taxRate))),
                taxRateIdsMap = Some(Map(london.id -> Seq(taxRate.id))),
                withTaxRateLocations = true,
                stockLevel = Some(7.0),
                variantStockLevels = Seq(Some(3.0), Some(4.0)),
                optionIds = Seq(variantTypeOption1.id, variantTypeOption2.id),
                supplierIds = Seq(supplier.id),
              )
            }
          }

          "return variant product, its relations and return 200" in new ProductsGetFSpecContext {
            val newYork = Factory.location(merchant).create
            Factory.userLocation(user, newYork).create

            val catalog = Factory.catalog(merchant).create
            val catalogCategory = Factory.catalogCategory(catalog).create

            val tshirtsCategory = Factory.systemCategory(defaultMenuCatalog, locations = Seq(rome)).create
            val modifierSet = Factory.modifierSet(merchant).create

            val product = Factory.templateProduct(merchant, categories = Seq(tshirtsCategory, catalogCategory)).create
            val variantType = Factory.variantOptionType(product).create
            val variantTypeOption1 = Factory.variantOption(product, variantType, "M").create
            val variantTypeOption2 = Factory.variantOption(product, variantType, "L").create

            val productVariant1 = Factory.variantProduct(merchant, product).create
            Factory.productVariantOption(productVariant1, variantTypeOption1).create
            val productVariant2 = Factory.variantProduct(merchant, product).create
            Factory.productVariantOption(productVariant2, variantTypeOption2).create

            Factory.modifierSetProduct(modifierSet, product).create
            val taxRate =
              Factory.taxRate(merchant, name = None, locations = Seq(london), products = Seq(product)).create

            val productVariant1AtLondon = Factory.productLocation(productVariant1, london).create
            val productVariant1AtNewYork = Factory.productLocation(productVariant1, newYork).create
            val productVariant2AtNewYork = Factory.productLocation(productVariant2, newYork).create

            val productVariant1AtLondonStock = Factory.stock(productVariant1AtLondon, Some(2.0)).create
            val productVariant1AtNewYorkStock = Factory.stock(productVariant1AtNewYork, Some(1.0)).create
            val productVariant2AtNewYorkStock = Factory.stock(productVariant2AtNewYork, Some(4.0)).create

            val supplier = Factory.supplier(merchant).create
            Factory.supplierProduct(supplier, product).create

            Get(
              s"/v1/products.get?product_id=${productVariant1.id}&expand[]=categories,category_ids,variants,modifiers,modifier_ids,tax_rates,tax_rate_locations,tax_rate_ids,stock_level,suppliers",
            ).addHeader(authorizationHeader) ~> routes ~> check {
              val productResponse = responseAs[ApiResponse[ProductEntity]].data

              assertResponse(
                productResponse,
                productVariant1,
                systemCategories = Some(Seq(tshirtsCategory)),
                systemCategoryIds = Some(Seq(tshirtsCategory.id)),
                locationIds = Seq(london.id, newYork.id),
                variantIds = Seq.empty,
                modifierSets = Some(Seq(modifierSet)),
                modifierSetIds = Some(Seq(modifierSet.id)),
                taxRatesMap = Some(Map(london.id -> Seq(taxRate), newYork.id -> Seq.empty)),
                taxRateIdsMap = Some(Map(london.id -> Seq(taxRate.id), newYork.id -> Seq.empty)),
                withTaxRateLocations = true,
                stockLevel = Some(3.0),
                variantStockLevels = Seq.empty,
                optionIds = Seq.empty,
                singleVariantOptionIds = Seq(variantTypeOption1.id),
                supplierIds = Seq(supplier.id),
              )
            }
          }

          "return simple product, its relations with empty variants and return 200" in new ProductsGetFSpecContext {
            val tshirts = Factory.systemCategory(defaultMenuCatalog, locations = Seq(london)).create

            val catalog = Factory.catalog(merchant).create
            val catalogCategory = Factory.catalogCategory(catalog).create

            val simpleProduct =
              Factory
                .simpleProduct(merchant, categories = Seq(tshirts, catalogCategory), locations = Seq(london))
                .create
            val taxRate =
              Factory.taxRate(merchant, name = None, locations = Seq(london), products = Seq(simpleProduct)).create
            val modifierSet = Factory.modifierSet(merchant).create
            Factory.modifierSetProduct(modifierSet, simpleProduct).create
            val supplier = Factory.supplier(merchant).create
            Factory.supplierProduct(supplier, simpleProduct).create

            Get(
              s"/v1/products.get?product_id=${simpleProduct.id}&expand[]=categories,category_ids,locations,variants,modifiers,modifier_ids,tax_rates,tax_rate_ids,suppliers",
            ).addHeader(authorizationHeader) ~> routes ~> check {
              val productResponse = responseAs[ApiResponse[ProductEntity]].data

              assertResponse(
                productResponse,
                simpleProduct,
                systemCategories = Some(Seq(tshirts)),
                systemCategoryIds = Some(Seq(tshirts.id)),
                locationIds = Seq(london.id),
                modifierSets = Some(Seq(modifierSet)),
                modifierSetIds = Some(Seq(modifierSet.id)),
                taxRatesMap = Some(Map(london.id -> Seq(taxRate))),
                taxRateIdsMap = Some(Map(london.id -> Seq(taxRate.id))),
                supplierIds = Seq(supplier.id),
              )
            }
          }

          "return simple product with backward compatibility routing fields values derived from location overrides" in new ProductsGetFSpecContext {
            val kitchen = Factory.kitchen(london, `type` = Some(KitchenType.Bar)).create

            val simpleProduct = Factory.simpleProduct(merchant).create
            Factory.productLocation(simpleProduct, london, routeToKitchen = Some(kitchen)).create

            val expectedSimpleProduct = simpleProduct.copy(orderRoutingBar = true)

            Get(s"/v1/products.get?product_id=${simpleProduct.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val productResponse = responseAs[ApiResponse[ProductEntity]].data

              assertResponse(productResponse, expectedSimpleProduct, locationIds = Seq(london.id))
            }
          }
        }

        "with expand[]=categories" should {

          "return product with all associated categories (regardless of category locations) and return 200" in new ProductsGetFSpecContext {
            val categoryWithLocations = Factory.systemCategory(defaultMenuCatalog, locations = Seq(rome)).create
            val categoryWithoutLocations = Factory.systemCategory(defaultMenuCatalog).create

            val catalog = Factory.catalog(merchant).create
            val catalogCategory = Factory.catalogCategory(catalog).create

            val product =
              Factory
                .simpleProduct(
                  merchant,
                  categories = Seq(categoryWithLocations, categoryWithoutLocations, catalogCategory),
                )
                .create

            Get(s"/v1/products.get?product_id=${product.id}&expand[]=categories")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val productResponse = responseAs[ApiResponse[ProductEntity]].data

              assertResponse(
                productResponse,
                product,
                systemCategories = Some(Seq(categoryWithLocations, categoryWithoutLocations)),
              )
            }
          }
        }

        "with expand[]=category_positions" should {

          "return product with all associated categories (regardless of category locations) and return 200" in new ProductsGetFSpecContext {

            val product = Factory.simpleProduct(merchant).create
            val category = Factory.systemCategory(defaultMenuCatalog, locations = Seq(rome)).create

            val catalog = Factory.catalog(merchant).create
            val catalogCategory = Factory.catalogCategory(catalog).create

            val productCategory = Factory.productCategory(product, category, Some(5)).create

            val productCatalogCategory = Factory.productCategory(product, catalogCategory, Some(10)).create

            Get(s"/v1/products.get?product_id=${product.id}&expand[]=category_positions")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val productResponse = responseAs[ApiResponse[ProductEntity]].data

              assertResponse(productResponse, product, productSystemCategories = Seq(productCategory))
            }
          }
        }

        "with expand[]=catalog_categories" should {

          "return product with all associated catalog categories and return 200" in new ProductsGetFSpecContext {
            val categoryWithLocations = Factory.systemCategory(defaultMenuCatalog, locations = Seq(rome)).create
            val categoryWithoutLocations = Factory.systemCategory(defaultMenuCatalog).create

            val catalog = Factory.catalog(merchant).create
            val catalogCategory = Factory.catalogCategory(catalog).create

            val product =
              Factory
                .simpleProduct(
                  merchant,
                  categories = Seq(categoryWithLocations, categoryWithoutLocations, catalogCategory),
                )
                .create

            Get(s"/v1/products.get?product_id=${product.id}&expand[]=catalog_categories")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val productResponse = responseAs[ApiResponse[ProductEntity]].data

              assertResponse(productResponse, product, catalogCategories = Some(Seq(catalogCategory)))
            }
          }
        }

        "with expand[]=catalog_category_options" should {

          "return product with all associated catalog categories and return 200" in new ProductsGetFSpecContext {

            val product = Factory.simpleProduct(merchant).create
            val systemCategory = Factory.systemCategory(defaultMenuCatalog, locations = Seq(rome)).create

            val catalog = Factory.catalog(merchant).create
            val catalogCategory = Factory.catalogCategory(catalog).create

            val productSystemCategory = Factory.productCategory(product, systemCategory, Some(5)).create
            val productCatalogCategory = Factory.productCategory(product, catalogCategory, Some(10)).create
            val productCatalogCategoryOption = Factory.productCategoryOption(productCatalogCategory).create
            val productCatalogCategoryOptionExpectation = CatalogCategoryOption(
              categoryId = productCatalogCategory.categoryId,
              deliveryEnabled = productCatalogCategoryOption.deliveryEnabled,
              takeAwayEnabled = productCatalogCategoryOption.takeAwayEnabled,
            )

            Get(s"/v1/products.get?product_id=${product.id}&expand[]=catalog_category_options")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val productResponse = responseAs[ApiResponse[ProductEntity]].data

              assertResponse(
                productResponse,
                product,
                productCategoryOptions = Seq(productCatalogCategoryOptionExpectation),
              )
            }
          }
        }

        "with expand[]=catalog_category_positions" should {

          "return product with all associated catalog categories and return 200" in new ProductsGetFSpecContext {

            val product = Factory.simpleProduct(merchant).create
            val systemCategory = Factory.systemCategory(defaultMenuCatalog, locations = Seq(rome)).create

            val catalog = Factory.catalog(merchant).create
            val catalogCategory = Factory.catalogCategory(catalog).create

            val productSystemCategory = Factory.productCategory(product, systemCategory, Some(5)).create
            val productCatalogCategory = Factory.productCategory(product, catalogCategory, Some(10)).create

            Get(s"/v1/products.get?product_id=${product.id}&expand[]=catalog_category_positions")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val productResponse = responseAs[ApiResponse[ProductEntity]].data

              assertResponse(
                productResponse,
                product,
                productCatalogCategories = Seq(productSystemCategory, productCatalogCategory),
              )
            }
          }
        }

        "with expand[]=locations,tax_rate_ids" should {

          "return product with tax_rate_ids expanded" in new ProductsGetFSpecContext {
            val simpleProduct =
              Factory.simpleProduct(merchant, locations = Seq(london)).create
            val taxRate =
              Factory.taxRate(merchant, name = None, locations = Seq(london), products = Seq(simpleProduct)).create

            Get(s"/v1/products.get?product_id=${simpleProduct.id}&expand[]=locations,tax_rate_ids")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val productResponse = responseAs[ApiResponse[ProductEntity]].data

              assertResponse(
                productResponse,
                simpleProduct,
                locationIds = Seq(london.id),
                taxRateIdsMap = Some(Map(london.id -> Seq(taxRate.id))),
              )
            }
          }
        }

        "with expand[]=recipe_details" should {

          "return a recipe with expanded recipe_details" in new ProductsGetFSpecContext {

            val recipe = Factory.comboPart(merchant).create

            val recipeDetail1 = Factory.recipeDetail(recipe).create
            val recipeDetail2 = Factory.recipeDetail(recipe).create
            val recipeDetail3 = Factory.recipeDetail(recipe).create
            val recipeDetails = Seq(recipeDetail1, recipeDetail2, recipeDetail3)

            Get(s"/v1/products.get?product_id=${recipe.id}&expand[]=recipe_details")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val productResponse = responseAs[ApiResponse[ProductEntity]].data

              assertResponse(productResponse, recipe, recipeDetails = Some(recipeDetails))
            }
          }
        }

        "with expand[]=reorder_amount" should {

          "return a simple product with expanded reorder_amount" in new ProductsGetFSpecContext {
            val bag = Factory.simpleProduct(merchant).create
            val bagLondon = Factory.productLocation(bag, london).create
            val stock = Factory.stock(bagLondon).create

            Get(s"/v1/products.get?product_id=${bag.id}&expand[]=reorder_amount")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[ProductEntity]].data

              assertResponse(entity, bag, locationIds = Seq(london.id), reorderAmount = Some(stock.reorderAmount))
            }
          }
        }

        "with expand[]=modifier_positions" should {

          "return a simple product with expanded modifier_positions" in new ProductsGetFSpecContext {
            val product = Factory.simpleProduct(merchant).create
            val modifierSet = Factory.modifierSet(merchant).create
            Factory.modifierSetProduct(modifierSet, product, position = Some(3)).create

            val expectedPositions = Seq(
              ModifierPosition(modifierSetId = modifierSet.id, position = Some(3)),
            )

            Get(s"/v1/products.get?product_id=${product.id}&expand[]=modifier_positions")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[ProductEntity]].data

              assertResponse(
                entity,
                product,
                modifierPositions = Some(expectedPositions),
              )
            }
          }
        }
      }

      "if product doesn't belong to same merchant" in {
        "return 404" in new ProductsGetFSpecContext {
          val competitor = Factory.merchant.create
          val competitorProduct = Factory.simpleProduct(competitor).create

          Get(s"/v1/products.get?product_id=${competitorProduct.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if request has valid app token" should {
      "return the product" in new ProductsGetFSpecContext with AppTokenFixtures {
        val product = Factory.simpleProduct(merchant).create
        Factory.productLocation(product, rome).create

        Get(s"/v1/products.get?product_id=${product.id}").addHeader(appAuthorizationHeader) ~> routes ~> check {
          assertStatusOK()
          val productResponse = responseAs[ApiResponse[ProductEntity]].data

          assertResponse(productResponse, product, locationIds = Seq(rome.id))
        }
      }
    }

    "if request has invalid token" should {
      "reject request" in new ProductsGetFSpecContext {
        val product = Factory.simpleProduct(merchant).create
        Factory.productLocation(product, rome).create

        Get(s"/v1/products.get?product_id=${product.id}").addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
