package io.paytouch.core.data.driver

import java.util.UUID

import scala.reflect.ClassTag
import com.github.tminglei.slickpg._
import org.json4s.native.Document
import slick.jdbc.PostgresProfile
import io.paytouch.core.data.model.{
  CustomerNote,
  MerchantNote,
  PaymentProcessorConfig,
  StatusTransition,
  CfdSettings => CfdSettingsModel,
  OnlineOrderSettings => OnlineOrderSettingsModel,
}
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.MerchantSetupSteps
import io.paytouch.core.json.JsonSupport
import io.paytouch.core.validators.RecoveredOrderBundleSet

trait CustomPgJsonSupport extends PgJson4sSupport { self: PostgresProfile =>
  override val pgjson = "jsonb"
  type DOCType = Document
  val jsonMethods = org.json4s.native.JsonMethods

  trait CustomJsonImplicits extends JsonSupport with Json4sJsonImplicits {
    private def jsonMapper[T: ClassTag](implicit m: Manifest[T]) =
      MappedColumnType.base[T, JValue](pds => forDb.fromEntityToJValue(pds), json => forDb.fromJsonToEntity[T](json))

    implicit val customerNotesMapper = jsonMapper[Seq[CustomerNote]]
    implicit val mapOfStringMapper = jsonMapper[Map[String, String]]
    implicit val mapOfMerchantStepsMapper = jsonMapper[Map[MerchantSetupSteps, MerchantSetupStep]]
    implicit val merchantNotesMapper = jsonMapper[Seq[MerchantNote]]
    implicit val paymentProcessorConfigMapper = jsonMapper[PaymentProcessorConfig]
    implicit val paymentDetailsMapper = jsonMapper[PaymentDetails]
    implicit val permissionsMapper = jsonMapper[Permissions]
    implicit val recoveredOrderBundleSetMapper = jsonMapper[Seq[RecoveredOrderBundleSet]]
    implicit val statusTransitionsMapper = jsonMapper[Seq[StatusTransition]]
    implicit val seatingMapper = jsonMapper[Seating]
    implicit val merchantFeaturesMapper = jsonMapper[MerchantFeatures]
    implicit val bigDecimalSeqMapper = jsonMapper[Seq[BigDecimal]]
    implicit val cfdSettingsModelMapper = jsonMapper[CfdSettingsModel]
    implicit val legalDetailsMapper = jsonMapper[LegalDetails]
    implicit val onlineOrderSettingsModelMapper = jsonMapper[OnlineOrderSettingsModel]
  }

  trait CustomJsonPlainImplicits extends Json4sJsonPlainImplicits
}
