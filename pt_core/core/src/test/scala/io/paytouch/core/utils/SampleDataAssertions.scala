package io.paytouch.core.utils

import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.model.enums._
import io.paytouch.core.data.model.UserRoleUpdate
import io.paytouch.core.services.ServiceDaoSpec

trait SampleDataAssertions { self: ServiceDaoSpec =>
  abstract class SampleDataServiceSpecContext extends ServiceDaoSpecContext with UserFixtures {
    override lazy val locations = Seq.empty
    val merchantDao = daos.merchantDao

    override lazy val userRole = daos
      .userRoleDao
      .bulkUpsert(UserRoleUpdate.defaults(merchant.id, SetupType.Paytouch))
      .map { entitiesWithResultType =>
        entitiesWithResultType
          .map {
            case (_, entity) => entity
          }
          .find(_.name == UserRoleUpdate.Admin)
          .get
      }
      .await

    def assertHasLoadedData(
        userCount: Int,
        locationsCount: Int = 0,
        taxRatesCount: Int = 0,
        taxRateLocationsCount: Int = 0,
        discountsCount: Int = 0,
        discountLocationsCount: Int = 0,
        categoriesCount: Int = 0,
        categoryLocationsCount: Int = 0,
        brandsCount: Int = 0,
        suppliersCount: Int = 0,
        supplierLocationsCount: Int = 0,
        modifierSetsCount: Int = 0,
        modifierOptionsCount: Int = 0,
        modifierSetLocationsCount: Int = 0,
        modifierSetCountProductRel: Int = 0,
        productCount: Int = 0,
        productCountLocationRel: Int = 0,
        productCountCategoryRel: Int = 0,
        productCountProductLocationTaxRateRel: Int = 0,
        productCountProductVariantOptionRel: Int = 0,
        productCountPartRel: Int = 0,
        stockCount: Int = 0,
        userCountLocationRel: Int = 0,
        customerCount: Int = 0,
        customerCountGroupRel: Int = 0,
        groupCount: Int = 0,
        orderCount: Int = 0,
        orderCountItemRel: Int = 0,
        orderCountPaymentTransactionRel: Int = 0,
        shiftCount: Int = 0,
        timeCardCount: Int = 0,
        timeOffCardCount: Int = 0,
        imageCount: Int = 0,
      ) = {
      afterAWhile {
        val updatedMerchant = merchantDao.findById(merchant.id).await.get
        updatedMerchant.loadingStatus ==== LoadingStatus.Successful
        updatedMerchant.updatedAt !=== merchant.updatedAt
      }

      assertLocationLoaded(locationsCount)
      assertTaxRateLoaded(taxRatesCount, taxRateLocationsCount)
      assertDiscountLoaded(discountsCount, discountLocationsCount)
      assertBrandLoaded(brandsCount)
      assertSystemCategoryLoaded(categoriesCount, categoryLocationsCount)
      assertSupplierLoaded(suppliersCount, supplierLocationsCount)
      assertModifierSetLoaded(
        modifierSetsCount,
        modifierOptionsCount,
        modifierSetLocationsCount,
        modifierSetCountProductRel,
      )
      assertProductLoaded(
        productCount,
        productCountLocationRel,
        productCountCategoryRel,
        productCountProductLocationTaxRateRel,
        productCountProductVariantOptionRel,
        productCountPartRel,
      )
      assertStockLoaded(stockCount)
      assertUserLoaded(userCount, userCountLocationRel)
      assertCustomerMerchantLoaded(customerCount, customerCountGroupRel)
      assertGroupLoaded(groupCount)
      assertOrderLoaded(orderCount, orderCountItemRel, orderCountPaymentTransactionRel)
      assertShiftLoaded(shiftCount)
      assertTimeCardLoaded(timeCardCount)
      assertTimeOffCardLoaded(timeOffCardCount)
      assertImageUploadLoaded(imageCount)
    }

    private def assertLocationLoaded(expectedCount: Int) = {
      assertCount(daos.locationDao, expectedCount)
      assertCount(daos.locationSettingsDao, expectedCount)
      assertCount(daos.locationReceiptDao, expectedCount)
      assertCount(daos.locationEmailReceiptDao, expectedCount)
      assertCount(daos.locationPrintReceiptDao, expectedCount)
    }

    private def assertTaxRateLoaded(expectedCount: Int, expectedCountLocationRel: Int) = {
      assertCount(daos.taxRateDao, expectedCount)
      assertCount(daos.taxRateLocationDao, expectedCountLocationRel)
    }

