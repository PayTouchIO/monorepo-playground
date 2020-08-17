package io.paytouch.ordering.graphql.location

import io.paytouch.ordering.entities.StoreContext
import io.paytouch.ordering.graphql.SchemaSpec
import io.paytouch.ordering.utils.{ CommonArbitraries, DefaultFixtures }

abstract class LocationSchemaSpec extends SchemaSpec with CommonArbitraries {

  abstract class LocationSchemaSpecContext extends SchemaSpecContext with DefaultFixtures {
    val store = londonStore
    val storeContext = StoreContext.fromRecord(store)
    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore(storeContext)
    val locationId = store.locationId
  }

}
