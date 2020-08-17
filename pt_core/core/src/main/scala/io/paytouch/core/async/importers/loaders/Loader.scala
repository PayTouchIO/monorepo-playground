package io.paytouch.core.async.importers.loaders

import com.typesafe.scalalogging.LazyLogging
import io.paytouch.core.data.model.ImportRecord
import io.paytouch.core.utils.Implicits

import scala.concurrent._

abstract class Loader[T] extends LazyLogging with Implicits {

  def load(importer: ImportRecord, data: T): Future[Unit]
}
