package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class OauthCode(code: UUID) extends ExposedEntity {
  val classShortName = ExposedName.OauthCode
}
