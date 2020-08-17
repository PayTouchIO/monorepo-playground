package io.paytouch.utils

import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.directives.BasicDirectives
import io.paytouch.logging.ResponseTimeout
import io.paytouch.utils.Tagging.withTag

import scala.concurrent.duration.FiniteDuration

trait StrictEntitiesDirectories {

  def responseTimeout: FiniteDuration withTag ResponseTimeout
  def toStrict: Directive0 = BasicDirectives.toStrictEntity(responseTimeout)

}
