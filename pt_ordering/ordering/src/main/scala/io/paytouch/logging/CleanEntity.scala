package io.paytouch.logging

import akka.http.scaladsl.model.HttpEntity
import akka.stream.Materializer
import io.paytouch.utils.StrictEntitiesHelper

import scala.concurrent.{ ExecutionContext, Future }

case class CleanEntity private (body: String)

object CleanEntity {
  def apply(entity: HttpEntity.Strict): CleanEntity =
    CleanEntity(cleanBody(entity))

  private def cleanBody(entity: HttpEntity.Strict): String =
    entity.data.utf8String.replaceAll("\n|\t", "").replaceAll("\\s\\s+", " ").hideField("password").hideField("pin")

  implicit private class RichString(s: String) {
    def hideField(field: String) = {
      val regex = "\"" + field + "\"\\s*:\\s*\".*?\""
      val replacement = "\"" + field + "\":\"*****\""
      s.replaceAll(regex, replacement)
    }
  }
}
