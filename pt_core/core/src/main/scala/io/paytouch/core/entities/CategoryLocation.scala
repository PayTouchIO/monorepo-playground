package io.paytouch.core.entities

import io.paytouch.core.Availabilities

final case class CategoryLocation(active: Option[Boolean], availabilities: Option[Availabilities])
    extends ItemLocationEntity

final case class CategoryLocationUpdate(active: Option[Boolean], availabilities: Option[Availabilities])
    extends ItemLocationUpdateEntity
