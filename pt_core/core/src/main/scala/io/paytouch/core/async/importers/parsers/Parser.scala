package io.paytouch.core.async.importers.parsers

import java.util.UUID

import cats.data._
import cats.implicits._

import io.paytouch.core.data.model.ImportRecord
import io.paytouch.core.utils.MultipleExtraction.ErrorsOr
import io.paytouch.core.utils.{ Implicits, MultipleExtraction }

import scala.concurrent._

abstract class Parser[T] extends Implicits {

  type ImportResult <: Product

  val importDao = daos.importDao

  def parse(importId: UUID): Future[ErrorsOr[(ImportRecord, ImportResult, T)]] =
    importDao.findById(importId).flatMap {
      case Some(importer) => parse(importer).mapNested { case (result, data) => (importer, result, data) }
      case None =>
        Future.successful(MultipleExtraction.failure(ValidationError(None, s"Import with id $importId not found")))
    }

  protected def parse(importer: ImportRecord): Future[ErrorsOr[(ImportResult, T)]]
}

final case class ValidationError(line: Option[Int], message: String)

final case class ValidationResult(errors: Seq[ValidationError])

object ValidationResult {

  def apply(msg: String): ValidationResult = ValidationResult(Seq(ValidationError(None, msg)))

  def apply(nelErrors: NonEmptyList[ValidationError]): ValidationResult = {
    val list = nelErrors.head +: nelErrors.tail
    ValidationResult(list.sortBy(_.line))
  }
}
