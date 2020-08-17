package io.paytouch.core.services

import scala.concurrent.duration._

import io.paytouch.implicits._

import io.paytouch.core.data.model.enums.BusinessType._
import io.paytouch.core.data.model.enums.MerchantMode._
import io.paytouch.core.utils.{ SampleDataAssertions, FixtureDaoFactory => Factory }

class SampleDataServiceSpec extends ServiceDaoSpec with SampleDataAssertions {
  implicit val timeout: FiniteDuration = 4 minutes

  "SampleDataService" in {
    "load" should {

      "when merchant mode is demo" should {

        "load sample data for a retail merchant" in new SampleDataServiceSpecContext {
          override lazy val merchant = Factory.merchant(mode = Some(Demo), businessType = Some(Retail)).create
          Factory.defaultMenuCatalog(merchant).create

          val result = sampleDataService.load(merchant).await
          result.success !=== 0

          assertHasLoadedData(
            locationsCount = 2,
            taxRatesCount = 2,
            taxRateLocationsCount = 3,
            discountsCount = 1,
            discountLocationsCount = 2,
            brandsCount = 2,
            categoriesCount = 2,
            categoryLocationsCount = 4,
            suppliersCount = 2,
            supplierLocationsCount = 4,
            modifierSetsCount = 1,
            modifierOptionsCount = 3,
            modifierSetLocationsCount = 2,
            modifierSetCountProductRel = 2,
            productCount = 15,
            productCountLocationRel = 30,
            productCountCategoryRel = 5,
            productCountProductLocationTaxRateRel = 10,
            productCountProductVariantOptionRel = 18,
            stockCount = 26,
            userCount = 2,
            userCountLocationRel = 4,
            customerCount = 4,
            customerCountGroupRel = 1,
            groupCount = 1,
            orderCount = 10,
            orderCountItemRel = 14,
            orderCountPaymentTransactionRel = 10,
            shiftCount = 5,
            timeCardCount = 1,
            timeOffCardCount = 1,
            imageCount = 17,
          )
        }

        "load sample data for a restaurant merchant" in new SampleDataServiceSpecContext {
          override lazy val merchant = Factory.merchant(mode = Some(Demo), businessType = Some(Restaurant)).create
          Factory.defaultMenuCatalog(merchant).create

          val result = sampleDataService.load(merchant).await
          result.success !=== 0

          assertHasLoadedData(
            locationsCount = 1,
            taxRatesCount = 1,
            taxRateLocationsCount = 1,
            discountsCount = 2,
            discountLocationsCount = 2,
            categoriesCount = 3,
            categoryLocationsCount = 3,
            brandsCount = 3,
            suppliersCount = 3,
            supplierLocationsCount = 3,
            productCount = 34,
            productCountLocationRel = 34,
            productCountCategoryRel = 13,
            productCountProductLocationTaxRateRel = 13,
            productCountProductVariantOptionRel = 17,
            productCountPartRel = 12,
            stockCount = 28,
            userCount = 2,
            userCountLocationRel = 2,
            customerCount = 3,
            orderCount = 11,
            orderCountItemRel = 31,
            orderCountPaymentTransactionRel = 12,
            shiftCount = 3,
            timeCardCount = 1,
            timeOffCardCount = 1,
            imageCount = 2,
          )
        }
      }

      "when merchant mode is production" should {
        "load no data" in new SampleDataServiceSpecContext {
          override lazy val merchant = Factory.merchant(mode = Some(Production)).create
          Factory.defaultMenuCatalog(merchant).create

          val result = sampleDataService.load(merchant).await
          result.success ==== 0

          assertHasLoadedData(userCount = 1)
        }
      }
    }
  }
}
