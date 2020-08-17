package io.paytouch.core.resources.events

import io.paytouch.core.data.model.EventRecord
import io.paytouch.core.entities._
import io.paytouch.core.utils._

abstract class EventsFSpec extends FSpec {

  abstract class BrandResourceFSpecContext extends FSpecContext with DefaultFixtures {
    val brandDao = daos.brandDao

    def assertResponse(entity: Event, record: EventRecord) = {
      entity.id ==== record.id
      entity.action ==== record.action
      entity.`object` ==== record.`object`
      entity.data ==== record.data
      entity.receivedAt ==== record.receivedAt
    }
  }
}
