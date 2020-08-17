package io.paytouch.core.resources.products

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class ProductsUpdateFSpec extends ProductsFSpec {

  abstract class ProductsUpdateFSpecContext extends ProductResourceFSpecContext

  "POST /v1/products.update?product_id=<product-id>" in {

    "if request has valid token" in {

      "if product and its relations belong to same merchant" should {

        "update product, its relations and return 200" in new ProductResourceFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val product = Factory.simpleProduct(merchant, categories = Seq(category1)).create
          val supplier = Factory.supplier(merchant).create

          val categoryIds = Seq(category1.id)
          val supplierIds = Seq(supplier.id)

          val update = random[ProductUpdate].copy(categoryIds = Some(categoryIds), supplierIds = Some(supplierIds))

          Post(s"/v1/products.update?product_id=${product.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, product.id, Some(categoryIds), Some(supplierIds))
          }
        }

        "update product and update variants, location settings, their relations and return 200" in new ProductResourceFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val categoryIds = Seq(category1.id, category2.id)
          val supplier = Factory.supplier(merchant).create
          val supplierIds = Seq(supplier.id)

          val template = Factory.templateProduct(merchant, categories = Seq(category1), locations = Seq(rome)).create
          val oldVariant = Factory.variantProduct(merchant, template).create

          val locationOverrides = Map(rome.id -> Some(random[ProductLocationUpdate].copy(taxRateIds = Seq.empty)))

          val productVariantUpdates = {
            val variantProducts = random[VariantProductUpdate](variantSelections.size)
            variantSelections
              .zip(variantProducts)
              .map {
                case (vs, varPrd) =>
                  val variant = Factory.variantProduct(merchant, template).create
                  Factory.productLocation(variant, rome).create
                  varPrd.copy(
                    id = variant.id,
                    optionIds = vs,
                    upc = randomUpc,
                    sku = randomWords,
                    locationOverrides = locationOverrides,
                  )
              }
              .toIndexedSeq
          }

          val update = random[ProductUpdate].copy(
            categoryIds = Some(categoryIds),
            description = Some("description"),
            variants = Some(variants),
            variantProducts = Some(productVariantUpdates),
            locationOverrides = locationOverrides,
            supplierIds = Some(supplierIds),
          )

          Post(s"/v1/products.update?product_id=${template.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, template.id, Some(categoryIds), Some(supplierIds))

            articleDao.findById(oldVariant.id).await should beNone
          }
        }

        "update product and update existing variants and create new ones" in new ProductResourceFSpecContext {
          val product = Factory.templateProduct(merchant, locations = Seq(rome)).create

          val productVariantUpdates = {
            val variantProducts = random[VariantProductUpdate](variantSelections.size)
            variantSelections
              .zip(variantProducts)
              .map {
                case (vs, varPrd) =>
                  val variant = Factory.variantProduct(merchant, product, locations = Seq(rome)).create
                  varPrd.copy(id = variant.id, optionIds = vs, upc = randomUpc, sku = randomWords)
              }
              .toIndexedSeq
          }
          val productVariantCreation = random[VariantProductUpdate].copy(
            id = UUID.randomUUID,
            optionIds = variantSelections.head,
            upc = randomUpc,
            sku = randomWords + 15,
            price = None,
          )
          val productLocationUpdateWithOneCreation = productVariantUpdates :+ productVariantCreation
          val update = random[ProductUpdate]
            .copy(variants = Some(variants), variantProducts = Some(productLocationUpdateWithOneCreation))

          Post(s"/v1/products.update?product_id=${product.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, product.id)
          }
        }

        "update product completely changing variant types, options and variants" in new ProductResourceFSpecContext {
          val product = Factory.templateProduct(merchant, locations = Seq(rome)).create

          val variantType = Factory.variantOptionType(product).create
          val variantTypeOption1 = Factory.variantOption(product, variantType, "M").create
          val variantTypeOption2 = Factory.variantOption(product, variantType, "L").create

          val variantProduct1 = Factory.variantProduct(merchant, product, locations = Seq(rome)).create
          val productVariantOption1 = Factory.productVariantOption(variantProduct1, variantTypeOption1).create
          val variantProduct2 = Factory.variantProduct(merchant, product, locations = Seq(rome)).create
          val productVariantOption2 = Factory.productVariantOption(variantProduct2, variantTypeOption2).create

          val productVariantUpdates = {
            val variantProducts = random[VariantProductUpdate](variantSelections.size)
            variantSelections
              .zip(variantProducts)
              .map {
                case (vs, varProd) =>
                  varProd.copy(optionIds = vs, upc = randomUpc, sku = randomWords)
              }
              .toIndexedSeq
          }
          val update =
            random[ProductUpdate].copy(variants = Some(variants), variantProducts = Some(productVariantUpdates))

          Post(s"/v1/products.update?product_id=${product.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, product.id)

            variantOptionTypeDao.findById(variantType.id).await must beNone
            variantOptionDao.findByIds(Seq(variantTypeOption1.id, variantTypeOption2.id)).await must beEmpty
            productVariantOptionDao
              .findByIds(Seq(productVariantOption1.id, productVariantOption2.id))
              .await must beEmpty
            val deletedProducts = productDao.findDeletedByIds(Seq(variantProduct1.id, variantProduct2.id)).await
            deletedProducts.map(_.id) should containTheSameElementsAs(Seq(variantProduct1.id, variantProduct2.id))
          }
        }

        "update product and update variants, location settings, location tax rates, their relations and return 200" in new ProductResourceFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val categoryIds = Seq(category1.id, category2.id)
          val supplier = Factory.supplier(merchant).create
          val supplierIds = Seq(supplier.id)
          val taxRate = Factory.taxRate(merchant).create
          Factory.taxRateLocation(taxRate, rome).create

          val product = Factory.templateProduct(merchant, categories = Seq(category1), locations = Seq(rome)).create

          val locationOverrides =
            Map(rome.id -> Some(random[ProductLocationUpdate].copy(taxRateIds = Seq(taxRate.id))))

          val productVariantUpdates = {
            val variantProducts = random[VariantProductUpdate](variantSelections.size)
            variantSelections
              .zip(variantProducts)
              .map {
                case (vs, variantProd) =>
                  val variant = Factory.variantProduct(merchant, product, locations = Seq(rome)).create
                  variantProd.copy(
                    id = variant.id,
                    optionIds = vs,
                    upc = randomUpc,
                    sku = randomWords,
                    locationOverrides = locationOverrides,
                  )
              }
              .toIndexedSeq
          }
          val update = random[ProductUpdate].copy(
            categoryIds = Some(categoryIds),
            description = Some("description"),
            variants = Some(variants),
            variantProducts = Some(productVariantUpdates),
            locationOverrides = locationOverrides,
            supplierIds = Some(supplierIds),
          )

          Post(s"/v1/products.update?product_id=${product.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, product.id, Some(categoryIds), Some(supplierIds))
          }
        }

        "update a simple product with new variants and return 400" in new ProductResourceFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val categoryIds = Seq(category1.id, category2.id)
          val supplier = Factory.supplier(merchant).create
          val supplierIds = Seq(supplier.id)

          val product = Factory.simpleProduct(merchant, categories = Seq(category1), locations = Seq(rome)).create

          val locationOverrides = Map(rome.id -> Some(random[ProductLocationUpdate].copy(taxRateIds = Seq.empty)))

          val productVariantCreations = {
            val variantProducts = random[VariantProductUpdate](variantSelections.size)
            variantSelections.zip(variantProducts).map {
              case (vs, variantProd) =>
                variantProd
                  .copy(optionIds = vs, upc = randomUpc, sku = randomWords, locationOverrides = locationOverrides)
            }
          }
          val update = random[ProductUpdate].copy(
            categoryIds = Some(categoryIds),
            description = Some("description"),
            variants = Some(variants),
            variantProducts = Some(productVariantCreations),
            locationOverrides = locationOverrides,
            supplierIds = Some(supplierIds),
          )

          Post(s"/v1/products.update?product_id=${product.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
          }
        }

        "update product with no location settings" should {
          "leave the product location data intact and return 200" in new ProductResourceFSpecContext {
            val product = Factory.templateProduct(merchant).create
            val productLocation = Factory.productLocation(product, rome).create
            val update = random[ProductUpdate]

            Post(s"/v1/products.update?product_id=${product.id}", update)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              assertUpdate(update, product.id)

              productLocationDao.findById(productLocation.id).await.get ==== productLocation
            }
          }
        }

        "update product with a null location overrides" should {
          "remove the product location data and return 200" in new ProductResourceFSpecContext {
            val product = Factory.templateProduct(merchant).create
            val productLocation = Factory.productLocation(product, rome).create
            val locationOverrides = Map(rome.id -> None)
            val update =
              random[ProductUpdate].copy(description = Some("description"), locationOverrides = locationOverrides)

            Post(s"/v1/products.update?product_id=${product.id}", update)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              assertUpdate(update, product.id)

              productLocationDao.findById(productLocation.id).await ==== None
            }
          }
        }

        "update product with image uploads" in new ProductResourceFSpecContext {
          val product = Factory.simpleProduct(merchant).create
          val oldImageUpload = Factory
            .imageUpload(merchant, objectId = Some(product.id), imageUploadType = Some(ImageUploadType.Product))
            .create
          val imageUpload = Factory
            .imageUpload(merchant, objectId = Some(product.id), imageUploadType = Some(ImageUploadType.Product))
            .create

          val update = random[ProductUpdate].copy(imageUploadIds = Some(Seq(imageUpload.id)))

          Post(s"/v1/products.update?product_id=${product.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, product.id)

            imageUploadDao.findByObjectIds(Seq(product.id), ImageUploadType.Product).await.map(_.id) ==== Seq(
              imageUpload.id,
            )
          }
        }

        "update product with categories without deleting any existing catalog category" in new ProductResourceFSpecContext {
          val product = Factory.simpleProduct(merchant).create

          val systemCategory = Factory.systemCategory(defaultMenuCatalog).create

          val catalog = Factory.catalog(merchant).create
          val catalogCategory = Factory.catalogCategory(catalog).create
          Factory.productCategory(product, catalogCategory).create

          val update = random[ProductUpdate].copy(categoryIds = Some(Seq(systemCategory.id)))

          Post(s"/v1/products.update?product_id=${product.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, product.id, categories = Some(Seq(systemCategory.id)))

            val productCategories = productCategoryDao.findByProductId(product.id).await
            productCategories.map(_.categoryId) should containTheSameElementsAs(
              Seq(catalogCategory.id, systemCategory.id),
            )
          }
        }
      }

      "if product doesn't belong to current user's merchant" should {

        "not update product, its relations and return 404" in new ProductResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDefaultMenuCatalog =
            Factory.defaultMenuCatalog(competitor).create
          val competitorCategory1 = Factory.systemCategory(competitorDefaultMenuCatalog).create
          val competitorProduct = Factory.simpleProduct(competitor, categories = Seq(competitorCategory1)).create

          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val categoryIds = Seq(category1.id)

          val update = random[ProductUpdate].copy(categoryIds = Some(categoryIds))

          Post(s"/v1/products.update?product_id=${competitorProduct.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            val updatedProduct = productDao.findById(competitorProduct.id).await.get
            updatedProduct ==== competitorProduct

            productCategoryDao.findByProductId(competitorProduct.id).await.map(_.categoryId) ==== Seq(
              competitorCategory1.id,
            )
          }
        }
      }

      "if category doesn't belong to current user's merchant" should {

        "update product, update own categories, skip spurious categories and return 200" in new ProductResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDefaultMenuCatalog =
            Factory.defaultMenuCatalog(competitor).create
          val competitorCategory1 = Factory.systemCategory(competitorDefaultMenuCatalog).create

          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val product = Factory.simpleProduct(merchant, categories = Seq(category1)).create

          val update = random[ProductUpdate].copy(categoryIds = Some(Seq(category1.id, competitorCategory1.id)))

          Post(s"/v1/products.update?product_id=${product.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, product.id, categories = Some(Seq(category1.id)))
          }
        }
      }

      "if product variant does not belong to the merchant" should {
        "reject the request with a 404" in new ProductResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorProduct = Factory.simpleProduct(competitor).create

          val newProductId = UUID.randomUUID

          val productVariantCreations = {
            val variantProducts = random[VariantProductUpdate](variantSelections.size)
            variantSelections.zip(variantProducts).map {
              case (vs, variantProd) =>
                variantProd.copy(id = competitorProduct.id, optionIds = vs, upc = randomUpc, sku = randomWords)
            }
          }
          val update =
            random[ProductUpdate].copy(variantProducts = Some(productVariantCreations), variants = Some(variants))

          Post(s"/v1/products.update?product_id=$newProductId", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            productDao.findById(newProductId).await ==== None
          }
        }
      }

      "if a location id does not belong to the merchant" should {
        "reject the request with a 404" in new ProductResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorProduct = Factory.simpleProduct(competitor).create

          val locationOverrides = Map(rome.id -> Some(random[ProductLocationUpdate].copy(taxRateIds = Seq.empty)))

          val newProductId = UUID.randomUUID

          val productVariantCreations = {
            val variantProducts = random[VariantProductUpdate](variantSelections.size)
            variantSelections.zip(variantProducts).map {
              case (vs, variantProd) =>
                variantProd.copy(id = competitorProduct.id, optionIds = vs, upc = randomUpc, sku = randomWords)
            }
          }
          val update = random[ProductUpdate].copy(
            variantProducts = Some(productVariantCreations),
            variants = Some(variants),
            locationOverrides = locationOverrides,
          )

          Post(s"/v1/products.update?product_id=$newProductId", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            productDao.findById(newProductId).await ==== None
          }
        }
      }

      "if an image upload id does not belong to the merchant" should {
        "reject the request with a 404" in new ProductResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorImageUpload = Factory.imageUpload(competitor).create

          val newProductId = UUID.randomUUID

          val update = random[ProductUpdate].copy(imageUploadIds = Some(Seq(competitorImageUpload.id)))

          Post(s"/v1/products.update?product_id=$newProductId", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            productDao.findById(newProductId).await ==== None
          }
        }
      }

      "if a image upload id does exists" should {
        "reject the request with a 404" in new ProductResourceFSpecContext {
          val newProductId = UUID.randomUUID

          val update = random[ProductUpdate].copy(imageUploadIds = Some(Seq(UUID.randomUUID)))

          Post(s"/v1/products.update?product_id=$newProductId", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            productDao.findById(newProductId).await ==== None
          }
        }
      }

      "if a tax rate is assigned to a product location and it's not active in that location" should {
        "reject the request with a 400" in new ProductResourceFSpecContext {
          val taxRate = Factory.taxRate(merchant).create
          val product = Factory.simpleProduct(merchant).create

          val locationOverrides =
            Map(rome.id -> Some(random[ProductLocationUpdate].copy(taxRateIds = Seq(taxRate.id))))

          val update =
            random[ProductUpdate].copy(locationOverrides = locationOverrides, categoryIds = None, supplierIds = None)

          Post(s"/v1/products.update?product_id=${product.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("InvalidTaxRateLocationAssociation")
          }
        }
      }

      "if a kitchen id is isn't valid" should {
        "reject the request with a 400 - kitchen" in new ProductResourceFSpecContext {
          val taxRate = Factory.taxRate(merchant).create
          val product = Factory.simpleProduct(merchant).create

          val locationOverrides =
            Map(
              rome.id -> Some(
                random[ProductLocationUpdate].copy(taxRateIds = Seq.empty, routeToKitchenId = Some(UUID.randomUUID)),
              ),
            )

          val update =
            random[ProductUpdate].copy(locationOverrides = locationOverrides, categoryIds = None, supplierIds = None)

          Post(s"/v1/products.update?product_id=${product.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
            assertErrorCode("NonAccessibleKitchenIds")
          }
        }
      }
    }

    "if product has been deleted" should {

      "reject the request with a 404" in new ProductResourceFSpecContext {
        val deletedProduct = Factory.templateProduct(merchant, deletedAt = Some(UtcTime.now)).create
        val update = random[ProductUpdate]
        Post(s"/v1/products.update?product_id=${deletedProduct.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new ProductResourceFSpecContext {
        val newProductId = UUID.randomUUID
        val update = random[ProductUpdate]
        Post(s"/v1/products.update?product_id=$newProductId", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
