package io.paytouch.core.entities

import io.paytouch.core.entities.enums.ExposedName

trait ExposedEntity {
  def classShortName: ExposedName
}
