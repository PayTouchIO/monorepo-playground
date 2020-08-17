package io.paytouch.core.data.daos

import io.paytouch.core.data.daos.features.{ SlickFindByProductDao, SlickRelDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ RecipeDetailRecord, RecipeDetailUpdate }
import io.paytouch.core.data.tables.RecipeDetailsTable

import scala.concurrent.ExecutionContext

class RecipeDetailDao(implicit val ec: ExecutionContext, val db: Database)
    extends SlickFindByProductDao
       with SlickRelDao {

  type Record = RecipeDetailRecord
  type Update = RecipeDetailUpdate
  type Table = RecipeDetailsTable

  val table = TableQuery[Table]

  def queryByRelIds(upsertion: RecipeDetailUpdate) = {
    require(upsertion.productId.isDefined, "RecipeDetailDao - Impossible to find by product id without a product id")
    queryFindByProductId(upsertion.productId.get)
  }
}
