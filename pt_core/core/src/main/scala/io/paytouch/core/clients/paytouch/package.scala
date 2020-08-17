package io.paytouch.core.clients

import io.paytouch.core.entities.ApiResponse
import io.paytouch.core.errors.ClientError

package object paytouch {

  type PaytouchResponse[T] = Either[ClientError, T]

  type OrderingApiResponse[T] = PaytouchResponse[ApiResponse[T]]
}
