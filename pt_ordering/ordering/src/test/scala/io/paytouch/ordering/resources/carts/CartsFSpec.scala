package io.paytouch.ordering.resources.carts

import java.util.UUID

import io.paytouch.ordering.data.model._
import io.paytouch.ordering.entities.enums.{ CartStatus, OrderType, PaymentProcessor }
import io.paytouch.ordering.entities.{ CartUpdate => CartUpdateEntity, _ }
import io.paytouch.ordering.utils.{ CommonArbitraries, FSpec, MultipleLocationFixtures }

abstract class CartsFSpec extends FSpec with CommonArbitraries {
  abstract class CartResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val cartDao = daos.cartDao
    val cartItemDao = daos.cartItemDao
    val cartItemModifierOptionDao = daos.cartItemModifierOptionDao
    val cartItemTaxRateDao = daos.cartItemTaxRateDao
    val cartItemVariantOptionDao = daos.cartItemVariantOptionDao
    val storeDao = daos.storeDao

    val nonEmptyAddress =
      AddressUpsertion(line1 = genString.instance, city = genString.instance, postalCode = genPostalCode.instance)
    val emptyAddress = AddressUpsertion()

    @scala.annotation.nowarn("msg=Auto-application")
    val deliveryAddress =
      random[DeliveryAddressUpsertion]

    val emptyDeliveryAddress = deliveryAddress.copy(address = emptyAddress)
    val nonEmptyDeliveryAddress = deliveryAddress.copy(address = nonEmptyAddress)

    def assertCreation(id: UUID, creation: CartCreation) =
      assertUpsert(id, creation.asUpsert)

    def assertUpdate(id: UUID, update: CartUpdateEntity) =
      assertUpsert(id, update.asUpsert)

    def assertUpsert(id: UUID, upsertion: CartUpsertion) = {
      val record = cartDao.findById(id).await.get

      if (upsertion.storeId.isDefined) upsertion.storeId ==== Some(record.storeId)
      if (upsertion.orderType.isDefined) upsertion.orderType ==== Some(record.orderType)
      if (upsertion.email.isDefined) upsertion.email ==== Some(record.email)
      if (upsertion.phoneNumber.isDefined) record.phoneNumber ==== upsertion.phoneNumber
      if (upsertion.prepareBy.isDefined) record.prepareBy ==== upsertion.prepareBy
      if (upsertion.tipAmount.isDefined) upsertion.tipAmount ==== Some(record.tipAmount)
      if (upsertion.paymentMethodType.isDefined) record.paymentMethodType ==== upsertion.paymentMethodType

      assertDeliveryAddressUpsertion(record, upsertion.deliveryAddress)

      val store = storeDao.findById(record.storeId).await.get
      val cartItems = cartItemDao.findByCartIds(Seq(record.id)).await
      record.orderType match {
        case OrderType.Delivery =>
          if (cartItems.forall(_.isGiftCard))
            record.deliveryFeeAmount ==== None
          else
            record.deliveryFeeAmount ==== store.deliveryFeeAmount

        case OrderType.TakeOut =>
          record.deliveryFeeAmount ==== None
      }
    }

    private def assertDeliveryAddressUpsertion(record: CartRecord, deliveryAddress: DeliveryAddressUpsertion) = {
      if (deliveryAddress.firstName.isDefined) record.firstName ==== deliveryAddress.firstName
      if (deliveryAddress.lastName.isDefined) record.lastName ==== deliveryAddress.lastName

      val origin = record.storeAddress
      val destination = deliveryAddress.address.toAddress

      if (origin.isDefined && destination.isDefined) {
        import io.paytouch.ordering.stubs.GMapsStubData._
        val org = origin.get
        val dest = destination.get
        record.drivingDistanceInMeters ==== Some(retrieveDistanceInMeters(org, dest))
        record.estimatedDrivingTimeInMins ==== Some(retrieveDurationInMins(org, dest))
      }

      assertAddressUpsertion(record, deliveryAddress.address)
    }

    private def assertAddressUpsertion(record: CartRecord, address: AddressUpsertion) = {
      if (address.line1.isDefined) record.deliveryAddressLine1 ==== address.line1
      if (address.line2.isDefined) record.deliveryAddressLine2 ==== address.line2
      if (address.city.isDefined) record.deliveryCity ==== address.city
      if (address.state.isDefined) record.deliveryState ==== address.state
      if (address.country.isDefined) record.deliveryCountry ==== address.country
      if (address.postalCode.isDefined) record.deliveryPostalCode ==== address.postalCode

      if (record.orderType == OrderType.Delivery) assertAddressNotEmpty(record)
    }

