package io.paytouch.core.entities

trait ItemLocationEntity

trait ItemLocationUpdateEntity

final case class ItemLocation(active: Boolean) extends ItemLocationEntity

final case class ItemLocationUpdate(active: Option[Boolean]) extends ItemLocationUpdateEntity
