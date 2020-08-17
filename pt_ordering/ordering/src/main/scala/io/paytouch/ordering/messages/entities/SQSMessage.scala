package io.paytouch.ordering.messages.entities

import java.util.UUID

import io.paytouch.ordering.entities.enums.ExposedName

trait SQSMessage[T] {

  def eventName: String
  def payload: EntityPayloadLike[T]
}

trait EntityPayloadLike[T] {
  def `object`: ExposedName
  def data: T
  def merchantId: UUID
}

trait PtCoreMsg[T] extends SQSMessage[T]

trait PtOrderingMsg[T] extends SQSMessage[T]
