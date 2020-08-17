package io.paytouch.ordering.graphql.merchant

import io.paytouch.ordering.graphql.SchemaSpec
import io.paytouch.ordering.utils._

abstract class MerchantSchemaSpec extends SchemaSpec with CommonArbitraries {
  abstract class MerchantSchemaSpecContext extends SchemaSpecContext with DefaultFixtures {
    val merchantService = MockedRestApi.merchantService
  }
}
