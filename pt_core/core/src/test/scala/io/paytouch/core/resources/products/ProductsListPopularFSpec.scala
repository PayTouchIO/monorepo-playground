package io.paytouch.core.resources.products

import io.paytouch.core.entities._
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class ProductsListPopularFSpec extends ProductsFSpec {
  abstract class ProductsListFSpecContext extends ProductResourceFSpecContext

  "GET /v1/products.list_popular" in {

    "if request has valid token" in {

      "with no parameters" should {

        "return a list of top popular products" in new ProductsListFSpecContext {
          val now = UtcTime.now

          val tShirt = Factory.simpleProduct(merchant).create
          val bag = Factory.simpleProduct(merchant).create
          val shoes = Factory.simpleProduct(merchant).create

          val tShirtOrder = Factory.order(merchant, location = Some(rome), receivedAt = Some(now)).create
          Factory.orderItem(tShirtOrder, product = Some(tShirt), quantity = Some(1)).create

          val bagOrderA = Factory.order(merchant, location = Some(rome), receivedAt = Some(now)).create
          Factory.orderItem(bagOrderA, product = Some(bag), quantity = Some(1)).create

          val bagOrderB = Factory.order(merchant, location = Some(rome), receivedAt = Some(now)).create
          (1 to 3).map(_ => Factory.orderItem(bagOrderB, product = Some(bag), quantity = Some(3)).create)

          val shoesOrder = Factory.order(merchant, location = Some(london), receivedAt = Some(now)).create
          (1 to 10).map(_ => Factory.orderItem(shoesOrder, product = Some(shoes), quantity = Some(10)).create)

          val oldOrder = Factory.order(merchant, location = Some(london), receivedAt = Some(now.minusDays(15))).create
          Factory.orderItem(oldOrder, product = Some(shoes), quantity = Some(100)).create

          Get("/v1/products.list_popular").addHeader(authorizationHeader) ~> routes ~> check {
            val popularProducts = responseAs[PaginatedApiResponse[Seq[Product]]].data
            popularProducts.map(_.id) ==== Seq(shoes.id, bag.id, tShirt.id)
            assertResponse(popularProducts.find(_.id == shoes.id).get, shoes)
            assertResponse(popularProducts.find(_.id == bag.id).get, bag)
            assertResponse(popularProducts.find(_.id == tShirt.id).get, tShirt)
          }
        }

        "return a list of top popular products (template included)" in new ProductsListFSpecContext {
          val now = UtcTime.now

          val tShirt = Factory.simpleProduct(merchant).create
          val bag = Factory.simpleProduct(merchant).create

          val shoesTemplate = Factory.templateProduct(merchant).create
          val shoesVariant = Factory.variantProduct(merchant, shoesTemplate).create

          val tShirtOrder = Factory.order(merchant, location = Some(rome), receivedAt = Some(now)).create
          Factory.orderItem(tShirtOrder, product = Some(tShirt), quantity = Some(1)).create

          val bagOrderA = Factory.order(merchant, location = Some(rome), receivedAt = Some(now)).create
          Factory.orderItem(bagOrderA, product = Some(bag), quantity = Some(1)).create

          val bagOrderB = Factory.order(merchant, location = Some(rome), receivedAt = Some(now)).create
          (1 to 3).map(_ => Factory.orderItem(bagOrderB, product = Some(bag), quantity = Some(3)).create)

          val shoesOrder = Factory.order(merchant, location = Some(london), receivedAt = Some(now)).create
          (1 to 10).map(_ => Factory.orderItem(shoesOrder, product = Some(shoesVariant), quantity = Some(10)).create)

          val oldOrder = Factory.order(merchant, location = Some(london), receivedAt = Some(now.minusDays(15))).create
          Factory.orderItem(oldOrder, product = Some(shoesVariant), quantity = Some(100)).create

          Get("/v1/products.list_popular").addHeader(authorizationHeader) ~> routes ~> check {
            val popularProducts = responseAs[PaginatedApiResponse[Seq[Product]]].data
            popularProducts.map(_.id) ==== Seq(shoesTemplate.id, bag.id, tShirt.id)
            assertResponse(popularProducts.find(_.id == shoesTemplate.id).get, shoesTemplate)
            assertResponse(popularProducts.find(_.id == bag.id).get, bag)
            assertResponse(popularProducts.find(_.id == tShirt.id).get, tShirt)
          }
        }
      }

      "with location_id filter" should {

        "return a list of products matching the filters" in new ProductsListFSpecContext {
          val now = UtcTime.now

          val tShirt = Factory.simpleProduct(merchant).create
          val bag = Factory.simpleProduct(merchant).create
          val shoes = Factory.simpleProduct(merchant).create

          val tShirtOrder = Factory.order(merchant, location = Some(rome), receivedAt = Some(now)).create
          Factory.orderItem(tShirtOrder, product = Some(tShirt), quantity = Some(1)).create

          val bagOrderA = Factory.order(merchant, location = Some(rome), receivedAt = Some(now)).create
          Factory.orderItem(bagOrderA, product = Some(bag), quantity = Some(1)).create

          val bagOrderB = Factory.order(merchant, location = Some(rome), receivedAt = Some(now)).create
          (1 to 3).map(_ => Factory.orderItem(bagOrderB, product = Some(bag), quantity = Some(3)).create)

          val shoesOrder = Factory.order(merchant, location = Some(london), receivedAt = Some(now)).create
          (1 to 10).map(_ => Factory.orderItem(shoesOrder, product = Some(shoes), quantity = Some(10)).create)

          val oldOrder = Factory.order(merchant, location = Some(london), receivedAt = Some(now.minusDays(15))).create
          Factory.orderItem(oldOrder, product = Some(shoes), quantity = Some(100)).create

          def assertRome(locationIds: String): Unit =
            Get(s"/v1/products.list_popular?$locationIds").addHeader(authorizationHeader) ~> routes ~> check {
              val popularProducts = responseAs[PaginatedApiResponse[Seq[Product]]].data

              popularProducts.map(_.id) should containTheSameElementsAs(Seq(bag, tShirt).map(_.id))

              assertResponse(popularProducts.find(_.id == bag.id).get, bag)
              assertResponse(popularProducts.find(_.id == tShirt.id).get, tShirt)
            }

          def assertLondonRome(locationIds: String): Unit =
            Get(s"/v1/products.list_popular?$locationIds").addHeader(authorizationHeader) ~> routes ~> check {
              val popularProducts = responseAs[PaginatedApiResponse[Seq[Product]]].data

              popularProducts.map(_.id) should containTheSameElementsAs(Seq(bag, tShirt, shoes).map(_.id))

              assertResponse(popularProducts.find(_.id == bag.id).get, bag)
              assertResponse(popularProducts.find(_.id == tShirt.id).get, tShirt)
              assertResponse(popularProducts.find(_.id == shoes.id).get, shoes)
            }

          val londonId = london.id
          val romeId = rome.id

          assertRome(s"location_id=$romeId")
          assertRome(s"location_id[]=$romeId")
          assertRome(s"location_id=$romeId&location_id[]=$romeId")
          assertLondonRome(s"location_id=$londonId&location_id[]=$romeId")
          assertLondonRome(s"location_id[]=$romeId,$londonId")
        }
      }
    }
  }
}
