package io.paytouch.core.resources.products

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class PartsUpdateFSpec extends PartsFSpec {

  abstract class PartsUpdateFSpecContext extends PartResourceFSpecContext

  "POST /v1/parts.update?part_id=<part-id>" in {

    "if request has valid token" in {

      "if part and its relations belong to same merchant" should {

        "update part, its relations and return 200" in new PartResourceFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val part = Factory.simplePart(merchant, categories = Seq(category1)).create
          val supplier = Factory.supplier(merchant).create

          val categoryIds = Seq(category1.id)
          val supplierIds = Seq(supplier.id)

          val update = random[PartUpdate].copy(categoryIds = Some(categoryIds), supplierIds = Some(supplierIds))

          Post(s"/v1/parts.update?part_id=${part.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, part.id, Some(categoryIds), Some(supplierIds))
          }
        }

        "update part and update variants, location settings, their relations and return 200" in new PartResourceFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val categoryIds = Seq(category1.id, category2.id)
          val supplier = Factory.supplier(merchant).create
          val supplierIds = Seq(supplier.id)

          val part = Factory.templatePart(merchant, categories = Seq(category1), locations = Seq(rome)).create

          val locationOverrides = Map(rome.id -> Some(random[PartLocationUpdate]))

          val partVariantUpdates = {
            val variantParts = random[VariantPartUpdate](variantSelections.size)
            variantSelections
              .zip(variantParts)
              .map {
                case (vs, varPrd) =>
                  val variant = Factory.variantPart(merchant, part).create
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

          val update = random[PartUpdate].copy(
            categoryIds = Some(categoryIds),
            description = Some("description"),
            variants = Some(variants),
            variantProducts = Some(partVariantUpdates),
            locationOverrides = locationOverrides,
            supplierIds = Some(supplierIds),
          )

          Post(s"/v1/parts.update?part_id=${part.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, part.id, Some(categoryIds), Some(supplierIds))
          }
        }

        "update part and update existing variants and create new ones" in new PartResourceFSpecContext {
          val part = Factory.templatePart(merchant, locations = Seq(rome)).create

          val partVariantUpdates = {
            val variantParts = random[VariantPartUpdate](variantSelections.size)
            variantSelections
              .zip(variantParts)
              .map {
                case (vs, varPrd) =>
                  val variant = Factory.variantPart(merchant, part, locations = Seq(rome)).create
                  varPrd.copy(id = variant.id, optionIds = vs, upc = randomUpc, sku = randomWords)
              }
              .toIndexedSeq
          }
          val partVariantCreation = random[VariantPartUpdate].copy(
            id = UUID.randomUUID,
            optionIds = variantSelections.head,
            upc = randomUpc,
            sku = randomWords + 15,
          )
          val partLocationUpdateWithOneCreation = partVariantUpdates :+ partVariantCreation
          val update = random[PartUpdate]
            .copy(variants = Some(variants), variantProducts = Some(partLocationUpdateWithOneCreation))

          Post(s"/v1/parts.update?part_id=${part.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, part.id)
          }
        }

        "update part completely changing variant types, options and variants" in new PartResourceFSpecContext {
          val part = Factory.templatePart(merchant, locations = Seq(rome)).create

          val variantType = Factory.variantOptionType(part).create
          val variantTypeOption1 = Factory.variantOption(part, variantType, "M").create
          val variantTypeOption2 = Factory.variantOption(part, variantType, "L").create

          val variantPart1 = Factory.variantPart(merchant, part, locations = Seq(rome)).create
          val partVariantOption1 = Factory.productVariantOption(variantPart1, variantTypeOption1).create
          val variantPart2 = Factory.variantPart(merchant, part, locations = Seq(rome)).create
          val partVariantOption2 = Factory.productVariantOption(variantPart2, variantTypeOption2).create

          val partVariantUpdates = {
            val variantParts = random[VariantPartUpdate](variantSelections.size)
            variantSelections
              .zip(variantParts)
              .map {
                case (vs, varProd) =>
                  varProd.copy(optionIds = vs, upc = randomUpc, sku = randomWords)
              }
              .toIndexedSeq
          }
          val update =
            random[PartUpdate].copy(variants = Some(variants), variantProducts = Some(partVariantUpdates))

          Post(s"/v1/parts.update?part_id=${part.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, part.id)

            variantOptionTypeDao.findById(variantType.id).await must beNone
            variantOptionDao.findByIds(Seq(variantTypeOption1.id, variantTypeOption2.id)).await must beEmpty
            productVariantOptionDao
              .findByIds(Seq(partVariantOption1.id, partVariantOption2.id))
              .await must beEmpty
            val deletedParts = partDao.findDeletedByIds(Seq(variantPart1.id, variantPart2.id)).await
            deletedParts.map(_.id) should containTheSameElementsAs(Seq(variantPart1.id, variantPart2.id))
          }
        }

        "update part and update variants, location settings, location tax rates, their relations and return 200" in new PartResourceFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val categoryIds = Seq(category1.id, category2.id)
          val supplier = Factory.supplier(merchant).create
          val supplierIds = Seq(supplier.id)
          val taxRate = Factory.taxRate(merchant).create
          Factory.taxRateLocation(taxRate, rome).create

          val part = Factory.templatePart(merchant, categories = Seq(category1), locations = Seq(rome)).create

          val locationOverrides = Map(rome.id -> Some(random[PartLocationUpdate]))

          val partVariantUpdates = {
            val variantParts = random[VariantPartUpdate](variantSelections.size)
            variantSelections
              .zip(variantParts)
              .map {
                case (vs, variantProd) =>
                  val variant = Factory.variantPart(merchant, part, locations = Seq(rome)).create
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
          val update = random[PartUpdate].copy(
            categoryIds = Some(categoryIds),
            description = Some("description"),
            variants = Some(variants),
            variantProducts = Some(partVariantUpdates),
            locationOverrides = locationOverrides,
            supplierIds = Some(supplierIds),
          )

          Post(s"/v1/parts.update?part_id=${part.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, part.id, Some(categoryIds), Some(supplierIds))
          }
        }

        "update variant part without changes and return 200" in new PartResourceFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val supplier = Factory.supplier(merchant).create

          val partId = UUID.randomUUID
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

          Post(s"/v1/parts.create?part_id=${partId}", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
          }

          // Update, changing nothing
          val update = creation

          Post(s"/v1/parts.update?part_id=${partId}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
          }
        }

        "update a simple part with new variants and return 400" in new PartResourceFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val categoryIds = Seq(category1.id, category2.id)
          val supplier = Factory.supplier(merchant).create
          val supplierIds = Seq(supplier.id)

          val part = Factory.simplePart(merchant, categories = Seq(category1), locations = Seq(rome)).create

          val locationOverrides = Map(rome.id -> Some(random[PartLocationUpdate]))

          val partVariantCreations = {
            val variantParts = random[VariantPartUpdate](variantSelections.size)
            variantSelections.zip(variantParts).map {
              case (vs, variantProd) =>
                variantProd
                  .copy(optionIds = vs, upc = randomUpc, sku = randomWords, locationOverrides = locationOverrides)
            }
          }
          val update = random[PartUpdate].copy(
            categoryIds = Some(categoryIds),
            description = Some("description"),
            variants = Some(variants),
            variantProducts = Some(partVariantCreations),
            locationOverrides = locationOverrides,
            supplierIds = Some(supplierIds),
          )

          Post(s"/v1/parts.update?part_id=${part.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
          }
        }

        "update part with no location settings" should {
          "leave the part location data intact and return 200" in new PartResourceFSpecContext {
            val part = Factory.templatePart(merchant).create
            val productLocation = Factory.productLocation(part, rome).create
            val update = random[PartUpdate]

            Post(s"/v1/parts.update?part_id=${part.id}", update)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              assertUpdate(update, part.id)

              productLocationDao.findById(productLocation.id).await.get ==== productLocation
            }
          }
        }

        "update part with a null location overrides" should {
          "remove the part location data and return 200" in new PartResourceFSpecContext {
            val part = Factory.templatePart(merchant).create
            val productLocation = Factory.productLocation(part, rome).create
            val locationOverrides = Map(rome.id -> None)
            val update =
              random[PartUpdate].copy(description = Some("description"), locationOverrides = locationOverrides)

            Post(s"/v1/parts.update?part_id=${part.id}", update)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              assertUpdate(update, part.id)

              productLocationDao.findById(productLocation.id).await ==== None
            }
          }
        }
      }

      "if part doesn't belong to current user's merchant" should {

        "not update part, its relations and return 404" in new PartResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDefaultMenuCatalog =
            Factory.defaultMenuCatalog(competitor).create
          val competitorCategory1 = Factory.systemCategory(competitorDefaultMenuCatalog).create
          val competitorPart = Factory.simplePart(competitor, categories = Seq(competitorCategory1)).create

          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val categoryIds = Seq(category1.id)

          val update = random[PartUpdate].copy(categoryIds = Some(categoryIds))

          Post(s"/v1/parts.update?part_id=${competitorPart.id}", update)
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

      "if category doesn't belong to current user's merchant" should {

        "update part, update own categories, skip spurious categories and return 200" in new PartResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDefaultMenuCatalog =
            Factory.defaultMenuCatalog(competitor).create
          val competitorCategory1 = Factory.systemCategory(competitorDefaultMenuCatalog).create

          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val part = Factory.simplePart(merchant, categories = Seq(category1)).create

          val update = random[PartUpdate].copy(categoryIds = Some(Seq(category1.id, competitorCategory1.id)))

          Post(s"/v1/parts.update?part_id=${part.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, part.id, categories = Some(Seq(category1.id)))
          }
        }
      }

      "if part variant does not belong to the merchant" should {
        "reject the request with a 404" in new PartResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorPart = Factory.simplePart(competitor).create

          val newPartId = UUID.randomUUID

          val partVariantCreations = {
            val variantParts = random[VariantPartUpdate](variantSelections.size)
            variantSelections.zip(variantParts).map {
              case (vs, variantProd) =>
                variantProd.copy(id = competitorPart.id, optionIds = vs, upc = randomUpc, sku = randomWords)
            }
          }
          val update =
            random[PartUpdate].copy(variantProducts = Some(partVariantCreations), variants = Some(variants))

          Post(s"/v1/parts.update?part_id=$newPartId", update)
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

          val partVariantCreations = {
            val variantParts = random[VariantPartUpdate](variantSelections.size)
            variantSelections.zip(variantParts).map {
              case (vs, variantProd) =>
                variantProd.copy(id = competitorPart.id, optionIds = vs, upc = randomUpc, sku = randomWords)
            }
          }
          val update = random[PartUpdate].copy(
            variantProducts = Some(partVariantCreations),
            variants = Some(variants),
            locationOverrides = locationOverrides,
          )

          Post(s"/v1/parts.update?part_id=$newPartId", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            partDao.findById(newPartId).await ==== None
          }
        }
      }
    }

    "if part has been deleted" should {

      "reject the request with a 404" in new PartResourceFSpecContext {
        val deletedPart = Factory.templatePart(merchant, deletedAt = Some(UtcTime.now)).create
        val update = random[PartUpdate]
        Post(s"/v1/parts.update?part_id=${deletedPart.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new PartResourceFSpecContext {
        val newPartId = UUID.randomUUID
        val update = random[PartUpdate]
        Post(s"/v1/parts.update?part_id=$newPartId", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
