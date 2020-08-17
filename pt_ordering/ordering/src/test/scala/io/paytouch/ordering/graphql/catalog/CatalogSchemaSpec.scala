package io.paytouch.ordering.graphql.catalog

import java.util.UUID

import io.paytouch.ordering.entities.StoreContext
import io.paytouch.ordering.graphql.SchemaSpec
import io.paytouch.ordering.utils._

abstract class CatalogSchemaSpec extends SchemaSpec with CommonArbitraries {
  abstract class CatalogSchemaSpecContext extends SchemaSpecContext with DefaultFixtures {
    val store = londonStore
    val catalogId = londonCatalogId
    val storeContext = StoreContext.fromRecord(store)
    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore(storeContext)
  }
}
