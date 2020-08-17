package io.paytouch.ordering.entities

trait CreationEntity[T] {
  def asUpsert: T
}

trait UpdateEntity[T] {
  def asUpsert: T
}
