package io.paytouch.ordering.services

import io.paytouch.ordering.entities.enums._
import io.paytouch.ordering.entities.PaymentMethod

object MergeValidPaymentMethodsWithActiveOnes {
  def apply(currentMethods: Seq[PaymentMethod], paymentProcessor: PaymentProcessor): Seq[PaymentMethod] =
    apply(currentMethods, getValidMethodTypes(paymentProcessor))

  def apply(currentMethods: Seq[PaymentMethod], validMethodTypes: Set[PaymentMethodType]): Seq[PaymentMethod] =
    currentMethods
      .foldLeft(toInactiveMethods(validMethodTypes).to(Seq)) { (acc, current) =>
        if (current.inactive)
          acc
        else
          acc.map { valid =>
            if (valid.`type` == current.`type`)
              valid.copy(active = true)
            else
              valid
          }
      }
      .sortBy { method =>
        // Booleans are sorted false first (but we want true first)
        // therefore it is flipped here by using `inactive`
        method.inactive -> method.`type`
      }

  private[this] def toInactiveMethods(methodTypes: Set[PaymentMethodType]): Set[PaymentMethod] =
    methodTypes.map(PaymentMethod(_, active = false))

  def getValidMethodTypes(paymentProcessor: PaymentProcessor): Set[PaymentMethodType] =
    Set(toMethodType(paymentProcessor), PaymentMethodType.Cash)

  def toMethodType(paymentProcessor: PaymentProcessor): PaymentMethodType =
    paymentProcessor match {
      case PaymentProcessor.Ekashu    => PaymentMethodType.Ekashu
      case PaymentProcessor.Jetdirect => PaymentMethodType.Jetdirect
      case PaymentProcessor.Worldpay  => PaymentMethodType.Worldpay
      case PaymentProcessor.Stripe    => PaymentMethodType.Stripe
      case PaymentProcessor.Paytouch  => PaymentMethodType.Cash
    }
}
