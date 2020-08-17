package io.paytouch.core.services.ordertransitions

object Errors {
  abstract class Error { def message: String }
  type Errors = Seq[Error]
  final case class ErrorsAndResult[A](errors: Errors, data: A)

  object ErrorsAndResult {
    def noErrors[A](data: A) = ErrorsAndResult(Seq.empty, data)
  }
}
