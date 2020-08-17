package io.paytouch.ordering.utils

import io.paytouch.ordering.data.daos.Daos

import scala.concurrent.ExecutionContext

trait Implicits {

  implicit def ec: ExecutionContext
  implicit def daos: Daos

}
