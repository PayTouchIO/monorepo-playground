package io.paytouch.ordering.utils

import io.paytouch._

sealed abstract class ResultType extends SerializableProduct
object ResultType {
  case object Created extends ResultType
  case object Updated extends ResultType
}
