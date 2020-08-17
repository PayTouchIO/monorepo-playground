package io.paytouch.core.resources.products

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class PartsCreateFSpec extends PartsFSpec {

  abstract class PartsCreateFSpecContext extends PartResourceFSpecContext

  "POST /v1/parts.create" in {

    "if request has valid token" in {

      "if relations belong to same merchant" should {

        "create simple part without variants, its relations and return 201" in new PartsCreateFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val supplier = Factory.supplier(merchant).create

          val newPartId = UUID.randomUUID
          val categoryIds = Seq(category1.id, category2.id)
          val supplierIds = Seq(supplier.id)

          val creation = random[PartCreation].copy(categoryIds = categoryIds, supplierIds = supplierIds)

          Post(s"/v1/parts.create?part_id=$newPartId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            assertCreation(creation, newPartId, Some(categoryIds), Some(supplierIds))
            assertSimpleType(newPartId)
          }
        }

        "create part with variants, their relations and return 201" in new PartsCreateFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val supplier = Factory.supplier(merchant).create

          val newPartId = UUID.randomUUID
          val categoryIds = Seq(category1.id, category2.id)
          val supplierIds = Seq(supplier.id)

          val partVariantCreations: Seq[VariantPartCreation] = {
            val variantParts = random[VariantPartCreation](variantSelections.size)

            variantSelections.zip(variantParts).map {
              case (vs, varPrdCreation) =>
                varPrdCreation
                  .copy(id = UUID.randomUUID, optionIds = vs, upc = randomUpc, sku = randomWords + varPrdCreation.id)
            }
          }

          val creation = random[PartCreation].copy(
            categoryIds = categoryIds,
            variants = variants,
            variantProducts = partVariantCreations,
            supplierIds = supplierIds,
          )

          Post(s"/v1/parts.create?part_id=$newPartId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            assertCreation(creation, newPartId, Some(categoryIds), Some(supplierIds))
            assertTemplateType(newPartId)
          }
        }

        "create part with variants, location settings, their relations and return 201" in new PartsCreateFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val supplier = Factory.supplier(merchant).create

          val newPartId = UUID.randomUUID
          val categoryIds = Seq(category1.id, category2.id)
          val supplierIds = Seq(supplier.id)

          val partVariantCreations: Seq[VariantPartCreation] = {
            val variantParts = random[VariantPartCreation](variantSelections.size)

            variantSelections.zip(variantParts).map {
              case (vs, varPrdCreation) =>
                varPrdCreation
                  .copy(id = UUID.randomUUID, optionIds = vs, upc = randomUpc, sku = randomWords + varPrdCreation.id)
            }
          }
          val creation = random[PartCreation].copy(
            categoryIds = categoryIds,
            variants = variants,
            variantProducts = partVariantCreations,
            supplierIds = supplierIds,
          )

          Post(s"/v1/parts.create?part_id=$newPartId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            assertCreation(creation, newPartId, Some(categoryIds), Some(supplierIds))
          }
        }

        "create part with variants, location settings, location tax rates, their relations and return 201" in new PartsCreateFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val supplier = Factory.supplier(merchant).create
          val taxRate = Factory.taxRate(merchant).create
          Factory.taxRateLocation(taxRate, rome).create

          val newPartId = UUID.randomUUID
          val categoryIds = Seq(category1.id, category2.id)
          val supplierIds = Seq(supplier.id)

          val locationOverrides = Map(rome.id -> Some(random[PartLocationUpdate]))

          val partVariantCreations: Seq[VariantPartCreation] = {
            val variantParts = random[VariantPartCreation](variantSelections.size)

            variantSelections.zip(variantParts).map {
              case (vs, varPrdCreation) =>
                varPrdCreation
                  .copy(id = UUID.randomUUID, optionIds = vs, upc = randomUpc, sku = randomWords + varPrdCreation.id)
            }
          }
          val creation = random[PartCreation].copy(
            categoryIds = categoryIds,
            variants = variants,
            variantProducts = partVariantCreations,
            locationOverrides = locationOverrides,
            supplierIds = supplierIds,
          )

          Post(s"/v1/parts.create?part_id=$newPartId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            assertCreation(creation, newPartId, Some(categoryIds), Some(supplierIds))
          }
        }
      }

      "if part doesn't belong to current user's merchant" should {

        "not create part, its relations and return 404" in new PartResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDefaultMenuCatalog =
            Factory.defaultMenuCatalog(competitor).create
          val competitorCategory1 = Factory.systemCategory(competitorDefaultMenuCatalog).create
          val competitorPart = Factory.simplePart(competitor, categories = Seq(competitorCategory1)).create

          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val categoryIds = Seq(category1.id)

          val partUpdate = random[PartCreation].copy(categoryIds = categoryIds)

          Post(s"/v1/parts.create?part_id=${competitorPart.id}", partUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            val updatedPart = partDao.findById(competitorPart.id).await.get
            updatedPart ==== competitorPart

            productCategoryDao.findByProductId(competitorPart.id).await.map(_.categoryId) ==== Seq(
              competitorCategory1.id,
            )
          }
        }
      }

      "if a variant does not belong to the part" should {

        "reject the request with a 404" in new PartResourceFSpecContext {
          val newPartId = UUID.randomUUID

          val part = Factory.templatePart(merchant).create
          val invalidVariantOptionType = Factory.variantOptionType(part).create

          val invalidVariantType =
            buildVariantTypeWithOptions("color", Seq("yellow", "blue")).copy(id = invalidVariantOptionType.id)

          val invalidVariants = Seq(invalidVariantType, variantType2)

          val invalidVariantSelections = invalidVariants.map(_.options.map(_.id))

          val partVariantCreations: Seq[VariantPartCreation] = {
            val variantParts = random[VariantPartCreation](variantSelections.size)

            invalidVariantSelections.zip(variantParts).map {
              case (vs, varPrdCreation) =>
                varPrdCreation
                  .copy(id = UUID.randomUUID, optionIds = vs, upc = randomUpc, sku = randomWords + varPrdCreation.id)
            }
          }

          val creation =
            random[PartCreation].copy(variantProducts = partVariantCreations, variants = invalidVariants)

          Post(s"/v1/parts.create?part_id=$newPartId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            partDao.findById(newPartId).await ==== None
          }
        }
      }

      "if part variant does not belong to the merchant" should {
        "reject the request with a 404" in new PartResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorPart = Factory.simplePart(competitor).create

          val newPartId = UUID.randomUUID

          val partVariantCreations: Seq[VariantPartCreation] = {
            val variantParts = random[VariantPartCreation](variantSelections.size)

            variantSelections.zip(variantParts).map {
              case (vs, varPrdCreation) =>
                varPrdCreation
                  .copy(id = competitorPart.id, optionIds = vs, upc = randomUpc, sku = randomWords + varPrdCreation.id)
            }
          }
          val creation = random[PartCreation].copy(variantProducts = partVariantCreations, variants = variants)

          Post(s"/v1/parts.create?part_id=$newPartId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            partDao.findById(newPartId).await ==== None
          }
        }
      }

      "if a location id does not belong to the merchant" should {
        "reject the request with a 404" in new PartResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorPart = Factory.simplePart(competitor).create

          val locationOverrides = Map(rome.id -> Some(random[PartLocationUpdate]))

          val newPartId = UUID.randomUUID

          val partVariantCreations: Seq[VariantPartCreation] = {
            val variantParts = random[VariantPartCreation](variantSelections.size)

            variantSelections.zip(variantParts).map {
              case (vs, varPrdCreation) =>
                varPrdCreation
                  .copy(id = competitorPart.id, optionIds = vs, upc = randomUpc, sku = randomWords + varPrdCreation.id)
            }
          }

          val creation = random[PartCreation].copy(
            variantProducts = partVariantCreations,
            variants = variants,
            locationOverrides = locationOverrides,
          )

          Post(s"/v1/parts.create?part_id=$newPartId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            partDao.findById(newPartId).await ==== None
          }
        }
      }

      "if the part has a duplicate name" should {
        "reject the request with a 401" in new PartResourceFSpecContext {
          val firstId = UUID.randomUUID
          val firstCreation = random[PartCreation].copy(name = "Pomodoro", upc = None)
          Post(s"/v1/parts.create?part_id=$firstId", firstCreation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            assertCreation(firstCreation, firstId, Some(Seq.empty), Some(Seq.empty))
          }

          val secondId = UUID.randomUUID
          val secondCreation = random[PartCreation].copy(name = "Pomodoro", upc = None)
          Post(s"/v1/parts.create?part_id=$secondId", secondCreation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCodesAtLeastOnce("AlreadyTakenName")
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new PartResourceFSpecContext {
        val newPartId = UUID.randomUUID
        val creation = random[PartCreation]
        Post(s"/v1/parts.create?part_id=$newPartId", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
