package io.paytouch.core.utils

import io.paytouch.core.data.daos.Daos

import scala.concurrent.ExecutionContext

trait Implicits {
  implicit def ec: ExecutionContext
  implicit def daos: Daos
}
