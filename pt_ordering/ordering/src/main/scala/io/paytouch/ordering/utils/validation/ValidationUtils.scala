package io.paytouch.ordering.utils.validation

import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.ordering.errors.{ Error => Err }

import scala.concurrent.Future

object ValidatedData extends ValidationUtils[Err] {

  type ValidatedData[T] = Validation[T]

  implicit class RichBoolean(b: Boolean) {
    def toValidated(err: Err) =
      if (b) success(b)
      else failure(err)
  }

  final implicit class RichEitherError[T, R](private val self: Either[Err, T]) extends AnyVal {
    def asValidatedFuture(f: T => Future[ValidatedData[R]]): Future[ValidatedData[R]] =
      self match {
        case Right(t)     => f(t)
        case Left(errors) => Future.successful(ValidatedData.failure(errors))
      }
  }
}

object ValidatedOptData extends ValidationUtils[Err] {

  type ValidatedOptData[T] = Validation[Option[T]]

  def successOpt[T](t: T): ValidatedOptData[T] = success(Some(t))

  def empty[T]: ValidatedOptData[T] = Valid(None)
}

trait ValidationUtils[EE] extends ValidationCombine[EE] {
  def failure[T](error: EE): Validation[T] =
    Invalid(error).toValidatedNel

  def success[T](t: T): Validation[T] =
    Valid(t)

  def successVoid: Validation[Unit] =
    Valid(())
}
