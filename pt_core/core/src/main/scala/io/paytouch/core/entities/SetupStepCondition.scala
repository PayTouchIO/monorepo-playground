package io.paytouch.core.entities

trait SetupStepCondition[A] extends (A => Boolean)

object SetupStepCondition {
  implicit val locationCondition: SetupStepCondition[Location] =
    location =>
      location.name.nonEmpty &&
        location.address.line1.isDefined &&
        location.address.city.isDefined &&
        location.address.state.isDefined &&
        location.address.postalCode.isDefined

  implicit val productCondition: SetupStepCondition[Product] =
    _ => true

  implicit val kitchenCondition: SetupStepCondition[Kitchen] =
    _ => true

  implicit val taxRateCondition: SetupStepCondition[TaxRate] =
    _ => true

  implicit val categoryCondition: SetupStepCondition[Category] =
    _ => true
}