    def assertResponseById(id: UUID, entity: Cart) = {
      val record = cartDao.findById(id).await.get
      assertResponse(entity, record)
    }

    def assertResponse(
        entity: Cart,
        record: CartRecord,
        cartTaxRates: Seq[CartTaxRateRecord] = Seq.empty,
        cartItems: Seq[CartItemRecord] = Seq.empty,
        cartItemModifierOptions: Map[CartItemRecord, Seq[CartItemModifierOptionRecord]] = Map.empty,
        cartItemTaxRates: Map[CartItemRecord, Seq[CartItemTaxRateRecord]] = Map.empty,
        cartItemVariantOptions: Map[CartItemRecord, Seq[CartItemVariantOptionRecord]] = Map.empty,
      ) = {
      entity.id ==== record.id
      entity.storeId ==== record.storeId
      entity.orderId ==== record.orderId
      entity.total.amount ==== record.totalAmount
      entity.total.currency ==== record.currency
      entity.subtotal.amount ==== record.subtotalAmount
      entity.subtotal.currency ==== record.currency
      entity.tax.amount ==== record.taxAmount
      entity.tax.currency ==== record.currency
      entity.tip.amount ==== record.tipAmount
      entity.tip.currency ==== record.currency
      entity.phoneNumber ==== record.phoneNumber
      entity.email ==== record.email
      entity.orderType ==== record.orderType
      entity.prepareBy ==== record.prepareBy
      entity.drivingDistanceInMeters ==== record.drivingDistanceInMeters
      entity.estimatedDrivingTimeInMins ==== record.estimatedDrivingTimeInMins
      entity.createdAt ==== record.createdAt
      entity.updatedAt ==== record.updatedAt

      assertDeliveryAddress(entity.deliveryAddress, record)
      assertCartTaxRates(entity.taxRates, cartTaxRates)
      assertCartItems(entity.items, cartItems, cartItemModifierOptions, cartItemTaxRates, cartItemVariantOptions)
    }

    private def assertDeliveryAddress(deliveryAddress: DeliveryAddress, record: CartRecord) = {
      record.firstName ==== deliveryAddress.firstName
      record.lastName ==== deliveryAddress.lastName
      assertAddress(deliveryAddress.address, record)
    }

    private def assertPaymentProcessorData(
        maybePaymentProcessorData: Option[PaymentProcessorData],
        record: CartRecord,
      ) = {
      maybePaymentProcessorData must beSome
      val paymentProcessorData = maybePaymentProcessorData.get
      record.paymentProcessor match {
        case Some(PaymentProcessor.Ekashu) | Some(PaymentProcessor.Jetdirect) =>
          paymentProcessorData.reference ==== Some(record.id.toString)
        case _ => ok
      }
    }

    private def assertAddress(address: Address, record: CartRecord) = {
      address.line1 ==== record.deliveryAddressLine1
      address.line2 ==== record.deliveryAddressLine2
      address.city ==== record.deliveryCity
      address.state ==== record.deliveryState
      address.country ==== record.deliveryCountry
      address.postalCode ==== record.deliveryPostalCode
    }

    private def assertAddressNotEmpty(cart: CartRecord) = {
      cart.deliveryAddressLine1 must beSome
      cart.deliveryCity must beSome
      cart.deliveryPostalCode must beSome
    }

    private def assertCartTaxRates(entities: Seq[CartTaxRate], records: Seq[CartTaxRateRecord]) = {
      entities.length ==== records.length
      records.map(assertCartTaxRate(entities, _))
    }

    private def assertCartTaxRate(entities: Seq[CartTaxRate], record: CartTaxRateRecord) = {
      val maybeEntity = entities.find(_.id == record.id)
      maybeEntity must beSome
      val entity = maybeEntity.get
      entity.id ==== record.id
      entity.taxRateId ==== record.taxRateId
      entity.name ==== record.name
      entity.`value` ==== record.`value`
      entity.total ==== MonetaryAmount(record.totalAmount)
    }

