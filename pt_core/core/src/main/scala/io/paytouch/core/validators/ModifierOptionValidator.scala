package io.paytouch.core.validators

import java.util.UUID

import io.paytouch.core.data.daos.{ Daos, ModifierOptionDao }
import io.paytouch.core.data.model.ModifierOptionRecord
import io.paytouch.core.errors.{
  InvalidModifierOptionIds,
  InvalidModifierSetIdPerModifierOptions,
  NonAccessibleModifierOptionIds,
}
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

class ModifierOptionValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[ModifierOptionRecord] {

  type Dao = ModifierOptionDao
  type Record = ModifierOptionRecord

  protected val dao = daos.modifierOptionDao
  val validationErrorF = InvalidModifierOptionIds(_)
  val accessErrorF = NonAccessibleModifierOptionIds(_)

  def validateByIdsWithModifierSetId(ids: Seq[UUID], modifierSetId: UUID): Future[ErrorsOr[Seq[Record]]] =
    dao.findByIds(ids).map {
      case modifierOptions if modifierOptions.forall(_.modifierSetId == modifierSetId) =>
        Multiple.success(modifierOptions)
      case modifierOptions =>
        val invalidModifierOptionIds = modifierOptions.filterNot(_.modifierSetId == modifierSetId).map(_.id)
        Multiple.failure(InvalidModifierSetIdPerModifierOptions(invalidModifierOptionIds, modifierSetId))
    }
}
