package io.paytouch.ordering

import io.paytouch.ordering.entities.ApiResponse
import io.paytouch.ordering.errors.ClientError
import io.paytouch.ordering.utils.validation.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData

package object clients {
  type CoreApiResponse[T] = PaytouchResponse[ApiResponse[T]]
  type PaytouchResponse[T] = Either[ClientError, T]

  final implicit class RichPaytouchResponse[T](private val self: PaytouchResponse[T]) extends AnyVal {
    def asValidatedData: ValidatedData[T] =
      self match {
        case Right(t)     => ValidatedData.success(t)
        case Left(errors) => ValidatedData.failure(errors)
      }
  }

  final implicit class RichCoreApiResponse[T](private val self: CoreApiResponse[T]) extends AnyVal {
    def asOption: Option[T] =
      self.map(_.data).toOption

    def asValidatedData: ValidatedData[T] =
      self match {
        case Right(t)     => ValidatedData.success(t.data)
        case Left(errors) => ValidatedData.failure(errors)
      }
  }
}
