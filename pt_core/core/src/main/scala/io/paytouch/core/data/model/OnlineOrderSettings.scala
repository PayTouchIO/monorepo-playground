package io.paytouch.core.data.model

import io.paytouch.core.entities.{ ResettableInt, OnlineOrderSettings => OnlineOrderSettingsEntity }

case class OnlineOrderSettings(defaultEstimatedPrepTimeInMins: ResettableInt = None) {
  def toEntity =
    OnlineOrderSettingsEntity(
      defaultEstimatedPrepTimeInMins = defaultEstimatedPrepTimeInMins.toOption,
    )

  def toUpdate =
    OnlineOrderSettingsUpdate(
      defaultEstimatedPrepTimeInMins = defaultEstimatedPrepTimeInMins,
    )
}

case class OnlineOrderSettingsUpdate(defaultEstimatedPrepTimeInMins: ResettableInt) {
  def toRecord: OnlineOrderSettings = updateRecord(None)

  def updateRecord(maybeExisting: Option[OnlineOrderSettings]): OnlineOrderSettings = {
    val existing = maybeExisting.getOrElse(OnlineOrderSettings())
    OnlineOrderSettings(
      defaultEstimatedPrepTimeInMins = defaultEstimatedPrepTimeInMins.getOrElse(existing.defaultEstimatedPrepTimeInMins),
    )
  }
}
