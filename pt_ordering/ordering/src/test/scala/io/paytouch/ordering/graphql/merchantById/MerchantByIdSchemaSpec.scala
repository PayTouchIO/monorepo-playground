package io.paytouch.ordering.graphql.merchantById

import io.paytouch.ordering.graphql.SchemaSpec
import io.paytouch.ordering.utils.{ CommonArbitraries, DefaultFixtures, MockedRestApi }

abstract class MerchantByIdSchemaSpec extends SchemaSpec with CommonArbitraries {

  abstract class MerchantByIdSchemaSpecContext extends SchemaSpecContext with DefaultFixtures {
    val merchantService = MockedRestApi.merchantService
  }

}
