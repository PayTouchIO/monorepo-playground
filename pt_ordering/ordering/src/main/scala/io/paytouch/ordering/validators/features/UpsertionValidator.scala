package io.paytouch.ordering.validators.features

import java.util.UUID

import io.paytouch.ordering.clients.PaytouchResponse
import io.paytouch.ordering.entities.AppContext
import io.paytouch.ordering.utils.validation.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData

import scala.concurrent.Future

trait UpsertionValidator {

  type Context <: AppContext
  type Record
  type Upsertion

  def validateUpsertion(
      id: UUID,
      upsertion: Upsertion,
      existing: Option[Record],
    )(implicit
      context: Context,
    ): Future[ValidatedData[Upsertion]]

  implicit def toRichCoreApiResponse[T](response: PaytouchResponse[T]): RichPaytouchResponse[T] =
    new RichPaytouchResponse(response)

  class RichPaytouchResponse[T](response: PaytouchResponse[T]) {

    def asValidatedData: ValidatedData[T] =
      response match {
        case Right(t)     => ValidatedData.success(t)
        case Left(errors) => ValidatedData.failure(errors)
      }
  }
}
