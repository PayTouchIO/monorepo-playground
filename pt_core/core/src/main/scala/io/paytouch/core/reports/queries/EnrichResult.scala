package io.paytouch.core.reports.queries

import io.paytouch.core.reports.entities.SingleReportData

import scala.concurrent._

trait EnrichResult[T] extends (Seq[SingleReportData[T]] => Future[Seq[SingleReportData[T]]])

object EnrichResult {
  def apply[T](f: Seq[SingleReportData[T]] => Future[Seq[SingleReportData[T]]]): EnrichResult[T] =
    new EnrichResult[T] {
      def apply(data: Seq[SingleReportData[T]]) = f(data)
    }

  def identity[T] = apply[T](r => Future.successful(r))
}
