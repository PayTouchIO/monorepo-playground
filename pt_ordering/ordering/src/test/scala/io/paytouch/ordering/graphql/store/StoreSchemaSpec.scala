package io.paytouch.ordering.graphql.store

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.Category
import io.paytouch.ordering.graphql.SchemaSpec
import io.paytouch.ordering.utils._

abstract class StoreSchemaSpec extends SchemaSpec with CommonArbitraries {
  abstract class StoreSchemaSpecContext extends SchemaSpecContext with DefaultFixtures {
    lazy val store = londonStore
    val catalogId = londonCatalogId
    val storeService = MockedRestApi.storeService

    @scala.annotation.nowarn("msg=Auto-application")
    lazy val expectedCategory =
      random[Category].copy(catalogId = londonCatalogId)
  }
}
