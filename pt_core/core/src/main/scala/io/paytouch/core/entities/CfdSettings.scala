package io.paytouch.core.entities

import java.util.UUID
import io.paytouch.core.data.model.{ CfdSettingsUpdate => CfdSettingsUpdateModel }

final case class CfdSettings(
    showCustomText: Boolean = false,
    customText: Option[String] = None,
    splashImageUrls: Seq[ImageUrls] = Seq.empty,
  )

final case class CfdSettingsUpdate(
    splashImageUploadIds: Option[Seq[UUID]] = None,
    showCustomText: Option[Boolean] = None,
    customText: ResettableString = None,
  ) {
  def toUpdateModel(): CfdSettingsUpdateModel =
    CfdSettingsUpdateModel(
      showCustomText = showCustomText,
      customText = customText,
    )
}
