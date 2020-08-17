package io.paytouch.core.entities

import java.util.UUID
import io.paytouch.core.data.model.{ OnlineOrderSettingsUpdate => OnlineOrderSettingsUpdateModel }

final case class OnlineOrderSettings(defaultEstimatedPrepTimeInMins: Option[Int] = None)

final case class OnlineOrderSettingsUpdate(defaultEstimatedPrepTimeInMins: ResettableInt) {
  def toUpdateModel(): OnlineOrderSettingsUpdateModel =
    OnlineOrderSettingsUpdateModel(
      defaultEstimatedPrepTimeInMins = defaultEstimatedPrepTimeInMins,
    )
}
