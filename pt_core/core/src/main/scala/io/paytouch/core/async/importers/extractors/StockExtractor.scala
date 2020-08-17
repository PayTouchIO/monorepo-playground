package io.paytouch.core.async.importers.extractors

import java.util.UUID

import io.paytouch.core.async.importers.parsers.EnrichedDataRow
import io.paytouch.core.async.importers.{ Keys, Rows, UpdatesWithCount }
import io.paytouch.core.data.model._
import io.paytouch.core.utils.MultipleExtraction
import io.paytouch.core.utils.MultipleExtraction.ErrorsOr

import scala.concurrent._

trait StockExtractor extends Extractor {

  val stockDao = daos.stockDao

  def extractStocks(data: Rows)(implicit importRecord: ImportRecord): Future[Extraction[StockUpdate]] = {
    logExtraction("stocks")
    val cleanData = data.filter(containsStockData)
    val extractedStocks = MultipleExtraction.sequence(cleanData.map(extractStockPerRow))
    val productIds = extractedStocks.getOrElse(Seq.empty).flatMap(_.productId)
    for {
      existingStocks <- stockDao.findByProductIdsAndMerchantId(productIds, importRecord.merchantId)
    } yield extractedStocks.map { extracted =>
      val extractedWithIds = extracted.map { extractedStock =>
        val existingId = existingStocks.find(s => extractedStock.productId.contains(s.productId)).map(_.id)
        extractedStock.copy(id = existingId)
      }
      toUpdatesWithCount(extractedWithIds)
    }
  }

  private def extractStockPerRow(
      row: EnrichedDataRow,
    )(implicit
      importRecord: ImportRecord,
    ): ErrorsOr[Seq[StockUpdate]] = {
    val quantity = extractQuantity(row)
    val minimumOnHand = extractMinOnHandAmount(row)
    MultipleExtraction.combine(quantity, minimumOnHand) {
      case (q, mOh) =>
        row.storableArticleIds.flatMap { productId =>
          toStockUpdates(productId = productId, quantity = q, minimumOnHand = mOh)
        }
    }
  }

  private def extractQuantity(row: EnrichedDataRow): ErrorsOr[BigDecimal] =
    extractBigDecimalWithDefault(row, Keys.StockQuantity)(0)

  private def extractMinOnHandAmount(row: EnrichedDataRow): ErrorsOr[BigDecimal] =
    extractBigDecimalWithDefault(row, Keys.StockMinimumOnHand)(0)

  private def toUpdatesWithCount(stocks: Seq[StockUpdate]): UpdatesWithCount[StockUpdate] =
    UpdatesWithCount(
      updates = stocks.map(s => s.copy(id = s.id.orElse(Some(UUID.randomUUID)))),
      toAdd = stocks.count(_.id.isEmpty),
      toUpdate = stocks.count(_.id.isDefined),
    )

  private def toStockUpdates(
      productId: UUID,
      quantity: BigDecimal,
      minimumOnHand: BigDecimal,
    )(implicit
      importRecord: ImportRecord,
    ): Seq[StockUpdate] =
    importRecord.locationIds.map { locationId =>
      StockUpdate(
        id = None,
        merchantId = Some(importRecord.merchantId),
        productId = Some(productId),
        locationId = Some(locationId),
        quantity = Some(quantity),
        minimumOnHand = Some(minimumOnHand),
        reorderAmount = None,
        sellOutOfStock = None,
      )
    }
}
