package io.paytouch.ordering.services

import io.paytouch.ordering.entities.enums._
import io.paytouch.ordering.entities.PaymentMethod
import io.paytouch.ordering.utils.PaytouchSuite

class MergeValidPaymentMethodsWithActiveOnesSuite extends PaytouchSuite {
  import org.scalacheck.ScalacheckShapeless._

  "unit tests" should {
    "apply" should {
      "return empty if validMethods is empty" in {
        prop { arbitraryMethods: Seq[PaymentMethod] =>
          val actual: Seq[PaymentMethod] =
            MergeValidPaymentMethodsWithActiveOnes(
              currentMethods = arbitraryMethods,
              validMethodTypes = Set.empty,
            )

          actual == Seq.empty
        }
      }

      "return validMethods with all methods being inactive if currentMethods was empty" in {
        prop { arbitraryValidMethodTypes: Set[PaymentMethodType] =>
          val actual: Seq[PaymentMethod] =
            MergeValidPaymentMethodsWithActiveOnes(
              currentMethods = Seq.empty,
              validMethodTypes = arbitraryValidMethodTypes,
            )

          val expected: Seq[PaymentMethod] =
            arbitraryValidMethodTypes.map(PaymentMethod(_, active = false)).to(Seq)

          actual must containTheSameElementsAs(expected)
        }
      }

      "return only methods defined in validMethodTypes marked as active (assuming they were active in the first place)" in {
        prop { (arbitraryMethods: Seq[PaymentMethod], arbitraryValidMethodTypes: Set[PaymentMethodType]) =>
          val actual: Seq[PaymentMethod] =
            MergeValidPaymentMethodsWithActiveOnes(
              currentMethods = arbitraryMethods,
              validMethodTypes = arbitraryValidMethodTypes,
            )

          val onlyMethodsDefinedInValidMethodTypes =
            actual.map(_.`type`) must containTheSameElementsAs(arbitraryValidMethodTypes.to(Seq))

          val markedAsActiveAssumingTheyWereActiveInTheFirstPlace =
            actual.forall { method =>
              val isValid =
                arbitraryValidMethodTypes.contains(method.`type`)

              val isCurrentlyActive =
                arbitraryMethods.exists(current => current.active && current.`type` == method.`type`)

              val actual = method.active
              val expected = isValid && isCurrentlyActive

              actual == expected
            }

          onlyMethodsDefinedInValidMethodTypes && markedAsActiveAssumingTheyWereActiveInTheFirstPlace
        }
      }

      "return only methods defined in validMethodTypes marked as active (assuming they were active in the first place) example" in {
        val actual: Seq[PaymentMethod] =
          MergeValidPaymentMethodsWithActiveOnes(
            currentMethods = Seq(
              PaymentMethod(PaymentMethodType.Cash, active = true),
              PaymentMethod(PaymentMethodType.Ekashu, active = true),
            ),
            validMethodTypes = Set(
              PaymentMethodType.Cash,
              PaymentMethodType.Worldpay,
            ),
          )

        val expected: Seq[PaymentMethod] =
          Seq(
            PaymentMethod(PaymentMethodType.Cash, active = true),
            PaymentMethod(PaymentMethodType.Worldpay, active = false),
          )

        actual must containTheSameElementsAs(expected)
      }

      "return methods grouped (active first) and ordered within groups (cash first, everything else alphabetically)" in {
        val actual: Seq[PaymentMethod] =
          MergeValidPaymentMethodsWithActiveOnes(
            currentMethods = Seq(
              PaymentMethod(PaymentMethodType.Worldpay, active = true),
              PaymentMethod(PaymentMethodType.Ekashu, active = true),
            ),
            validMethodTypes = scala.util.Random.shuffle(PaymentMethodType.values).to(Set),
          )

        val expected: Seq[PaymentMethodType] =
          Seq(
            PaymentMethodType.Ekashu, // active
            PaymentMethodType.Worldpay, // active
            PaymentMethodType.Cash, // inactive (cash is always first in it's group)
            PaymentMethodType.Jetdirect, // inactive
            PaymentMethodType.Stripe, // inactive
          )

        actual.map(_.`type`) === expected
      }
    }

    "getValidMethodTypes" should {
      "always contain at least Cash" in {
        prop { (arbitraryPaymentProcessor: PaymentProcessor) =>
          val actual: Set[PaymentMethodType] =
            MergeValidPaymentMethodsWithActiveOnes.getValidMethodTypes(arbitraryPaymentProcessor)

          actual.contains(PaymentMethodType.Cash)
        }
      }
    }
  }
}
