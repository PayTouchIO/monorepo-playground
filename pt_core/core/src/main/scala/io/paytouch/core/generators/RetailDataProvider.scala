package io.paytouch.core.generators

import io.paytouch.core.data.model.enums.ImageUploadType._

import scala.concurrent.ExecutionContext

class RetailDataProvider(implicit val ec: ExecutionContext) extends DataProvider {

  lazy val directory = "retail"

  lazy val imageMap = Map(Product -> 13, EmailReceipt -> 2, PrintReceipt -> 2)
}
