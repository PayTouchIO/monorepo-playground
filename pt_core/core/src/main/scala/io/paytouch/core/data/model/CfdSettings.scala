package io.paytouch.core.data.model

import io.paytouch.core.entities.{ ResettableString, CfdSettings => CfdSettingsEntity }

case class CfdSettings(showCustomText: Boolean = false, customText: Option[String] = None) {
  def toEntity =
    CfdSettingsEntity(
      showCustomText = showCustomText,
      customText = customText,
    )

  def toUpdate =
    CfdSettingsUpdate(
      showCustomText = Some(showCustomText),
      customText = Some(customText),
    )
}

case class CfdSettingsUpdate(showCustomText: Option[Boolean], customText: ResettableString) {
  def toRecord: CfdSettings = updateRecord(None)

  def updateRecord(maybeExisting: Option[CfdSettings]): CfdSettings = {
    val existing = maybeExisting.getOrElse(CfdSettings())
    CfdSettings(
      showCustomText = showCustomText.getOrElse(existing.showCustomText),
      customText = customText.getOrElse(existing.customText),
    )
  }
}
