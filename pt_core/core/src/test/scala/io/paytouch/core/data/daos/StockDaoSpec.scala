package io.paytouch.core.data.daos

import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class StockDaoSpec extends DaoSpec {

  abstract class StockDaoSpecContext extends DaoSpecContext with BaseFixtures {
    lazy val stockDao = daos.stockDao
  }

  "StockDao" in {
    "findStockLevelByVariantProductIds" in {
      "with no stocks" should {
        "return no stock quantities" in new StockDaoSpecContext {
          stockDao
            .findStockLevelByVariantProductIds(Seq(templateProduct.id, simpleProduct.id), allLocationIds)
            .await ==== Map()
        }
      }
      "with some stocks" should {
        "without filter by location" should {
          "return the sum of all stock quantities from all locations for each variant" in new StockDaoSpecContext
            with StockFixtures {
            stockDao
              .findStockLevelByVariantProductIds(Seq(variant1.id, variant2.id), allLocationIds)
              .await must containTheSameElementsAs(
              Map(
                variant1.id -> Map(london.id -> 1.0, rome.id -> 2.0),
                variant2.id -> Map(london.id -> 4.0, rome.id -> 8.0),
              ).toSeq,
            )
          }
        }
        "with optional filter by location" should {
          "return the sum of all stock quantities from one locations for each variant" in new StockDaoSpecContext
            with StockFixtures {
            stockDao
              .findStockLevelByVariantProductIds(Seq(variant1.id, variant2.id), Seq(london.id))
              .await must containTheSameElementsAs(
              Map(
                variant1.id -> Map(london.id -> 1.0),
                variant2.id -> Map(london.id -> 4.0),
              ).toSeq,
            )
          }
        }
      }
    }
  }

  trait BaseFixtures {
    val merchant = Factory.merchant.create
    val london = Factory.location(merchant).create
    val rome = Factory.location(merchant).create
    val allLocationIds = Seq(london.id, rome.id)

    val templateProduct = Factory.templateProduct(merchant).create
    val variant1 = Factory.variantProduct(merchant, templateProduct).create
    val variant2 = Factory.variantProduct(merchant, templateProduct).create

    val variant1London = Factory.productLocation(variant1, london).create
    val variant1Rome = Factory.productLocation(variant1, rome).create
    val variant2London = Factory.productLocation(variant2, london).create
    val variant2Rome = Factory.productLocation(variant2, rome).create

    val simpleProduct = Factory.simpleProduct(merchant).create
    val simpleProductLondon = Factory.productLocation(simpleProduct, london).create
    val simpleProductRome = Factory.productLocation(simpleProduct, rome).create
  }

  trait StockFixtures extends BaseFixtures {
    Factory.stock(variant1London, Some(1.0)).create
    Factory.stock(variant1Rome, Some(2.0)).create
    Factory.stock(variant2London, Some(4.0)).create
    Factory.stock(variant2Rome, Some(8.0)).create

    Factory.stock(simpleProductLondon, Some(16.0)).create
    Factory.stock(simpleProductRome, Some(32.0)).create
  }
}
