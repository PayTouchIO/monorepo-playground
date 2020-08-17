package io.paytouch.utils

import akka.http.scaladsl.model.HttpEntity
import akka.stream.Materializer
import io.paytouch.logging.ResponseTimeout
import io.paytouch.utils.Tagging.withTag

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future }

trait StrictEntitiesHelper {
  def responseTimeout: FiniteDuration withTag ResponseTimeout

  protected def extractStrictEntity(
      entity: HttpEntity,
    )(implicit
      ec: ExecutionContext,
      fm: Materializer,
    ): Future[HttpEntity.Strict] =
    entity match {
      case strict: HttpEntity.Strict => Future.successful(strict)
      case nonStrict                 => nonStrict.withoutSizeLimit.toStrict(responseTimeout)
    }

  protected def extractBody(entity: HttpEntity)(implicit ec: ExecutionContext, fm: Materializer): Future[String] =
    extractStrictEntity(entity).map(_.data.utf8String)

}
