package io.paytouch.ordering.entities

import io.paytouch.ordering.entities.enums.ExposedName

trait ExposedEntity {
  def classShortName: ExposedName
}
