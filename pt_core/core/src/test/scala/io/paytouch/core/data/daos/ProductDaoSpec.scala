package io.paytouch.core.data.daos

import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.ArticleType
import io.paytouch.core.data.model.upsertions.ArticleUpsertion
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ProductDaoSpec extends DaoSpec {

  lazy val productDao = daos.productDao
  lazy val productLocationDao = daos.productLocationDao

  abstract class ProductDaoSpecContext extends DaoSpecContext with BaseFixtures {
    def findProduct(product: ArticleRecord) = productDao.findById(product.id).await.get
    def findProductLocation(productLocation: ProductLocationRecord) =
      productLocationDao.findById(productLocation.id).await.get

    def assertUpsertionWasApplied(
        originalProduct: ArticleRecord,
        originalProductLocation: ProductLocationRecord,
        modifiedProduct: ArticleUpdate,
        modifiedProductLocation: ProductLocationUpdate,
      ) = {
      modifiedProduct.description ==== findProduct(originalProduct).description
      modifiedProductLocation.margin ==== findProductLocation(originalProductLocation).margin
    }

    def assertUpsertionWasntApplied(
        originalProduct: ArticleRecord,
        originalProductLocation: ProductLocationRecord,
        modifiedProduct: ArticleUpdate,
        modifiedProductLocation: ProductLocationUpdate,
      ) = {
      findProduct(originalProduct).description !=== modifiedProduct.description
      findProductLocation(originalProductLocation).margin !=== modifiedProductLocation.margin
    }

    def assertModelsHaventChanged(originalProduct: ArticleRecord, originalProductLocation: ProductLocationRecord) = {
      findProduct(originalProduct).description ==== originalProduct.description
      findProductLocation(originalProductLocation).margin ==== originalProductLocation.margin
    }

    def prepareUpsertion(modifiedProduct: ArticleUpdate, modifiedProductLocation: ProductLocationUpdate) =
      new ArticleUpsertion(
        product = modifiedProduct,
        variantProducts = None,
        variantOptionTypes = None,
        variantOptions = None,
        productLocations = Map(location.id -> Some(modifiedProductLocation)),
        productCategories = None,
        supplierProducts = None,
        productLocationTaxRates = Map.empty,
        imageUploads = None,
        recipeDetails = None,
        bundleSets = None,
      )
  }

  "ProductDao" in {

    "update" in {
      "if there is a value that triggers a failure in the transaction" should {
        "not apply the update and leave the db untouched" in new ProductDaoSpecContext with UpsertionFixtures {
          val modifiedProduct = productUpdate.copy(description = Some("Simple update"))
          val modifiedProductLocation = productLocationUpdate.copy(margin = Some(1000000000))
          val upsertion = prepareUpsertion(modifiedProduct, modifiedProductLocation)

          try {
            productDao.upsert(upsertion).await
            failure("upsert should have failed (and hasn't) because margin is out of limits for numeric field")
          }
          catch {
            case _: Throwable =>
          }
          finally {
            assertUpsertionWasntApplied(product, productLocation, modifiedProduct, modifiedProductLocation)
            assertModelsHaventChanged(product, productLocation)
          }
        }
      }

      "if all value are correct" should {
        "apply the update and change the db" in new ProductDaoSpecContext with UpsertionFixtures {
          val modifiedProduct = productUpdate.copy(description = Some("Simple update"))
          val modifiedProductLocation = productLocationUpdate.copy(margin = Some(10))
          val upsertion = prepareUpsertion(modifiedProduct, modifiedProductLocation)

          productDao.upsert(upsertion).await

          assertUpsertionWasApplied(product, productLocation, modifiedProduct, modifiedProductLocation)
        }
      }
    }

    "findMainByMerchantId" in {
      "with no extra filters" should {
        "return products ordered by position" in new ProductDaoSpecContext with FindMainByMerchantIdFixtures {
          Factory.productCategory(simpleProduct1, tshirts, position = Some(3)).create
          Factory.productCategory(simpleProduct2, tshirts, position = Some(1)).create
          Factory.productCategory(simpleProduct3, tshirts, position = Some(2)).create

          val products = productDao.findAllByMerchantId(merchant.id, articleTypes = Some(ArticleType.mains)).await

          products.map(_.id) ==== Seq(simpleProduct1.id, simpleProduct2.id, simpleProduct3.id)
        }
      }
    }
  }

  trait BaseFixtures {
    val merchant = Factory.merchant.create
    val location = Factory.location(merchant).create
  }

  trait UpsertionFixtures extends BaseFixtures {
    val (product, productUpdate) =
      Factory.templateProduct(merchant, name = Some("main product")).map(factory => (factory.create, factory.get))
    val productVariant = Factory.variantProduct(merchant, product).create
    val (productLocation, productLocationUpdate) =
      Factory.productLocation(product, location).map(factory => (factory.create, factory.get))
  }

  trait FindMainByMerchantIdFixtures extends BaseFixtures {
    val defaultMenuCatalog = Factory.defaultMenuCatalog(merchant).create
    val tshirts = Factory.systemCategory(defaultMenuCatalog).create
    val simpleProduct1 = Factory.simpleProduct(merchant, name = Some("A")).create
    val simpleProduct2 = Factory.simpleProduct(merchant, name = Some("B")).create
    val simpleProduct3 = Factory.simpleProduct(merchant, name = Some("C")).create
  }
}