    private def assertCartItems(
        entities: Seq[CartItem],
        records: Seq[CartItemRecord],
        cartItemModifierOptions: Map[CartItemRecord, Seq[CartItemModifierOptionRecord]],
        cartItemTaxRates: Map[CartItemRecord, Seq[CartItemTaxRateRecord]],
        cartItemVariantOptions: Map[CartItemRecord, Seq[CartItemVariantOptionRecord]],
      ) = {
      entities.map(_.id) ==== records.map(_.id)

      records.map { record =>
        val modifierOptions = cartItemModifierOptions.getOrElse(record, Seq.empty)
        val taxRates = cartItemTaxRates.getOrElse(record, Seq.empty)
        val variantOptions = cartItemVariantOptions.getOrElse(record, Seq.empty)

        assertCartItem(
          entities,
          record,
          modifierOptions,
          taxRates,
          variantOptions,
        )
      }
    }

    private def assertCartItem(
        entities: Seq[CartItem],
        record: CartItemRecord,
        cartItemModifierOptions: Seq[CartItemModifierOptionRecord],
        cartItemTaxRates: Seq[CartItemTaxRateRecord],
        cartItemVariantOptions: Seq[CartItemVariantOptionRecord],
      ) = {
      val maybeEntity = entities.find(_.id == record.id)
      maybeEntity must beSome

      val entity = maybeEntity.get
      entity.id ==== record.id
      entity.product.id ==== record.productId
      entity.product.name ==== record.productName
      entity.product.description ==== record.productDescription
      entity.quantity ==== record.quantity
      entity.unit ==== record.unit
      entity.price ==== MonetaryAmount(record.priceAmount)
      entity.tax ==== MonetaryAmount(record.taxAmount)
      entity.calculatedPrice ==== MonetaryAmount(record.calculatedPriceAmount)
      entity.totalPrice ==== MonetaryAmount(record.totalPriceAmount)
      entity.notes ==== record.notes
      entity.`type` ==== record.`type`
      entity.giftCardData ==== record.giftCardData

      assertCartItemModifierOptions(entity.modifierOptions, cartItemModifierOptions)
      assertCartItemTaxRates(entity.taxRates, cartItemTaxRates)
      assertCartItemVariantOptions(entity.variantOptions, cartItemVariantOptions)
    }

    private def assertCartItemModifierOptions(
        entities: Seq[CartItemModifierOption],
        records: Seq[CartItemModifierOptionRecord],
      ) = {
      entities.map(_.id) ==== records.map(_.id)
      records.map(assertCartItemModifierOption(entities, _))
    }

    private def assertCartItemModifierOption(
        entities: Seq[CartItemModifierOption],
        record: CartItemModifierOptionRecord,
      ) = {
      val maybeEntity = entities.find(_.id == record.id)
      maybeEntity must beSome
      val entity = maybeEntity.get
      entity.id ==== record.id
      entity.modifierOptionId ==== record.modifierOptionId
      entity.name ==== record.name
      entity.`type` ==== record.`type`
      entity.price ==== MonetaryAmount(record.priceAmount)
      entity.quantity ==== record.quantity
    }

    private def assertCartItemTaxRates(entities: Seq[CartItemTaxRate], records: Seq[CartItemTaxRateRecord]) = {
      entities.map(_.id) ==== records.map(_.id)
      records.map(assertCartItemTaxRate(entities, _))
    }

    private def assertCartItemTaxRate(entities: Seq[CartItemTaxRate], record: CartItemTaxRateRecord) = {
      val maybeEntity = entities.find(_.id == record.id)
      maybeEntity must beSome
      val entity = maybeEntity.get
      entity.id ==== record.id
      entity.taxRateId ==== record.taxRateId
      entity.name ==== record.name
      entity.`value` ==== record.`value`
      entity.total ==== MonetaryAmount(record.totalAmount)
      entity.applyToPrice ==== record.applyToPrice
    }

    private def assertCartItemVariantOptions(
        entities: Seq[CartItemVariantOption],
        records: Seq[CartItemVariantOptionRecord],
      ) = {
      entities.map(_.id) ==== records.map(_.id)
      records.map(assertCartItemVariantOption(entities, _))
    }

    private def assertCartItemVariantOption(
        entities: Seq[CartItemVariantOption],
        record: CartItemVariantOptionRecord,
      ) = {
      val maybeEntity = entities.find(_.id == record.id)
      maybeEntity must beSome
      val entity = maybeEntity.get
      entity.id ==== record.id
      entity.variantOptionId ==== record.variantOptionId
      entity.optionName ==== record.optionName
      entity.optionTypeName ==== record.optionTypeName
    }

    def assertCartStatus(
        id: UUID,
        status: CartStatus,
        synced: Option[Boolean] = None,
      ) = {
      val record = cartDao.findById(id).await.get

      if (synced.isDefined) record.orderId.isDefined ==== synced.get
      record.status ==== status
    }
  }
}
