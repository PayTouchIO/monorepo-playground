package io.paytouch.core.generators

import io.paytouch.core.data.model.enums.ImageUploadType._

import scala.concurrent.ExecutionContext

class RestaurantDataProvider(implicit val ec: ExecutionContext) extends DataProvider {

  lazy val directory = "restaurant"

  lazy val imageMap = Map(EmailReceipt -> 1, PrintReceipt -> 1)

}
