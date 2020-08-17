package io.paytouch.core.resources.products

import java.time.ZonedDateTime

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities.{ Product => ProductEntity, _ }
import io.paytouch.core.utils.{ AppTokenFixtures, UtcTime, FixtureDaoFactory => Factory }

class ProductsListFSpec extends ProductsFSpec {

  abstract class ProductsListFSpecContext extends ProductResourceFSpecContext

  "GET /v1/products.list" in {

    "if request has valid token" in {

      "with no parameters" should {

        "return a list of products ordered by name" in new ProductsListFSpecContext {
          val templateProduct = Factory.templateProduct(merchant, name = Some("A product")).create
          val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
          val simpleProduct = Factory.simpleProduct(merchant, name = Some("B product")).create
          val comboProduct = Factory.comboProduct(merchant, name = Some("C product")).create

          val templatePart = Factory.templatePart(merchant, name = Some("A part")).create
          val variantPart = Factory.variantPart(merchant, templatePart, name = Some("AA part")).create
          val simplePart = Factory.simplePart(merchant, name = Some("B part")).create
          val comboPart = Factory.comboPart(merchant, name = Some("C part")).create

          Get("/v1/products.list").addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) ==== Seq(templateProduct.id, simpleProduct.id, comboProduct.id)

            assertResponse(products.find(_.id == templateProduct.id).get, templateProduct)
            assertResponse(products.find(_.id == simpleProduct.id).get, simpleProduct)
            assertResponse(products.find(_.id == comboProduct.id).get, comboProduct)
          }
        }

        "return a list of products ordered by name and id -- edge case" in new ProductsListFSpecContext {
          val sameName = "Same Name"
          val templateProduct = Factory.templateProduct(merchant, name = Some(sameName)).create
          val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
          val simpleProduct = Factory.simpleProduct(merchant, name = Some(sameName)).create

          Get("/v1/products.list?page=1&per_page=1").addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.size ==== 1
            products.map(_.id) ==== Seq(templateProduct.id, simpleProduct.id).sortBy(_.toString).take(1)
          }
        }
      }

      "with scope = product" in {

        "with type = main" should {
          "return a list of products ordered by name" in new ProductsListFSpecContext {
            val templateProduct = Factory.templateProduct(merchant, name = Some("A product")).create
            val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
            val simpleProduct = Factory.simpleProduct(merchant, name = Some("B product")).create
            val comboProduct = Factory.comboProduct(merchant, name = Some("C product")).create

            val templatePart = Factory.templatePart(merchant, name = Some("A part")).create
            val variantPart = Factory.variantPart(merchant, templatePart, name = Some("AA part")).create
            val simplePart = Factory.simplePart(merchant, name = Some("B part")).create
            val comboPart = Factory.comboPart(merchant, name = Some("C part")).create

            Get("/v1/products.list?scope=product&type=main").addHeader(authorizationHeader) ~> routes ~> check {
              val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
              products.map(_.id) ==== Seq(templateProduct.id, simpleProduct.id, comboProduct.id)

              assertResponse(products.find(_.id == templateProduct.id).get, templateProduct)
              assertResponse(products.find(_.id == simpleProduct.id).get, simpleProduct)
              assertResponse(products.find(_.id == comboProduct.id).get, comboProduct)
            }
          }
        }

        "with type = storable" should {
          "return a list of products ordered by name" in new ProductsListFSpecContext {
            val templateProduct = Factory.templateProduct(merchant, name = Some("A product")).create
            val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
            val simpleProduct = Factory.simpleProduct(merchant, name = Some("B product")).create
            val comboProduct = Factory.comboProduct(merchant, name = Some("C product")).create

            val templatePart = Factory.templatePart(merchant, name = Some("A part")).create
            val variantPart = Factory.variantPart(merchant, templatePart, name = Some("AA part")).create
            val simplePart = Factory.simplePart(merchant, name = Some("B part")).create
            val comboPart = Factory.comboPart(merchant, name = Some("C part")).create

            Get("/v1/products.list?scope=product&type=storable").addHeader(authorizationHeader) ~> routes ~> check {
              val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
              products.map(_.id) ==== Seq(variantProduct.id, simpleProduct.id, comboProduct.id)

              assertResponse(products.find(_.id == variantProduct.id).get, variantProduct)
              assertResponse(products.find(_.id == simpleProduct.id).get, simpleProduct)
              assertResponse(products.find(_.id == comboProduct.id).get, comboProduct)
            }
          }

          "with category id filter" should {
            "return a list of products ordered by name" in new ProductsListFSpecContext {
              val templateProduct = Factory.templateProduct(merchant, name = Some("A product")).create
              val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
              val simpleProduct = Factory.simpleProduct(merchant, name = Some("B product")).create
              val comboProduct = Factory.comboProduct(merchant, name = Some("C product")).create

              val templatePart = Factory.templatePart(merchant, name = Some("A part")).create
              val variantPart = Factory.variantPart(merchant, templatePart, name = Some("AA part")).create
              val simplePart = Factory.simplePart(merchant, name = Some("B part")).create
              val comboPart = Factory.comboPart(merchant, name = Some("C part")).create

              val category = Factory.systemCategory(defaultMenuCatalog).create
              Factory.productCategory(templateProduct, category).create
              Factory.productCategory(templateProduct, category).create

              val catalog = Factory.catalog(merchant).create
              val catalogCategory = Factory.catalogCategory(catalog).create
              Factory.productCategory(templateProduct, catalogCategory).create
              Factory.productCategory(templateProduct, catalogCategory).create

              Get(s"/v1/products.list?scope=product&type=storable&category_id=${category.id}")
                .addHeader(authorizationHeader) ~> routes ~> check {
                val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
                products.map(_.id) ==== Seq(variantProduct.id)

                assertResponse(products.find(_.id == variantProduct.id).get, variantProduct)
              }
            }
          }

          "with catalog ids filter" should {
            "return a list of products ordered by name" in new ProductsListFSpecContext {
              val templateProduct = Factory.templateProduct(merchant, name = Some("A product")).create
              val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
              val simpleProduct = Factory.simpleProduct(merchant, name = Some("B product")).create
              val comboProduct = Factory.comboProduct(merchant, name = Some("C product")).create

              val templatePart = Factory.templatePart(merchant, name = Some("A part")).create
              val variantPart = Factory.variantPart(merchant, templatePart, name = Some("AA part")).create
              val simplePart = Factory.simplePart(merchant, name = Some("B part")).create
              val comboPart = Factory.comboPart(merchant, name = Some("C part")).create

              val category = Factory.systemCategory(defaultMenuCatalog).create
              Factory.productCategory(templateProduct, category).create
              Factory.productCategory(templateProduct, category).create

              val catalog = Factory.catalog(merchant).create
              val catalogCategory = Factory.catalogCategory(catalog).create
              Factory.productCategory(templateProduct, catalogCategory).create
              Factory.productCategory(templateProduct, catalogCategory).create

              Get(s"/v1/products.list?scope=product&type=main&catalog_id[]=${catalog.id}")
                .addHeader(authorizationHeader) ~> routes ~> check {
                val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
                products.map(_.id) ==== Seq(templateProduct.id)

                assertResponse(products.find(_.id == templateProduct.id).get, templateProduct)
              }

              Get(s"/v1/products.list?scope=product&type=main&catalog_id[]=${defaultMenuCatalog.id}")
                .addHeader(authorizationHeader) ~> routes ~> check {
                val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
                products.map(_.id) ==== Seq(templateProduct.id)

                assertResponse(products.find(_.id == templateProduct.id).get, templateProduct)
              }
            }
          }

          "return a list of products ordered by name with correct variants and stock level" in new ProductsListFSpecContext {
            val templateProduct = Factory.templateProduct(merchant, name = Some("A product")).create
            val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
            val simpleProduct = Factory.simpleProduct(merchant, name = Some("B product")).create
            val comboProduct = Factory.comboProduct(merchant, name = Some("C product")).create

            val templatePart = Factory.templatePart(merchant, name = Some("A part")).create
            val variantPart = Factory.variantPart(merchant, templatePart, name = Some("AA part")).create
            val simplePart = Factory.simplePart(merchant, name = Some("B part")).create
            val comboPart = Factory.comboPart(merchant, name = Some("C part")).create

            val variantType = Factory.variantOptionType(templateProduct).create
            val variantTypeOption1 = Factory.variantOption(templateProduct, variantType, "M").create

            Factory.productVariantOption(variantProduct, variantTypeOption1).create

            val templateProductAtRome = Factory.productLocation(templateProduct, rome).create
            val variantProductAtRome = Factory.productLocation(variantProduct, rome).create
            val simpleProductAtRome = Factory.productLocation(simpleProduct, rome).create
            val comboProductAtRome = Factory.productLocation(comboProduct, rome).create

            val templateProductAtRomeStock = Factory.stock(templateProductAtRome, Some(2.0)).create
            val variantProductAtRomeStock = Factory.stock(variantProductAtRome, Some(1.0)).create
            val simpleProductAtRomeStock = Factory.stock(simpleProductAtRome, Some(4.0)).create
            val comboProductAtRomeStock = Factory.stock(comboProductAtRome, Some(4.0)).create

            Get("/v1/products.list?scope=product&type=storable&expand[]=variants,stock_level").addHeader(
              authorizationHeader,
            ) ~> routes ~> check {
              val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
              products.map(_.id) ==== Seq(variantProduct.id, simpleProduct.id, comboProduct.id)

              assertResponse(
                products.find(_.id == variantProduct.id).get,
                variantProduct,
                singleVariantOptionIds = Seq(variantTypeOption1.id),
                locationIds = Seq(rome.id),
                stockLevel = Some(1.0),
              )
              assertResponse(
                products.find(_.id == simpleProduct.id).get,
                simpleProduct,
                locationIds = Seq(rome.id),
                stockLevel = Some(4.0),
              )
              assertResponse(
                products.find(_.id == comboProduct.id).get,
                comboProduct,
                locationIds = Seq(rome.id),
                stockLevel = Some(4.0),
              )
            }
          }
        }

        "with type = simple" should {
          "return a list of products ordered by name" in new ProductsListFSpecContext {
            val templateProduct = Factory.templateProduct(merchant, name = Some("A product")).create
            val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
            val simpleProduct = Factory.simpleProduct(merchant, name = Some("B product")).create
            val comboProduct = Factory.comboProduct(merchant, name = Some("C product")).create

            val templatePart = Factory.templatePart(merchant, name = Some("A part")).create
            val variantPart = Factory.variantPart(merchant, templatePart, name = Some("AA part")).create
            val simplePart = Factory.simplePart(merchant, name = Some("B part")).create
            val comboPart = Factory.comboPart(merchant, name = Some("C part")).create

            Get("/v1/products.list?scope=product&type=simple").addHeader(authorizationHeader) ~> routes ~> check {
              val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
              products.map(_.id) ==== Seq(simpleProduct.id, comboProduct.id)

              assertResponse(products.find(_.id == simpleProduct.id).get, simpleProduct)
              assertResponse(products.find(_.id == comboProduct.id).get, comboProduct)
            }
          }
        }

        "with type = template" should {
          "return a list of products ordered by name" in new ProductsListFSpecContext {
            val templateProduct = Factory.templateProduct(merchant, name = Some("A product")).create
            val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
            val simpleProduct = Factory.simpleProduct(merchant, name = Some("B product")).create
            val comboProduct = Factory.comboProduct(merchant, name = Some("C product")).create

            val templatePart = Factory.templatePart(merchant, name = Some("A part")).create
            val variantPart = Factory.variantPart(merchant, templatePart, name = Some("AA part")).create
            val simplePart = Factory.simplePart(merchant, name = Some("B part")).create
            val comboPart = Factory.comboPart(merchant, name = Some("C part")).create

            Get("/v1/products.list?scope=product&type=template").addHeader(authorizationHeader) ~> routes ~> check {
              val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
              products.map(_.id) ==== Seq(templateProduct.id)

              assertResponse(products.find(_.id == templateProduct.id).get, templateProduct)
            }
          }
        }

        "with type = variant" should {
          "return a list of products ordered by name" in new ProductsListFSpecContext {
            val templateProduct = Factory.templateProduct(merchant, name = Some("A product")).create
            val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
            val simpleProduct = Factory.simpleProduct(merchant, name = Some("B product")).create
            val comboProduct = Factory.comboProduct(merchant, name = Some("C product")).create

            val templatePart = Factory.templatePart(merchant, name = Some("A part")).create
            val variantPart = Factory.variantPart(merchant, templatePart, name = Some("AA part")).create
            val simplePart = Factory.simplePart(merchant, name = Some("B part")).create
            val comboPart = Factory.comboPart(merchant, name = Some("C part")).create

            Get("/v1/products.list?scope=product&type=variant").addHeader(authorizationHeader) ~> routes ~> check {
              val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
              products.map(_.id) ==== Seq(variantProduct.id)

              assertResponse(products.find(_.id == variantProduct.id).get, variantProduct)
            }
          }
        }

        "with type[] = simple, variant" should {
          "return a list of products ordered by name" in new ProductsListFSpecContext {
            val templateProduct = Factory.templateProduct(merchant, name = Some("A product")).create
            val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
            val simpleProduct = Factory.simpleProduct(merchant, name = Some("B product")).create
            val comboProduct = Factory.comboProduct(merchant, name = Some("C product")).create

            val templatePart = Factory.templatePart(merchant, name = Some("A part")).create
            val variantPart = Factory.variantPart(merchant, templatePart, name = Some("AA part")).create
            val simplePart = Factory.simplePart(merchant, name = Some("B part")).create
            val comboPart = Factory.comboPart(merchant, name = Some("C part")).create

            Get("/v1/products.list?scope=product&type[]=simple,variant").addHeader(
              authorizationHeader,
            ) ~> routes ~> check {
              val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
              products.map(_.id) ==== Seq(variantProduct.id, simpleProduct.id, comboProduct.id)

              assertResponse(products.find(_.id == variantProduct.id).get, variantProduct)
              assertResponse(products.find(_.id == simpleProduct.id).get, simpleProduct)
              assertResponse(products.find(_.id == comboProduct.id).get, comboProduct)
            }
          }
        }
      }

      "with scope = part" in {

        "with type = main" should {
          "return a list of products ordered by name" in new ProductsListFSpecContext {
            val templateProduct = Factory.templateProduct(merchant, name = Some("A product")).create
            val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
            val simpleProduct = Factory.simpleProduct(merchant, name = Some("B product")).create
            val comboProduct = Factory.comboProduct(merchant, name = Some("C product")).create

            val templatePart = Factory.templatePart(merchant, name = Some("A part")).create
            val variantPart = Factory.variantPart(merchant, templatePart, name = Some("AA part")).create
            val simplePart = Factory.simplePart(merchant, name = Some("B part")).create
            val comboPart = Factory.comboPart(merchant, name = Some("C part")).create

            Get("/v1/products.list?scope=part&type=main").addHeader(authorizationHeader) ~> routes ~> check {
              val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
              products.map(_.id) ==== Seq(templatePart.id, simplePart.id, comboPart.id)

              assertResponse(products.find(_.id == templatePart.id).get, templatePart)
              assertResponse(products.find(_.id == simplePart.id).get, simplePart)
              assertResponse(products.find(_.id == comboPart.id).get, comboPart)
            }
          }
        }

        "with type = storable" should {
          "return a list of products ordered by name" in new ProductsListFSpecContext {
            val templateProduct = Factory.templateProduct(merchant, name = Some("A product")).create
            val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
            val simpleProduct = Factory.simpleProduct(merchant, name = Some("B product")).create
            val comboProduct = Factory.comboProduct(merchant, name = Some("C product")).create

            val templatePart = Factory.templatePart(merchant, name = Some("A part")).create
            val variantPart = Factory.variantPart(merchant, templatePart, name = Some("AA part")).create
            val simplePart = Factory.simplePart(merchant, name = Some("B part")).create
            val comboPart = Factory.comboPart(merchant, name = Some("C part")).create

            Get("/v1/products.list?scope=part&type=storable").addHeader(authorizationHeader) ~> routes ~> check {
              val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
              products.map(_.id) ==== Seq(variantPart.id, simplePart.id, comboPart.id)

              assertResponse(products.find(_.id == variantPart.id).get, variantPart)
              assertResponse(products.find(_.id == simplePart.id).get, simplePart)
              assertResponse(products.find(_.id == comboPart.id).get, comboPart)
            }
          }
        }

        "with type = simple" should {
          "return a list of products ordered by name" in new ProductsListFSpecContext {
            val templateProduct = Factory.templateProduct(merchant, name = Some("A product")).create
            val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
            val simpleProduct = Factory.simpleProduct(merchant, name = Some("B product")).create
            val comboProduct = Factory.comboProduct(merchant, name = Some("C product")).create

            val templatePart = Factory.templatePart(merchant, name = Some("A part")).create
            val variantPart = Factory.variantPart(merchant, templatePart, name = Some("AA part")).create
            val simplePart = Factory.simplePart(merchant, name = Some("B part")).create
            val comboPart = Factory.comboPart(merchant, name = Some("C part")).create

            Get("/v1/products.list?scope=part&type=simple").addHeader(authorizationHeader) ~> routes ~> check {
              val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
              products.map(_.id) ==== Seq(simplePart.id, comboPart.id)

              assertResponse(products.find(_.id == simplePart.id).get, simplePart)
              assertResponse(products.find(_.id == comboPart.id).get, comboPart)
            }
          }
        }

        "with type = template" should {
          "return a list of products ordered by name" in new ProductsListFSpecContext {
            val templateProduct = Factory.templateProduct(merchant, name = Some("A product")).create
            val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
            val simpleProduct = Factory.simpleProduct(merchant, name = Some("B product")).create
            val comboProduct = Factory.comboProduct(merchant, name = Some("C product")).create

            val templatePart = Factory.templatePart(merchant, name = Some("A part")).create
            val variantPart = Factory.variantPart(merchant, templatePart, name = Some("AA part")).create
            val simplePart = Factory.simplePart(merchant, name = Some("B part")).create
            val comboPart = Factory.comboPart(merchant, name = Some("C part")).create

            Get("/v1/products.list?scope=part&type=template").addHeader(authorizationHeader) ~> routes ~> check {
              val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
              products.map(_.id) ==== Seq(templatePart.id)

              assertResponse(products.find(_.id == templatePart.id).get, templatePart)
            }
          }
        }

        "with type = variant" should {
          "return a list of products ordered by name" in new ProductsListFSpecContext {
            val templateProduct = Factory.templateProduct(merchant, name = Some("A product")).create
            val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
            val simpleProduct = Factory.simpleProduct(merchant, name = Some("B product")).create
            val comboProduct = Factory.comboProduct(merchant, name = Some("C product")).create

            val templatePart = Factory.templatePart(merchant, name = Some("A part")).create
            val variantPart = Factory.variantPart(merchant, templatePart, name = Some("AA part")).create
            val simplePart = Factory.simplePart(merchant, name = Some("B part")).create
            val comboPart = Factory.comboPart(merchant, name = Some("C part")).create

            Get("/v1/products.list?scope=part&type=variant").addHeader(authorizationHeader) ~> routes ~> check {
              val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
              products.map(_.id) ==== Seq(variantPart.id)

              assertResponse(products.find(_.id == variantPart.id).get, variantPart)
            }
          }
        }

        "with type[] = simple, variant" should {
          "return a list of products ordered by name" in new ProductsListFSpecContext {
            val templateProduct = Factory.templateProduct(merchant, name = Some("A product")).create
            val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
            val simpleProduct = Factory.simpleProduct(merchant, name = Some("B product")).create
            val comboProduct = Factory.comboProduct(merchant, name = Some("C product")).create

            val templatePart = Factory.templatePart(merchant, name = Some("A part")).create
            val variantPart = Factory.variantPart(merchant, templatePart, name = Some("AA part")).create
            val simplePart = Factory.simplePart(merchant, name = Some("B part")).create
            val comboPart = Factory.comboPart(merchant, name = Some("C part")).create

            Get("/v1/products.list?scope=part&type[]=simple,variant").addHeader(
              authorizationHeader,
            ) ~> routes ~> check {
              val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
              products.map(_.id) ==== Seq(variantPart.id, simplePart.id, comboPart.id)

              assertResponse(products.find(_.id == variantPart.id).get, variantPart)
              assertResponse(products.find(_.id == simplePart.id).get, simplePart)
              assertResponse(products.find(_.id == comboPart.id).get, comboPart)
            }
          }
        }
      }

      "with expand[]=modifiers" should {

        "returns a list of products enriched with modifiers" in new ProductsListFSpecContext {
          val newYork = Factory.location(merchant).create
          val modifierSet1 = Factory.modifierSet(merchant, locations = Seq(newYork)).create
          val modifierSet2 = Factory.modifierSet(merchant, locations = Seq(rome)).create

          val templateProduct = Factory.templateProduct(merchant, name = Some("A")).create
          val simpleProduct = Factory.simpleProduct(merchant, name = Some("B")).create

          Factory.modifierSetProduct(modifierSet1, templateProduct).create
          Factory.modifierSetProduct(modifierSet1, simpleProduct).create
          Factory.modifierSetProduct(modifierSet2, simpleProduct).create

          Get("/v1/products.list?expand[]=modifiers").addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) ==== Seq(templateProduct.id, simpleProduct.id)

            assertResponse(
              products.find(_.id == templateProduct.id).get,
              templateProduct,
              modifierSets = Some(Seq(modifierSet1)),
            )
            assertResponse(
              products.find(_.id == simpleProduct.id).get,
              simpleProduct,
              modifierSets = Some(Seq(modifierSet1, modifierSet2)),
            )
          }
        }
      }

      "with expand[]=modifier_ids" should {

        "returns a list of products enriched with modifierSet ids" in new ProductsListFSpecContext {
          val newYork = Factory.location(merchant).create
          val modifierSet1 = Factory.modifierSet(merchant, locations = Seq(newYork)).create
          val modifierSet2 = Factory.modifierSet(merchant, locations = Seq(rome)).create

          val templateProduct = Factory.templateProduct(merchant, name = Some("A")).create
          val simpleProduct = Factory.simpleProduct(merchant, name = Some("B")).create

          Factory.modifierSetProduct(modifierSet1, templateProduct).create
          Factory.modifierSetProduct(modifierSet1, simpleProduct).create
          Factory.modifierSetProduct(modifierSet2, simpleProduct).create

          Get("/v1/products.list?expand[]=modifier_ids").addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) ==== Seq(templateProduct.id, simpleProduct.id)

            assertResponse(
              products.find(_.id == templateProduct.id).get,
              templateProduct,
              modifierSetIds = Some(Seq(modifierSet1.id)),
            )
            assertResponse(
              products.find(_.id == simpleProduct.id).get,
              simpleProduct,
              modifierSetIds = Some(Seq(modifierSet1.id, modifierSet2.id)),
            )
          }
        }
      }

      "with expand[]=modifier_positions" should {

        "returns a list of products enriched with modifier positions" in new ProductsListFSpecContext {
          val newYork = Factory.location(merchant).create
          val modifierSet1 = Factory.modifierSet(merchant, locations = Seq(newYork)).create
          val modifierSet2 = Factory.modifierSet(merchant, locations = Seq(rome)).create

          val templateProduct = Factory.templateProduct(merchant, name = Some("A")).create
          val simpleProduct = Factory.simpleProduct(merchant, name = Some("B")).create
          val emptyProduct = Factory.templateProduct(merchant, name = Some("C")).create

          Factory.modifierSetProduct(modifierSet1, templateProduct, position = None).create
          Factory.modifierSetProduct(modifierSet1, simpleProduct, position = Some(1)).create
          Factory.modifierSetProduct(modifierSet2, simpleProduct, position = Some(2)).create

          Get("/v1/products.list?expand[]=modifier_positions").addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) ==== Seq(templateProduct.id, simpleProduct.id, emptyProduct.id)

            assertResponse(
              products.find(_.id == templateProduct.id).get,
              templateProduct,
              modifierPositions = Some(
                Seq(
                  ModifierPosition(modifierSetId = modifierSet1.id, position = None),
                ),
              ),
            )
            assertResponse(
              products.find(_.id == simpleProduct.id).get,
              simpleProduct,
              modifierPositions = Some(
                Seq(
                  ModifierPosition(modifierSetId = modifierSet1.id, position = Some(1)),
                  ModifierPosition(modifierSetId = modifierSet2.id, position = Some(2)),
                ),
              ),
            )
            assertResponse(
              products.find(_.id == emptyProduct.id).get,
              emptyProduct,
              modifierPositions = Some(Seq.empty),
            )
          }
        }
      }

      "with expand[]=categories" should {

        "returns a list of products enriched with categories" in new ProductsListFSpecContext {
          val newYork = Factory.location(merchant).create
          val category1 = Factory.systemCategory(defaultMenuCatalog, locations = Seq(newYork)).create
          val category2 = Factory.systemCategory(defaultMenuCatalog, locations = Seq(rome)).create

          val catalog = Factory.catalog(merchant).create
          val catalogCategory = Factory.catalogCategory(catalog).create

          val templateProduct =
            Factory.templateProduct(merchant, name = Some("A"), categories = Seq(category1, catalogCategory)).create
          val simpleProduct =
            Factory.simpleProduct(merchant, name = Some("B"), categories = Seq(category2, catalogCategory)).create

          Get("/v1/products.list?expand[]=categories").addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) ==== Seq(templateProduct.id, simpleProduct.id)

            assertResponse(
              products.find(_.id == templateProduct.id).get,
              templateProduct,
              systemCategories = Some(Seq(category1)),
            )
            assertResponse(
              products.find(_.id == simpleProduct.id).get,
              simpleProduct,
              systemCategories = Some(Seq(category2)),
            )
          }
        }
      }

      "with expand[]=category_ids" should {

        "returns a list of products enriched with category ids" in new ProductsListFSpecContext {
          val newYork = Factory.location(merchant).create
          val category1 = Factory.systemCategory(defaultMenuCatalog, locations = Seq(newYork)).create
          val category2 = Factory.systemCategory(defaultMenuCatalog, locations = Seq(rome)).create

          val catalog = Factory.catalog(merchant).create
          val catalogCategory = Factory.catalogCategory(catalog).create

          val templateProduct =
            Factory.templateProduct(merchant, name = Some("A"), categories = Seq(category1, catalogCategory)).create
          val simpleProduct =
            Factory.simpleProduct(merchant, name = Some("B"), categories = Seq(category2, catalogCategory)).create

          Get("/v1/products.list?expand[]=category_ids").addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) ==== Seq(templateProduct.id, simpleProduct.id)

            assertResponse(
              products.find(_.id == templateProduct.id).get,
              templateProduct,
              systemCategoryIds = Some(Seq(category1.id)),
            )
            assertResponse(
              products.find(_.id == simpleProduct.id).get,
              simpleProduct,
              systemCategoryIds = Some(Seq(category2.id)),
            )
          }
        }
      }

      "with expand[]=category_positions" should {

        "returns a list of products enriched with category positions" in new ProductsListFSpecContext {
          val newYork = Factory.location(merchant).create
          val category1 = Factory.systemCategory(defaultMenuCatalog, locations = Seq(newYork)).create
          val category2 = Factory.systemCategory(defaultMenuCatalog, locations = Seq(rome)).create

          val templateProduct = Factory.templateProduct(merchant, name = Some("A")).create
          val category1Product = Factory.productCategory(templateProduct, category1, Some(2)).create
          val simpleProduct = Factory.simpleProduct(merchant, name = Some("B")).create
          val category2Product = Factory.productCategory(simpleProduct, category2, Some(3)).create

          Get("/v1/products.list?expand[]=category_positions").addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) ==== Seq(templateProduct.id, simpleProduct.id)

            assertResponse(
              products.find(_.id == templateProduct.id).get,
              templateProduct,
              productSystemCategories = Seq(category1Product),
            )
            assertResponse(
              products.find(_.id == simpleProduct.id).get,
              simpleProduct,
              productSystemCategories = Seq(category2Product),
            )
          }
        }
      }

      "with expand[]=catalog_categories" should {

        "returns a list of products enriched with catalog categories" in new ProductsListFSpecContext {
          val newYork = Factory.location(merchant).create
          val category1 = Factory.systemCategory(defaultMenuCatalog, locations = Seq(newYork)).create
          val category2 = Factory.systemCategory(defaultMenuCatalog, locations = Seq(rome)).create

          val catalog = Factory.catalog(merchant).create
          val catalogCategory = Factory.catalogCategory(catalog).create

          val templateProduct =
            Factory.templateProduct(merchant, name = Some("A"), categories = Seq(category1, catalogCategory)).create
          val simpleProduct =
            Factory.simpleProduct(merchant, name = Some("B"), categories = Seq(category2, catalogCategory)).create

          Get("/v1/products.list?expand[]=catalog_categories").addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) ==== Seq(templateProduct.id, simpleProduct.id)

            assertResponse(
              products.find(_.id == templateProduct.id).get,
              templateProduct,
              catalogCategories = Some(Seq(catalogCategory)),
            )
            assertResponse(
              products.find(_.id == simpleProduct.id).get,
              simpleProduct,
              catalogCategories = Some(Seq(catalogCategory)),
            )
          }
        }
      }

      "with expand[]=catalog_category_options" should {

        "returns a list of products enriched with catalog category options" in new ProductsListFSpecContext {
          val newYork = Factory.location(merchant).create
          val category1 = Factory.systemCategory(defaultMenuCatalog, locations = Seq(newYork)).create
          val category2 = Factory.systemCategory(defaultMenuCatalog, locations = Seq(rome)).create

          val catalog = Factory.catalog(merchant).create
          val catalogCategory = Factory.catalogCategory(catalog).create

          val templateProduct = Factory.templateProduct(merchant, name = Some("A")).create
          val category1Product = Factory.productCategory(templateProduct, category1, Some(2)).create
          val catalogCategory1Product = Factory.productCategory(templateProduct, catalogCategory, Some(20)).create
          val catalogCategory1ProductOption = Factory.productCategoryOption(catalogCategory1Product).create
          val catalogCategory1ProductOptionExpectation = CatalogCategoryOption(
            categoryId = catalogCategory1Product.categoryId,
            deliveryEnabled = catalogCategory1ProductOption.deliveryEnabled,
            takeAwayEnabled = catalogCategory1ProductOption.takeAwayEnabled,
          )

          val simpleProduct = Factory.simpleProduct(merchant, name = Some("B")).create
          val category2Product = Factory.productCategory(simpleProduct, category2, Some(3)).create
          val catalogCategory2Product = Factory.productCategory(simpleProduct, catalogCategory, Some(30)).create
          val catalogCategory2ProductOption = Factory.productCategoryOption(catalogCategory2Product).create
          val catalogCategory2ProductOptionExpectation = CatalogCategoryOption(
            categoryId = catalogCategory2Product.categoryId,
            deliveryEnabled = catalogCategory2ProductOption.deliveryEnabled,
            takeAwayEnabled = catalogCategory2ProductOption.takeAwayEnabled,
          )

          Get("/v1/products.list?expand[]=catalog_category_options").addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) ==== Seq(templateProduct.id, simpleProduct.id)

            assertResponse(
              products.find(_.id == templateProduct.id).get,
              templateProduct,
              productCategoryOptions = Seq(catalogCategory1ProductOptionExpectation),
            )
            assertResponse(
              products.find(_.id == simpleProduct.id).get,
              simpleProduct,
              productCategoryOptions = Seq(catalogCategory2ProductOptionExpectation),
            )
          }
        }
      }

      "with expand[]=catalog_category_positions" should {

        "returns a list of products enriched with catalog category positions" in new ProductsListFSpecContext {
          val newYork = Factory.location(merchant).create
          val category1 = Factory.systemCategory(defaultMenuCatalog, locations = Seq(newYork)).create
          val category2 = Factory.systemCategory(defaultMenuCatalog, locations = Seq(rome)).create

          val catalog = Factory.catalog(merchant).create
          val catalogCategory = Factory.catalogCategory(catalog).create

          val templateProduct = Factory.templateProduct(merchant, name = Some("A")).create
          val category1Product = Factory.productCategory(templateProduct, category1, Some(2)).create
          val catalogCategory1Product = Factory.productCategory(templateProduct, catalogCategory, Some(20)).create
          val simpleProduct = Factory.simpleProduct(merchant, name = Some("B")).create
          val category2Product = Factory.productCategory(simpleProduct, category2, Some(3)).create
          val catalogCategory2Product = Factory.productCategory(simpleProduct, catalogCategory, Some(30)).create

          Get("/v1/products.list?expand[]=catalog_category_positions").addHeader(
            authorizationHeader,
          ) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) ==== Seq(templateProduct.id, simpleProduct.id)

            assertResponse(
              products.find(_.id == templateProduct.id).get,
              templateProduct,
              productCatalogCategories = Seq(category1Product, catalogCategory1Product),
            )
            assertResponse(
              products.find(_.id == simpleProduct.id).get,
              simpleProduct,
              productCatalogCategories = Seq(category2Product, catalogCategory2Product),
            )
          }
        }
      }

      "with location_id, category_id, q parameters" should {

        "return a list of products matching the filters" in new ProductsListFSpecContext {
          val tShirts = Factory.systemCategory(defaultMenuCatalog).create
          val jeans = Factory.systemCategory(defaultMenuCatalog).create

          val tShirtLondon1 = Factory
            .simpleProduct(merchant, name = Some("Cool blue shirt"), locations = Seq(london), categories = Seq(tShirts))
            .create
          val tShirtLondon2 = Factory
            .simpleProduct(merchant, name = Some("Life goes on"), locations = Seq(london), categories = Seq(tShirts))
            .create
          val tShirtRome = Factory.simpleProduct(merchant, locations = Seq(rome), categories = Seq(tShirts)).create
          val jeansLondon = Factory.simpleProduct(merchant, locations = Seq(london), categories = Seq(jeans)).create
          val jeansRome = Factory.simpleProduct(merchant, locations = Seq(rome), categories = Seq(jeans)).create

          Get(s"/v1/products.list?location_id=${london.id}&category_id=${tShirts.id}&q=cool%20BLUE")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) ==== Seq(tShirtLondon1.id)

            assertResponse(products.head, tShirtLondon1, locationIds = Seq(london.id))
          }
        }
      }

      "with category_id[]" should {

        "return a list of products matching the filters" in new ProductsListFSpecContext {
          val tShirts = Factory.systemCategory(defaultMenuCatalog).create
          val jeans = Factory.systemCategory(defaultMenuCatalog).create
          val hat = Factory.systemCategory(defaultMenuCatalog).create

          val tShirtLondon1 = Factory
            .simpleProduct(merchant, name = Some("Cool blue shirt"), locations = Seq(london), categories = Seq(tShirts))
            .create
          val tShirtLondon2 = Factory
            .simpleProduct(merchant, name = Some("Life goes on"), locations = Seq(london), categories = Seq(tShirts))
            .create
          val tShirtRome = Factory.simpleProduct(merchant, locations = Seq(rome), categories = Seq(tShirts)).create
          val jeansLondon = Factory.simpleProduct(merchant, locations = Seq(london), categories = Seq(jeans)).create
          val hatRome = Factory.simpleProduct(merchant, locations = Seq(rome), categories = Seq(hat)).create

          val categoryIds = Seq(jeans.id, hat.id)

          Get(s"/v1/products.list?category_id[]=${categoryIds.mkString(",")}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) should containTheSameElementsAs(Seq(jeansLondon.id, hatRome.id))

            assertResponse(products.find(_.id == jeansLondon.id).get, jeansLondon, locationIds = Seq(london.id))
            assertResponse(products.find(_.id == hatRome.id).get, hatRome, locationIds = Seq(rome.id))
          }
        }
      }

      "with catalog_category_id[]" should {

        "return a list of products matching the filters" in new ProductsListFSpecContext {
          val tShirts = Factory.systemCategory(defaultMenuCatalog).create
          val jeans = Factory.systemCategory(defaultMenuCatalog).create
          val hat = Factory.systemCategory(defaultMenuCatalog).create

          val catalog = Factory.catalog(merchant).create
          val catalogCategory = Factory.catalogCategory(catalog).create

          val tShirtLondon1 = Factory
            .simpleProduct(merchant, name = Some("Cool blue shirt"), locations = Seq(london), categories = Seq(tShirts))
            .create
          val tShirtLondon2 = Factory
            .simpleProduct(merchant, name = Some("Life goes on"), locations = Seq(london), categories = Seq(tShirts))
            .create
          val tShirtRome = Factory.simpleProduct(merchant, locations = Seq(rome), categories = Seq(tShirts)).create
          val jeansLondon =
            Factory.simpleProduct(merchant, locations = Seq(london), categories = Seq(jeans, catalogCategory)).create
          val hatRome =
            Factory.simpleProduct(merchant, locations = Seq(rome), categories = Seq(catalogCategory)).create

          Get(s"/v1/products.list?catalog_category_id[]=${catalogCategory.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) should containTheSameElementsAs(Seq(jeansLondon.id, hatRome.id))

            assertResponse(products.find(_.id == jeansLondon.id).get, jeansLondon, locationIds = Seq(london.id))
            assertResponse(products.find(_.id == hatRome.id).get, hatRome, locationIds = Seq(rome.id))
          }
        }
      }

      "with q parameters" should {

        "return a list of products matching the filters" in new ProductsListFSpecContext {
          val tShirt1 = Factory.simpleProduct(merchant, name = Some("name77523name")).create
          val tShirt2 = Factory.simpleProduct(merchant, upc = Some("0177523789")).create
          val tShirt3 = Factory.simpleProduct(merchant, upc = Some("skusku0127752389skusku")).create
          val tShirt4 = Factory.simpleProduct(merchant).create
          val jeans1 = Factory.simpleProduct(merchant).create
          val jeans2 = Factory.simpleProduct(merchant).create

          Get(s"/v1/products.list?q=77523").addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) should containTheSameElementsAs(Seq(tShirt1.id, tShirt2.id, tShirt3.id))

            assertResponse(products.find(_.id == tShirt1.id).get, tShirt1)
            assertResponse(products.find(_.id == tShirt2.id).get, tShirt2)
            assertResponse(products.find(_.id == tShirt3.id).get, tShirt3)
          }
        }
      }

      "with modifier_set_id and location id" should {

        "return a list of products matching the filters" in new ProductsListFSpecContext {
          val jeans = Factory.simpleProduct(merchant, name = Some("1 jeans")).create
          val tShirt = Factory.simpleProduct(merchant, name = Some("2 tShirt")).create
          val bananas = Factory.simpleProduct(merchant, name = Some("3 bananas")).create

          val modifierSetLondon = Factory.modifierSet(merchant, locations = Seq(london)).create
          val modifierSetRome = Factory.modifierSet(merchant, locations = Seq(rome)).create

          Factory.modifierSetProduct(modifierSetLondon, jeans).create
          Factory.modifierSetProduct(modifierSetLondon, bananas).create
          Factory.modifierSetProduct(modifierSetRome, tShirt).create

          Factory.productLocation(jeans, london).create
          Factory.productLocation(tShirt, london).create

          Get(s"/v1/products.list?modifier_set_id=${modifierSetLondon.id}&location_id=${london.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) ==== Seq(jeans.id)

            assertResponse(products.find(_.id == jeans.id).get, jeans, locationIds = Seq(london.id))
          }
        }
      }

      "with modifier_set_id" should {

        "return a list of products matching the filters" in new ProductsListFSpecContext {
          val jeans = Factory.simpleProduct(merchant, name = Some("1 jeans")).create
          val tShirt = Factory.simpleProduct(merchant, name = Some("2 tShirt")).create
          val bananas = Factory.simpleProduct(merchant, name = Some("3 bananas")).create

          val modifierSet = Factory.modifierSet(merchant).create
          Factory.modifierSetProduct(modifierSet, jeans).create
          Factory.modifierSetProduct(modifierSet, tShirt).create

          Get(s"/v1/products.list?modifier_set_id=${modifierSet.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) ==== Seq(jeans.id, tShirt.id)

            assertResponse(products.find(_.id == jeans.id).get, jeans)
            assertResponse(products.find(_.id == tShirt.id).get, tShirt)
          }
        }
      }

      "with supplier_id" should {

        "return a list of products matching the filters" in new ProductsListFSpecContext {
          val jeans = Factory.simpleProduct(merchant, name = Some("1 jeans")).create
          val tShirt = Factory.simpleProduct(merchant, name = Some("2 tShirt")).create
          val bananas = Factory.simpleProduct(merchant, name = Some("3 bananas")).create
          val template = Factory.templateProduct(merchant, name = Some("4 template")).create
          val productVariant1 = Factory.variantProduct(merchant, template, name = Some("4 variant1")).create
          val productVariant2 = Factory.variantProduct(merchant, template, name = Some("4 variant2")).create

          val supplier1 = Factory.supplier(merchant).create
          Factory.supplierProduct(supplier1, jeans).create
          Factory.supplierProduct(supplier1, tShirt).create
          Factory.supplierProduct(supplier1, template).create

          val supplier2 = Factory.supplier(merchant).create
          Factory.supplierProduct(supplier2, bananas).create

          Get(s"/v1/products.list?supplier_id=${supplier1.id}").addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) ==== Seq(jeans.id, tShirt.id, template.id)

            assertResponse(products.find(_.id == jeans.id).get, jeans)
            assertResponse(products.find(_.id == tShirt.id).get, tShirt)
            assertResponse(products.find(_.id == template.id).get, template)
          }
        }

        "with type=storable" should {
          "return a list of products matching the filters" in new ProductsListFSpecContext {
            val jeans = Factory.simpleProduct(merchant, name = Some("1 jeans")).create
            val tShirt = Factory.simpleProduct(merchant, name = Some("2 tShirt")).create
            val bananas = Factory.simpleProduct(merchant, name = Some("3 bananas")).create
            val template = Factory.templateProduct(merchant, name = Some("4 template")).create
            val productVariant1 = Factory.variantProduct(merchant, template, name = Some("4 variant1")).create
            val productVariant2 = Factory.variantProduct(merchant, template, name = Some("4 variant2")).create

            val supplier1 = Factory.supplier(merchant).create
            Factory.supplierProduct(supplier1, jeans).create
            Factory.supplierProduct(supplier1, tShirt).create
            Factory.supplierProduct(supplier1, template).create
            val supplier2 = Factory.supplier(merchant).create
            Factory.supplierProduct(supplier2, bananas).create

            Get(s"/v1/products.list?type=storable&supplier_id=${supplier1.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
              products.map(_.id) ==== Seq(jeans.id, tShirt.id, productVariant1.id, productVariant2.id)

              assertResponse(products.find(_.id == jeans.id).get, jeans)
              assertResponse(products.find(_.id == tShirt.id).get, tShirt)
              assertResponse(products.find(_.id == productVariant1.id).get, productVariant1)
              assertResponse(products.find(_.id == productVariant2.id).get, productVariant2)
            }
          }
        }
      }

      "with expand=stock_level,price_ranges,cost_ranges and location_id" should {
        "return a list of products, matching showing stock levels for the given location" in new ProductsListFSpecContext {
          val tShirtsCategory = Factory.systemCategory(defaultMenuCatalog).create
          val modifierSet = Factory.modifierSet(merchant).create

          val simpleProduct = Factory.simpleProduct(merchant).create
          val product = Factory.templateProduct(merchant, categories = Seq(tShirtsCategory)).create
          val productVariant1 = Factory.variantProduct(merchant, product).create
          val productVariant2 = Factory.variantProduct(merchant, product).create
          val productVariantDeleted = Factory.variantProduct(merchant, product, deletedAt = Some(UtcTime.now)).create

          Factory.modifierSetProduct(modifierSet, product).create
          val taxRate = Factory.taxRate(merchant, name = None, products = Seq(product)).create
          Factory.taxRateLocation(taxRate, london).create

          val simpleProductAtLondon =
            Factory.productLocation(simpleProduct, london, priceAmount = Some(3), costAmount = Some(1)).create
          val simpleProductAtRome =
            Factory.productLocation(simpleProduct, rome, priceAmount = Some(6), costAmount = Some(3)).create

          val simpleProductAtLondonStock = Factory.stock(simpleProductAtLondon, Some(5.0)).create
          val simpleProductAtRomeStock = Factory.stock(simpleProductAtRome, Some(1.0)).create

          val productTemplateAtLondon = Factory.productLocation(product, london, priceAmount = Some(5)).create
          val productVariant1AtLondon =
            Factory.productLocation(productVariant1, london, priceAmount = Some(5), costAmount = Some(2)).create
          val productVariant2AtLondon =
            Factory.productLocation(productVariant2, london, priceAmount = Some(7), costAmount = Some(4)).create
          val productVariantDeletedAtLondon =
            Factory
              .productLocation(productVariantDeleted, london, priceAmount = Some(70000), costAmount = Some(40000))
              .create

          val productTemplateAtRome = Factory.productLocation(product, rome, priceAmount = Some(14)).create
          val productVariant1AtRome = Factory.productLocation(productVariant1, rome, priceAmount = Some(15)).create
          val productVariant2AtRome = Factory.productLocation(productVariant2, rome, priceAmount = Some(13)).create
          val productVariantDeletedAtRome =
            Factory.productLocation(productVariantDeleted, rome, priceAmount = Some(130000)).create

          val productVariant1AtLondonStock = Factory.stock(productVariant1AtLondon, Some(2.0)).create
          val productVariantDeletedAtLondonStock = Factory.stock(productVariantDeletedAtLondon, Some(200000)).create
          val productVariant1AtRomeStock = Factory.stock(productVariant1AtRome, Some(1.0)).create
          val productVariant2AtRomeStock = Factory.stock(productVariant2AtRome, Some(4.0)).create
          val productVariantDeletedAtRomeStock = Factory.stock(productVariantDeletedAtRome, Some(400000)).create

          Get(s"/v1/products.list?expand[]=stock_level,price_ranges,cost_ranges&location_id=${london.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val productsResponse = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            assertResponse(
              productsResponse.find(_.id == product.id).get,
              product,
              locationIds = Seq(london.id),
              stockLevel = Some(2.0),
              priceRange = Some(MonetaryRange(5.$$$, 7.$$$)),
              costRange = Some(MonetaryRange(2.$$$, 4.$$$)),
            )
            assertResponse(
              productsResponse.find(_.id == simpleProduct.id).get,
              simpleProduct,
              locationIds = Seq(london.id),
              stockLevel = Some(5.0),
              priceRange = Some(MonetaryRange(3.$$$, 3.$$$)),
              costRange = Some(MonetaryRange(1.$$$, 1.$$$)),
            )
          }
        }
      }

      "with expand=stock_level,variants,price_ranges" should {
        "return a list of products, matching showing stock levels for all locations" in new ProductsListFSpecContext {
          val tShirtsCategory = Factory.systemCategory(defaultMenuCatalog).create
          val modifierSet = Factory.modifierSet(merchant).create

          val simpleProduct = Factory.simpleProduct(merchant).create

          val product = Factory.templateProduct(merchant, categories = Seq(tShirtsCategory)).create
          val productVariant1 = Factory.variantProduct(merchant, product, priceAmount = Some(1)).create
          val productVariant2 = Factory.variantProduct(merchant, product, priceAmount = Some(2)).create
          val productVariant3 = Factory.variantProduct(merchant, product, priceAmount = Some(3)).create

          Factory.modifierSetProduct(modifierSet, product).create
          val taxRate = Factory.taxRate(merchant, name = None, locations = Seq(london), products = Seq(product)).create

          val simpleProductAtLondon = Factory.productLocation(simpleProduct, london, priceAmount = Some(7)).create
          val simpleProductAtRome = Factory.productLocation(simpleProduct, rome, priceAmount = Some(14)).create

          val simpleProductAtLondonStock = Factory.stock(simpleProductAtLondon, Some(5.0)).create
          val simpleProductAtRomeStock = Factory.stock(simpleProductAtRome, Some(1.0)).create

          val productVariant1AtLondon = Factory.productLocation(productVariant1, london, priceAmount = Some(5)).create
          val productVariant1AtRome = Factory.productLocation(productVariant1, rome, priceAmount = Some(15)).create
          val productVariant2AtRome = Factory.productLocation(productVariant2, rome, priceAmount = Some(10)).create

          val productVariant1AtLondonStock = Factory.stock(productVariant1AtLondon, Some(2.0)).create
          val productVariant1AtRomeStock = Factory.stock(productVariant1AtRome, Some(1.0)).create
          val productVariant2AtRomeStock = Factory.stock(productVariant2AtRome, Some(4.0)).create

          Get(s"/v1/products.list?expand[]=stock_level,variants,price_ranges")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val productsResponse = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data

            assertResponse(
              productsResponse.find(_.id == product.id).get,
              product,
              locationIds = Seq(london.id),
              variantIds = Seq(productVariant1.id, productVariant2.id, productVariant3.id),
              stockLevel = Some(7.0),
              variantStockLevels = Seq(Some(3.0), Some(4.0), Some(0)),
              priceRange = Some(MonetaryRange(5.$$$, 15.$$$)),
            )

            assertResponse(
              productsResponse.find(_.id == simpleProduct.id).get,
              simpleProduct,
              locationIds = Seq(london.id, rome.id),
              variantIds = Seq.empty,
              stockLevel = Some(6.0),
              variantStockLevels = Seq.empty,
              priceRange = Some(MonetaryRange(7.$$$, 14.$$$)),
            )
          }
        }
      }

      "with expand[]=suppliers" should {

        "returns a list of products enriched with suppliers" in new ProductsListFSpecContext {
          val supplier = Factory.supplier(merchant).create

          val templateProduct = Factory.templateProduct(merchant, name = Some("A")).create
          val simpleProduct = Factory.simpleProduct(merchant, name = Some("B")).create

          Factory.supplierProduct(supplier, templateProduct).create
          Factory.supplierProduct(supplier, simpleProduct).create

          Get("/v1/products.list?expand[]=suppliers").addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) ==== Seq(templateProduct.id, simpleProduct.id)

            assertResponse(
              products.find(_.id == templateProduct.id).get,
              templateProduct,
              supplierIds = Seq(supplier.id),
            )
            assertResponse(products.find(_.id == simpleProduct.id).get, simpleProduct, supplierIds = Seq(supplier.id))
          }
        }
      }

      "with loyalty_program_id" should {

        "return a list of products matching the filters" in new ProductsListFSpecContext {
          val jeans = Factory.simpleProduct(merchant, name = Some("1 jeans")).create
          val tShirt = Factory.simpleProduct(merchant, name = Some("2 tShirt")).create
          val bananas = Factory.simpleProduct(merchant, name = Some("3 bananas")).create

          val loyaltyProgram = Factory.loyaltyProgram(merchant).create
          val loyaltyReward = Factory.loyaltyReward(loyaltyProgram).create
          Factory.loyaltyRewardProduct(loyaltyReward, jeans).create
          Factory.loyaltyRewardProduct(loyaltyReward, tShirt).create

          Get(s"/v1/products.list?loyalty_reward_id=${loyaltyReward.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) ==== Seq(jeans.id, tShirt.id)

            assertResponse(products.find(_.id == jeans.id).get, jeans)
            assertResponse(products.find(_.id == tShirt.id).get, tShirt)
          }
        }
      }

      "filtered by updated_since date-time" should {
        "return a paginated list of all products sorted by received at in descending order and filtered by updated_since date-time" in new ProductsListFSpecContext {
          val now = ZonedDateTime.parse("2015-12-03T20:15:30Z")

          val jeans =
            Factory.simpleProduct(merchant, name = Some("1 jeans"), overrideNow = Some(now.minusDays(1))).create
          val tShirt = Factory.simpleProduct(merchant, name = Some("2 tShirt"), overrideNow = Some(now)).create
          val bananas =
            Factory.simpleProduct(merchant, name = Some("3 bananas"), overrideNow = Some(now.minusDays(1))).create
          Factory.productLocation(bananas, london, overrideNow = Some(now.plusDays(1))).create

          Get(s"/v1/products.list?updated_since=2015-12-03").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[ApiResponseWithMetadata[Seq[ProductEntity]]].data
            entities.map(_.id) ==== Seq(tShirt.id, bananas.id)
            assertResponse(entities.find(_.id == tShirt.id).get, tShirt)
            assertResponse(entities.find(_.id == bananas.id).get, bananas, locationIds = Seq(london.id))
          }
        }
      }

      "with low_inventory parameters" should {

        "when low_inventory=true" should {

          "return a list of products matching the filters" in new ProductsListFSpecContext {
            val tShirt = Factory.simpleProduct(merchant).create
            val tShirtRome = Factory.productLocation(tShirt, rome).create
            val lowStock = Factory.stock(tShirtRome, quantity = Some(5), minimumOnHand = Some(10)).create

            val jeans = Factory.simpleProduct(merchant).create
            val jeansRome = Factory.productLocation(jeans, rome).create
            val highStock = Factory.stock(jeansRome, quantity = Some(10), minimumOnHand = Some(5)).create

            Get(s"/v1/products.list?low_inventory=true").addHeader(authorizationHeader) ~> routes ~> check {
              val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
              products.map(_.id) ==== Seq(tShirt.id)

              assertResponse(products.find(_.id == tShirt.id).get, tShirt, locationIds = Seq(rome.id))
            }
          }
        }

        "when low_inventory=false" should {

          "return a list of products matching the filters" in new ProductsListFSpecContext {
            val tShirt = Factory.simpleProduct(merchant).create
            val tShirtRome = Factory.productLocation(tShirt, rome).create
            val lowStock = Factory.stock(tShirtRome, quantity = Some(5), minimumOnHand = Some(10)).create

            val jeans = Factory.simpleProduct(merchant).create
            val jeansRome = Factory.productLocation(jeans, rome).create
            val highStock = Factory.stock(jeansRome, quantity = Some(10), minimumOnHand = Some(5)).create

            Get(s"/v1/products.list?low_inventory=false").addHeader(authorizationHeader) ~> routes ~> check {
              val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
              products.map(_.id) ==== Seq(jeans.id)

              assertResponse(products.find(_.id == jeans.id).get, jeans, locationIds = Seq(rome.id))
            }
          }
        }
      }

      "with expand=reorder_amount and location_id" should {
        "return a list of products, matching showing reorder_amount for the given location" in new ProductsListFSpecContext {
          val tShirtsCategory = Factory.systemCategory(defaultMenuCatalog).create
          val modifierSet = Factory.modifierSet(merchant).create

          val simpleProduct = Factory.simpleProduct(merchant).create
          val product = Factory.templateProduct(merchant, categories = Seq(tShirtsCategory)).create
          val productVariant1 = Factory.variantProduct(merchant, product).create
          val productVariant2 = Factory.variantProduct(merchant, product).create

          Factory.modifierSetProduct(modifierSet, product).create
          val taxRate = Factory.taxRate(merchant, name = None, locations = Seq(london), products = Seq(product)).create

          val simpleProductAtLondon = Factory.productLocation(simpleProduct, london).create
          val simpleProductAtRome = Factory.productLocation(simpleProduct, rome).create

          val simpleProductAtLondonStock = Factory.stock(simpleProductAtLondon, reorderAmount = Some(5.0)).create
          val simpleProductAtRomeStock = Factory.stock(simpleProductAtRome, reorderAmount = Some(1.0)).create

          val productVariant1AtLondon = Factory.productLocation(productVariant1, london).create
          val productVariant1AtRome = Factory.productLocation(productVariant1, rome).create
          val productVariant2AtRome = Factory.productLocation(productVariant2, rome).create

          val productVariant1AtLondonStock = Factory.stock(productVariant1AtLondon, reorderAmount = Some(2.0)).create
          val productVariant1AtRomeStock = Factory.stock(productVariant1AtRome, reorderAmount = Some(1.0)).create
          val productVariant2AtRomeStock = Factory.stock(productVariant2AtRome, reorderAmount = Some(4.0)).create

          Get(s"/v1/products.list?expand[]=reorder_amount&location_id=${london.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val productsResponse = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            assertResponse(
              productsResponse.find(_.id == product.id).get,
              product,
              locationIds = Seq(london.id),
              reorderAmount = Some(2.0),
            )
            assertResponse(
              productsResponse.find(_.id == simpleProduct.id).get,
              simpleProduct,
              locationIds = Seq(london.id),
              reorderAmount = Some(5.0),
            )
          }
        }
      }

      "with filter is_combo" should {
        "return a list of non-combo products ordered by name" in new ProductsListFSpecContext {
          val templateProduct = Factory.templateProduct(merchant, name = Some("A product")).create
          val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
          val simpleProduct = Factory.simpleProduct(merchant, name = Some("B product")).create
          val comboProduct = Factory.comboProduct(merchant, name = Some("C product")).create

          val templatePart = Factory.templatePart(merchant, name = Some("A part")).create
          val variantPart = Factory.variantPart(merchant, templatePart, name = Some("AA part")).create
          val simplePart = Factory.simplePart(merchant, name = Some("B part")).create
          val comboPart = Factory.comboPart(merchant, name = Some("C part")).create

          Get("/v1/products.list?is_combo=false").addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) ==== Seq(templateProduct.id, simpleProduct.id)

            assertResponse(products.find(_.id == templateProduct.id).get, templateProduct)
            assertResponse(products.find(_.id == simpleProduct.id).get, simpleProduct)
          }
        }

        "return a list of combo products ordered by name" in new ProductsListFSpecContext {
          val templateProduct = Factory.templateProduct(merchant, name = Some("A product")).create
          val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
          val simpleProduct = Factory.simpleProduct(merchant, name = Some("B product")).create
          val comboProduct = Factory.comboProduct(merchant, name = Some("C product")).create

          val templatePart = Factory.templatePart(merchant, name = Some("A part")).create
          val variantPart = Factory.variantPart(merchant, templatePart, name = Some("AA part")).create
          val simplePart = Factory.simplePart(merchant, name = Some("B part")).create
          val comboPart = Factory.comboPart(merchant, name = Some("C part")).create

          Get("/v1/products.list?is_combo=true").addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) ==== Seq(comboProduct.id)

            assertResponse(products.find(_.id == comboProduct.id).get, comboProduct)
          }
        }
      }

      "with ids paramater" should {

        "return a list of owned products filtered by id" in new ProductsListFSpecContext {
          val templateProduct = Factory.templateProduct(merchant, name = Some("A product")).create
          val variantProduct = Factory.variantProduct(merchant, templateProduct, name = Some("AA product")).create
          val simpleProduct = Factory.simpleProduct(merchant, name = Some("B product")).create
          val comboProduct = Factory.comboProduct(merchant, name = Some("C product")).create

          val templatePart = Factory.templatePart(merchant, name = Some("A part")).create
          val variantPart = Factory.variantPart(merchant, templatePart, name = Some("AA part")).create
          val simplePart = Factory.simplePart(merchant, name = Some("B part")).create
          val comboPart = Factory.comboPart(merchant, name = Some("C part")).create

          val competitor = Factory.merchant.create
          val competitorTemplateProduct = Factory.templateProduct(competitor, name = Some("A product")).create

          Get(s"/v1/products.list?ids[]=${templateProduct.id},${simpleProduct.id},${competitorTemplateProduct.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val products = responseAs[PaginatedApiResponse[Seq[ProductEntity]]].data
            products.map(_.id) ==== Seq(templateProduct.id, simpleProduct.id)

            assertResponse(products.find(_.id == templateProduct.id).get, templateProduct)
            assertResponse(products.find(_.id == simpleProduct.id).get, simpleProduct)
          }
        }
      }
    }

    "if request has valid app token" should {
      "return the product" in new ProductsListFSpecContext with AppTokenFixtures {
        val product = Factory.simpleProduct(merchant).create

        Get(s"/v1/products.list").addHeader(appAuthorizationHeader) ~> routes ~> check {
          assertStatusOK()
          val entitiies = responseAs[ApiResponse[Seq[ProductEntity]]].data
          entitiies.map(_.id) ==== Seq(product.id)

          assertResponse(entitiies.find(_.id == product.id).get, product)
        }
      }
    }

    "if request has invalid token" should {
      "reject request" in new ProductsListFSpecContext {
        val product = Factory.simpleProduct(merchant).create

        Get(s"/v1/products.list").addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
