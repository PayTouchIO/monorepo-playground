package io.paytouch.core.data.daos

import io.paytouch.core.utils.{ MultipleLocationFixtures, FixtureDaoFactory => Factory }

class CategoryDaoSpec extends DaoSpec {

  abstract class CategoryDaoSpecContext extends DaoSpecContext with BaseFixtures {
    lazy val categoryDao = daos.categoryDao
  }

  "CategoryDao" in {
    "countProductsByCategoryIds without location filter" in {
      "with two products in a root category and one of those in another main category" should {
        "count 2 products in the first category and one in the other" in new CategoryDaoSpecContext {
          Factory.productCategory(cleanCodeBook, books).create

          Factory.productCategory(cleanCodeBook, bestsellers).create
          Factory.productCategory(tddBook, bestsellers).create

          categoryDao.countProductsByCategoryIds(Seq(books.id, bestsellers.id)).await ==== Map(
            books.id -> 1,
            bestsellers.id -> 2,
          )
        }
      }
      "with same product in a root category and in one of its subcategories" should {
        "count the same product once" in new CategoryDaoSpecContext {
          Factory.productCategory(cleanCodeBook, books).create
          Factory.productCategory(cleanCodeBook, books_manuals).create

          categoryDao.countProductsByCategoryIds(Seq(books.id, books_manuals.id)).await ==== Map(
            books.id -> 1,
            books_manuals.id -> 1,
          )
        }
      }
      "with same product in 2 subcategories of a different root category" should {
        "count the same product once for each subcategory and do not count it in the category" in new CategoryDaoSpecContext {
          Factory.productCategory(cleanCodeBook, books_manuals).create
          Factory.productCategory(cleanCodeBook, bestsellers_computerScience).create

          categoryDao
            .countProductsByCategoryIds(Seq(books.id, bestsellers.id, books_manuals.id, bestsellers_computerScience.id))
            .await ==== Map(books_manuals.id -> 1, bestsellers_computerScience.id -> 1)
        }
      }
      "with no products assigned" should {
        "count 0" in new CategoryDaoSpecContext {
          categoryDao.countProductsByCategoryIds(Seq(books.id)).await ==== Map()
        }
      }
    }

    "countProductsByCategoryIds with location filter" in {
      "with two products in a root category and one of those in another main category" should {
        "count 1 product in the first category and one in the other" in new CategoryDaoSpecContext {
          Factory.productCategory(cleanCodeBook, books).create

          Factory.productCategory(cleanCodeBook, bestsellers).create
          Factory.productCategory(tddBook, bestsellers).create

          categoryDao
            .countProductsByCategoryIds(Seq(books.id, bestsellers.id), locationIds = Some(Seq(rome.id)))
            .await ==== Map(books.id -> 1, bestsellers.id -> 1)
        }
      }
    }
  }

  trait BaseFixtures extends MultipleLocationFixtures {
    val cleanCodeBook = Factory.simpleProduct(merchant, locations = Seq(rome)).create
    val tddBook = Factory.simpleProduct(merchant, locations = Seq(london)).create
    val sicpBook = Factory.simpleProduct(merchant, locations = Seq(rome)).create

    val books = Factory.systemCategory(defaultMenuCatalog, name = Some("Books")).create
    val books_manuals = Factory.systemSubcategory(defaultMenuCatalog, books, name = Some("Manuals")).create

    val bestsellers = Factory.systemCategory(defaultMenuCatalog, name = Some("Bestsellers")).create
    val bestsellers_computerScience =
      Factory.systemSubcategory(defaultMenuCatalog, bestsellers, name = Some("Computer Science")).create
  }
}
