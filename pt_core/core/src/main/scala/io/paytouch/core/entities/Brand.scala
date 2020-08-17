package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class Brand(id: UUID, name: String) extends ExposedEntity {
  val classShortName = ExposedName.Brand
}

final case class BrandCreation(name: String) extends CreationEntity[Brand, BrandUpdate] {
  def asUpdate = BrandUpdate(Some(name))
}

final case class BrandUpdate(name: Option[String]) extends UpdateEntity[Brand]
