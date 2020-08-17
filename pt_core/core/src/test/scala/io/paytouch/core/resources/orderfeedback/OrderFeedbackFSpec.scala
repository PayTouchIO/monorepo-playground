package io.paytouch.core.resources.orderfeedback

import io.paytouch.core.data.model.{ CustomerMerchantRecord, OrderFeedbackRecord }
import io.paytouch.core.entities.{ CustomerMerchant, OrderFeedback => OrderFeedbackEntity }
import io.paytouch.core.json.JsonSupport
import io.paytouch.core.utils._

abstract class OrderFeedbackFSpec extends FSpec {
  abstract class OrderFeedbackResourceFSpecContext extends FSpecContext with JsonSupport with MultipleLocationFixtures {
    val orderFeedbackDao = daos.orderFeedbackDao

    def assertResponse(
        record: OrderFeedbackRecord,
        entity: OrderFeedbackEntity,
        customer: Option[CustomerMerchantRecord] = None,
      ) = {
      record.id ==== entity.id
      record.orderId ==== entity.orderId
      record.customerId ==== entity.customerId
      record.rating ==== entity.rating
      record.body ==== entity.body
      record.read ==== entity.read
      record.createdAt ==== entity.createdAt
      record.updatedAt ==== entity.updatedAt

      customer.foreach(assertCustomerResponse(_, entity.customer))

    }

    private def assertCustomerResponse(record: CustomerMerchantRecord, entity: Option[CustomerMerchant]) = {
      val customer = entity.get
      record.customerId ==== customer.id
      record.firstName ==== customer.firstName
      record.lastName ==== customer.lastName
      record.dob ==== customer.dob
      record.anniversary ==== customer.anniversary
      record.email ==== customer.email
      record.phoneNumber ==== customer.phoneNumber
      record.addressLine1 ==== customer.address.line1
      record.addressLine2 ==== customer.address.line2
      record.city ==== customer.address.city
      record.state ==== customer.address.state
      record.country ==== customer.address.country
      record.postalCode ==== customer.address.postalCode
      record.createdAt ==== customer.createdAt
      record.updatedAt ==== customer.updatedAt
    }
  }
}
