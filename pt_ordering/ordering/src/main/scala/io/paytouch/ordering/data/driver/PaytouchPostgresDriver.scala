package io.paytouch.ordering.data.driver

import slick.jdbc.PostgresProfile

trait PaytouchPostgresDriver extends PostgresProfile with PgJsonSupport with PgDateSupport {

  override val api = new API with JsonImplicits with Json4sJsonImplicits with DateImplicits

  val plainApi = new API with PlainImplicits with DatePlainImplicits
}

object PaytouchPostgresDriver extends PaytouchPostgresDriver
