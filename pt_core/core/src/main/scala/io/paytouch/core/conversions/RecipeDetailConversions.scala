package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ RecipeDetailRecord, RecipeDetailUpdate }
import io.paytouch.core.entities.{ UserContext, RecipeDetail => RecipeDetailEntity }

trait RecipeDetailConversions extends EntityConversion[RecipeDetailRecord, RecipeDetailEntity] {

  def fromRecordToEntity(record: RecipeDetailRecord)(implicit user: UserContext): RecipeDetailEntity =
    RecipeDetailEntity(id = record.id, makesQuantity = record.makesQuantity)

  def toUpdate(recipeId: UUID, makesQuantity: BigDecimal)(implicit user: UserContext) =
    RecipeDetailUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      productId = Some(recipeId),
      makesQuantity = Some(makesQuantity),
    )

}
