package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model.{ CustomerMerchantUpdate, GlobalCustomerUpdate }

final case class CustomerUpsertion(globalCustomer: GlobalCustomerUpdate, customerMerchant: CustomerMerchantUpdate)
