package io.paytouch.ordering.data.driver

import com.github.tminglei.slickpg._
import org.json4s.native.Document
import slick.jdbc.PostgresProfile

trait PgJsonSupport extends PostgresProfile with PgJson4sSupport {

  override val pgjson = "jsonb"
  type DOCType = Document
  val jsonMethods = org.json4s.native.JsonMethods
}
