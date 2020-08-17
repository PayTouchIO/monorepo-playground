package io.paytouch.core.resources.sessions

import io.paytouch.core.data.model.SessionRecord
import io.paytouch.core.entities._
import io.paytouch.core.utils._

abstract class SessionsFSpec extends FSpec {

  abstract class SessionResourceFSpecContext extends FSpecContext with DefaultFixtures {
    def assertResponse(entity: Session, record: SessionRecord) = {
      entity.id ==== record.id
      entity.source ==== record.source
      entity.createdAt ==== record.createdAt
      entity.updatedAt ==== record.updatedAt
    }
  }
}
