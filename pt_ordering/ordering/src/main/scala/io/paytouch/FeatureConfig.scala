package io.paytouch

final case class FeatureConfig(useStorePaymentTransaction: FeatureConfig.UseStorePaymentTransaction)
object FeatureConfig {
  final case class UseStorePaymentTransaction(value: Boolean) extends Opaque[Boolean]
  object UseStorePaymentTransaction extends OpaqueCompanion[Boolean, UseStorePaymentTransaction]
}
