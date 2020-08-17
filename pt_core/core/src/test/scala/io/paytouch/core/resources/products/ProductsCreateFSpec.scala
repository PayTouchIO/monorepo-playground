package io.paytouch.core.resources.products

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ProductsCreateFSpec extends ProductsFSpec {

  abstract class ProductsCreateFSpecContext extends ProductResourceFSpecContext

  "POST /v1/products.create" in {

    "if request has valid token" in {

      "if relations belong to same merchant" should {

        "create simple product without variants, its relations and return 201" in new ProductsCreateFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val supplier = Factory.supplier(merchant).create

          val newProductId = UUID.randomUUID
          val categoryIds = Seq(category1.id, category2.id)
          val supplierIds = Seq(supplier.id)

          val creation = random[ProductCreation].copy(categoryIds = categoryIds, supplierIds = supplierIds)

          Post(s"/v1/products.create?product_id=$newProductId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            assertCreation(creation, newProductId, Some(categoryIds), Some(supplierIds))
            assertSimpleType(newProductId)
          }
        }

        "create product with variants, their relations and return 201" in new ProductsCreateFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val supplier = Factory.supplier(merchant).create

          val newProductId = UUID.randomUUID
          val categoryIds = Seq(category1.id, category2.id)
          val supplierIds = Seq(supplier.id)

          val productVariantCreations: Seq[VariantProductCreation] = {
            val variantProducts = random[VariantProductCreation](variantSelections.size)

            variantSelections.zip(variantProducts).map {
              case (vs, varPrdCreation) =>
                varPrdCreation
                  .copy(id = UUID.randomUUID, optionIds = vs, upc = randomUpc, sku = randomWords + varPrdCreation.id)
            }
          }

          val creation = random[ProductCreation].copy(
            categoryIds = categoryIds,
            variants = variants,
            variantProducts = productVariantCreations,
            supplierIds = supplierIds,
          )

          Post(s"/v1/products.create?product_id=$newProductId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            assertCreation(creation, newProductId, Some(categoryIds), Some(supplierIds))
            assertTemplateType(newProductId)
          }
        }

        "create product with variants, location settings, their relations and return 201" in new ProductsCreateFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val supplier = Factory.supplier(merchant).create

          val newProductId = UUID.randomUUID
          val categoryIds = Seq(category1.id, category2.id)
          val supplierIds = Seq(supplier.id)

          val locationOverrides = Map(rome.id -> Some(random[ProductLocationUpdate].copy(taxRateIds = Seq.empty)))

          val productVariantCreations: Seq[VariantProductCreation] = {
            val variantProducts = random[VariantProductCreation](variantSelections.size)

            variantSelections.zip(variantProducts).map {
              case (vs, varPrdCreation) =>
                varPrdCreation
                  .copy(id = UUID.randomUUID, optionIds = vs, upc = randomUpc, sku = randomWords + varPrdCreation.id)
            }
          }
          val creation = random[ProductCreation].copy(
            categoryIds = categoryIds,
            variants = variants,
            variantProducts = productVariantCreations,
            locationOverrides = locationOverrides,
            supplierIds = supplierIds,
          )

          Post(s"/v1/products.create?product_id=$newProductId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            assertCreation(creation, newProductId, Some(categoryIds), Some(supplierIds))
          }
        }

        "create product with variants, location settings, location tax rates, their relations and return 201" in new ProductsCreateFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val supplier = Factory.supplier(merchant).create
          val taxRate = Factory.taxRate(merchant).create
          Factory.taxRateLocation(taxRate, rome).create

          val newProductId = UUID.randomUUID
          val categoryIds = Seq(category1.id, category2.id)
          val supplierIds = Seq(supplier.id)

          val locationOverrides =
            Map(rome.id -> Some(random[ProductLocationUpdate].copy(taxRateIds = Seq(taxRate.id))))

          val productVariantCreations: Seq[VariantProductCreation] = {
            val variantProducts = random[VariantProductCreation](variantSelections.size)

            variantSelections.zip(variantProducts).map {
              case (vs, varPrdCreation) =>
                varPrdCreation
                  .copy(id = UUID.randomUUID, optionIds = vs, upc = randomUpc, sku = randomWords + varPrdCreation.id)
            }
          }
          val creation = random[ProductCreation].copy(
            categoryIds = categoryIds,
            variants = variants,
            variantProducts = productVariantCreations,
            locationOverrides = locationOverrides,
            supplierIds = supplierIds,
          )

          Post(s"/v1/products.create?product_id=$newProductId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            assertCreation(creation, newProductId, Some(categoryIds), Some(supplierIds))
          }
        }

        "create product with image uploads" in new ProductResourceFSpecContext {
          val imageUpload1 = Factory.imageUpload(merchant).create
          val imageUpload2 = Factory.imageUpload(merchant).create
          val imageUploadIds = Seq(imageUpload1.id, imageUpload2.id)

          val newProductId = UUID.randomUUID
          val creation = random[ProductCreation].copy(imageUploadIds = imageUploadIds)

          Post(s"/v1/products.create?product_id=$newProductId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            assertCreation(creation, newProductId)

            imageUploadDao
              .findByObjectIds(Seq(newProductId), ImageUploadType.Product)
              .await
              .map(_.id)
              .toSet ==== imageUploadIds.toSet
          }
        }

      }

      "if product doesn't belong to current user's merchant" should {

        "not create product, its relations and return 404" in new ProductResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDefaultMenuCatalog =
            Factory.defaultMenuCatalog(competitor).create
          val competitorCategory1 = Factory.systemCategory(competitorDefaultMenuCatalog).create
          val competitorProduct = Factory.simpleProduct(competitor, categories = Seq(competitorCategory1)).create

          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val categoryIds = Seq(category1.id)

          val productUpdate = random[ProductCreation].copy(categoryIds = categoryIds)

          Post(s"/v1/products.create?product_id=${competitorProduct.id}", productUpdate)
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

      "if a variant does not belong to the product" should {

        "reject the request with a 404" in new ProductResourceFSpecContext {
          val newProductId = UUID.randomUUID

          val product = Factory.templateProduct(merchant).create
          val invalidVariantOptionType = Factory.variantOptionType(product).create

          val invalidVariantType =
            buildVariantTypeWithOptions("color", Seq("yellow", "blue")).copy(id = invalidVariantOptionType.id)

          val invalidVariants = Seq(invalidVariantType, variantType2)

          val invalidVariantSelections = invalidVariants.map(_.options.map(_.id))

          val productVariantCreations: Seq[VariantProductCreation] = {
            val variantProducts = random[VariantProductCreation](variantSelections.size)

            invalidVariantSelections.zip(variantProducts).map {
              case (vs, varPrdCreation) =>
                varPrdCreation
                  .copy(id = UUID.randomUUID, optionIds = vs, upc = randomUpc, sku = randomWords + varPrdCreation.id)
            }
          }

          val creation =
            random[ProductCreation].copy(variantProducts = productVariantCreations, variants = invalidVariants)

          Post(s"/v1/products.create?product_id=$newProductId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            productDao.findById(newProductId).await ==== None
          }
        }
      }

      "if product variant does not belong to the merchant" should {
        "reject the request with a 404" in new ProductResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorProduct = Factory.simpleProduct(competitor).create

          val newProductId = UUID.randomUUID

          val productVariantCreations: Seq[VariantProductCreation] = {
            val variantProducts = random[VariantProductCreation](variantSelections.size)

            variantSelections.zip(variantProducts).map {
              case (vs, varPrdCreation) =>
                varPrdCreation.copy(
                  id = competitorProduct.id,
                  optionIds = vs,
                  upc = randomUpc,
                  sku = randomWords + varPrdCreation.id,
                )
            }
          }
          val creation = random[ProductCreation].copy(variantProducts = productVariantCreations, variants = variants)

          Post(s"/v1/products.create?product_id=$newProductId", creation)
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

          val productVariantCreations: Seq[VariantProductCreation] = {
            val variantProducts = random[VariantProductCreation](variantSelections.size)

            variantSelections.zip(variantProducts).map {
              case (vs, varPrdCreation) =>
                varPrdCreation.copy(
                  id = competitorProduct.id,
                  optionIds = vs,
                  upc = randomUpc,
                  sku = randomWords + varPrdCreation.id,
                )
            }
          }

          val creation = random[ProductCreation].copy(
            variantProducts = productVariantCreations,
            variants = variants,
            locationOverrides = locationOverrides,
          )

          Post(s"/v1/products.create?product_id=$newProductId", creation)
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

          val creation = random[ProductCreation].copy(imageUploadIds = Seq(competitorImageUpload.id))

          Post(s"/v1/products.create?product_id=$newProductId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            productDao.findById(newProductId).await ==== None
          }
        }
      }

      "if a image upload id does exists" should {
        "reject the request with a 404" in new ProductResourceFSpecContext {
          val newProductId = UUID.randomUUID

          val creation = random[ProductCreation].copy(imageUploadIds = Seq(UUID.randomUUID))

          Post(s"/v1/products.create?product_id=$newProductId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            productDao.findById(newProductId).await ==== None
          }
        }
      }

      "if a tax rate is assigned to a product location and it's not active in that location" should {
        "reject the request with a 400" in new ProductResourceFSpecContext {
          {
            val taxRate = Factory.taxRate(merchant).create
            val newProductId = UUID.randomUUID

            val locationOverrides =
              Map(rome.id -> Some(random[ProductLocationUpdate].copy(taxRateIds = Seq(taxRate.id))))

            val creation = random[ProductCreation].copy(
              locationOverrides = locationOverrides,
              categoryIds = Seq.empty,
              supplierIds = Seq.empty,
            )

            Post(s"/v1/products.create?product_id=$newProductId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)
              assertErrorCode("InvalidTaxRateLocationAssociation")
            }
          }
        }
      }

      "if an invalid kitchen id is sent" should {
        "reject the request with a 404" in new ProductResourceFSpecContext {
          {
            val newProductId = UUID.randomUUID

            val locationOverrides =
              Map(
                rome.id -> Some(
                  random[ProductLocationUpdate].copy(taxRateIds = Seq.empty, routeToKitchenId = Some(UUID.randomUUID)),
                ),
              )

            val creation = random[ProductCreation].copy(
              locationOverrides = locationOverrides,
              categoryIds = Seq.empty,
              supplierIds = Seq.empty,
            )

            Post(s"/v1/products.create?product_id=$newProductId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
              assertErrorCode("NonAccessibleKitchenIds")
            }
          }
        }
      }

      "if the sku contains spaces" should {
        "reject the request with a 400" in new ProductResourceFSpecContext {
          val newProductId = UUID.randomUUID

          val creation = random[ProductCreation].copy(sku = "44444 222")

          Post(s"/v1/products.create?product_id=$newProductId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCodesAtLeastOnce("InvalidSku")
          }
        }
      }

      "if the upc contains spaces" should {
        "reject the request with a 400" in new ProductResourceFSpecContext {
          val newProductId = UUID.randomUUID

          val creation = random[ProductCreation].copy(upc = "44444 222")

          Post(s"/v1/products.create?product_id=$newProductId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCodesAtLeastOnce("InvalidUpc")
          }
        }
      }

      "if the upc is duplicated" should {
        "reject the request with a 400" in new ProductResourceFSpecContext {
          val upc = "44442222"
          val existingProduct = Factory.simpleProduct(merchant, upc = Some(upc)).create

          val newProductId = UUID.randomUUID
          val creation = random[ProductCreation].copy(upc = upc)

          Post(s"/v1/products.create?product_id=$newProductId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCodesAtLeastOnce("AlreadyTakenUpc")
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new ProductResourceFSpecContext {
        val newProductId = UUID.randomUUID
        val creation = random[ProductCreation]
        Post(s"/v1/products.create?product_id=$newProductId", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