    private def assertDiscountLoaded(expectedCount: Int, expectedCountLocationRel: Int) = {
      assertCount(daos.discountDao, expectedCount)
      assertCount(daos.discountLocationDao, expectedCountLocationRel)
    }

    private def assertBrandLoaded(expectedCount: Int) =
      assertCount(daos.brandDao, expectedCount)

    private def assertSystemCategoryLoaded(expectedCount: Int, expectedCountLocationRel: Int) = {
      assertCount(daos.systemCategoryDao, expectedCount)
      assertCount(daos.categoryLocationDao, expectedCountLocationRel)
    }

    private def assertSupplierLoaded(expectedCount: Int, expectedCountLocationRel: Int) = {
      assertCount(daos.supplierDao, expectedCount)
      assertCount(daos.supplierLocationDao, expectedCountLocationRel)
    }

    private def assertModifierSetLoaded(
        expectedCount: Int,
        expectedOptionCount: Int,
        expectedCountLocationRel: Int,
        expectedCountProductRel: Int,
      ) = {
      assertCount(daos.modifierSetDao, expectedCount)
      assertCount(daos.modifierOptionDao, expectedOptionCount)
      assertCount(daos.modifierSetLocationDao, expectedCountLocationRel)
      assertCount(daos.modifierSetProductDao, expectedCountProductRel)
    }

    private def assertProductLoaded(
        expectedCount: Int,
        expectedCountLocationRel: Int,
        expectedCountCategoryRel: Int,
        expectedCountProductLocationTaxRateRel: Int,
        expectedCountProductVariantOptionRel: Int,
        expectedCountPartRel: Int,
      ) = {
      assertCount(daos.articleDao, expectedCount)
      assertCount(daos.productLocationDao, expectedCountLocationRel)
      assertCount(daos.productCategoryDao, expectedCountCategoryRel)
      assertCount(daos.productLocationTaxRateDao, expectedCountProductLocationTaxRateRel)
      assertCount(daos.productVariantOptionDao, expectedCountProductVariantOptionRel)
      assertCount(daos.productPartDao, expectedCountPartRel)
    }

    private def assertStockLoaded(expectedCount: Int) =
      assertCount(daos.stockDao, expectedCount)

    private def assertUserLoaded(expectedCount: Int, expectedCountLocationRel: Int) = {
      assertCount(daos.userDao, expectedCount)
      assertCount(daos.userLocationDao, expectedCountLocationRel)
    }

    private def assertCustomerMerchantLoaded(expectedCount: Int, expectedCountGroupRel: Int) = {
      assertCount(daos.customerMerchantDao, expectedCount)
      assertCount(daos.customerGroupDao, expectedCountGroupRel)
    }

    private def assertGroupLoaded(expectedCount: Int) = assertCount(daos.groupDao, expectedCount)

    private def assertOrderLoaded(
        expectedCount: Int,
        expectedCountItemRel: Int,
        expectedCountPaymentTransactionRel: Int,
      ) = {
      assertCount(daos.orderDao, expectedCount)
      assertCount(daos.orderItemDao, expectedCountItemRel)
      assertCount(daos.paymentTransactionDao, expectedCountPaymentTransactionRel)

      val orders = daos.orderDao.findAllByMerchantId(merchant.id).await
      orders.forall(_.number.isDefined) should beTrue
      val ordersSortedByNumber = orders.sortBy(_.number.getOrElse("-1").toInt)
      val ordersSortedByReceivedAtAndId = orders.sortBy { case o => (o.receivedAt.toString, o.id) }
      ordersSortedByNumber.map(_.number) ==== ordersSortedByReceivedAtAndId.map(_.number)
    }

    private def assertShiftLoaded(expectedCount: Int) = assertCount(daos.shiftDao, expectedCount)

    private def assertTimeCardLoaded(expectedCount: Int) = assertCount(daos.timeCardDao, expectedCount)

    private def assertTimeOffCardLoaded(expectedCount: Int) = assertCount(daos.timeOffCardDao, expectedCount)

    private def assertImageUploadLoaded(expectedCount: Int) =
      assertCount(daos.imageUploadDao, expectedCount)

    private def assertCount(dao: SlickMerchantDao, expectedCount: Int) =
      dao.findAllByMerchantId(merchant.id).await.size ==== expectedCount
  }
}
