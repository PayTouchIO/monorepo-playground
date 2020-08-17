package io.paytouch.core.clients.urbanairship

import org.json4s.JField
import org.json4s.JsonAST._

import io.paytouch.core.clients.urbanairship.entities._
import io.paytouch.core.entities.enums.{ PassType, TemplateType }
import io.paytouch.core.entities.enums.PassType.{ Android, Ios }
import io.paytouch.core.json.JsonSupport.JValue
import io.paytouch.core.utils.JsonTemplateUtils

sealed trait TemplateLoader[A <: TemplateData] extends JsonTemplateUtils {
  final def loadTemplate(passType: PassType, templateData: A): JValue =
    updateTemplateFields(passType, templateData, readTemplate(passType))

  private def readTemplate(passType: PassType): JValue =
    passType match {
      case Ios     => IosTemplate
      case Android => AndroidTemplate
    }

  private lazy val IosTemplate: JValue =
    cacheTemplate(PassType.Ios)

  private lazy val AndroidTemplate: JValue =
    cacheTemplate(PassType.Android)

  private def cacheTemplate(passType: PassType): JValue =
    loadAsJson(s"/urbanairship/${templateType.entryName}_template_${passType.entryName}.json")

  protected def templateType: TemplateType

  protected def updateTemplateFields(
      passType: PassType,
      data: A,
      template: JValue,
    ): JValue
}

object TemplateLoader {
  implicit val loyaltyTemplateLoader: TemplateLoader[TemplateData.LoyaltyTemplateData] =
    new TemplateLoader[TemplateData.LoyaltyTemplateData] {
      override protected val templateType: TemplateType =
        TemplateType.Loyalty

      override protected def updateTemplateFields(
          passType: PassType,
          data: TemplateData.LoyaltyTemplateData,
          template: JValue,
        ): JValue =
        passType match {
          case PassType.Ios =>
            template
              .transformField(writeValueByKeys(List("headers", "logo_text", "value"), data.merchantName))
              .transformField(writeValueByKeys(List("headers", "logo_image", "value"), data.logoImage))
              .transformField(writeValueByKeys(List("fields", "Program Details", "value"), data.details))
              .transformField(writeValueByKeys(List("fields", "Address", "value"), data.address))
              .transformField(writeValueByKeys(List("fields", "Business Name", "value"), data.merchantName))
              .transformField(writeValueByKeys(List("fields", "Phone", "value"), data.phone))
          case PassType.Android =>
            template
              // value is hardcoded (by us) to "Loyalty Card"
              .transformField(writeValueByKeys(List("titleModule", "Business Name", "label"), data.merchantName))
              .transformField(writeValueByKeys(List("titleModule", "image"), data.logoImage))
              .transformField(writeValueByKeys(List("textModulesData", "Program Details", "body"), data.details))
              // value is the displayed text
              .transformField(writeValueByKeys(List("linksModuleData", "Merchant Address", "value"), data.address))
              // uri is the link that will be passed to google maps
              .transformField(
                writeValueByKeys(List("linksModuleData", "Merchant Address", "uri"), googleMaps(data.address)),
              )
              // value is the displayed text
              .transformField(writeValueByKeys(List("linksModuleData", "Merchant Phone", "value"), data.phone))
              // uri is the link that will be passed to the browser
              .transformField(writeValueByKeys(List("linksModuleData", "Merchant Phone", "uri"), data.phone))
              // value is the displayed text
              .transformField(writeValueByKeys(List("linksModuleData", "Merchant Website", "value"), data.website))
              // uri is the phone number which will be passed to the android dialer
              .transformField(writeValueByKeys(List("linksModuleData", "Merchant Website", "uri"), data.website))
        }
    }

  implicit val giftCardTemplateLoader: TemplateLoader[TemplateData.GiftCardTemplateData] =
    new TemplateLoader[TemplateData.GiftCardTemplateData] {
      override protected val templateType: TemplateType =
        TemplateType.GiftCard

      override protected def updateTemplateFields(
          passType: PassType,
          data: TemplateData.GiftCardTemplateData,
          template: JValue,
        ): JValue =
        passType match {
          case PassType.Ios =>
            template
              .transformField(writeValueByKeys(List("headers", "logo_text", "value"), data.merchantName))
              .transformField(writeValueByKeys(List("headers", "logo_image", "value"), data.logoImage))
              .transformField(writeValueByKeys(List("fields", "Our Location:", "value"), data.address))
              .transformField(writeValueByKeys(List("fields", "Gift Card Details", "value"), data.details))
              .transformField(writeValueByKeys(List("fields", "Phone", "value"), data.phone))
              .transformField(writeValueByKeys(List("fields", "Find us online:", "value"), data.website))
              .transformField(
                writeValueByKeys(List("fields", "Last Spend", "value"), data.lastSpend.map(_.roundedAmount)),
              )
              .transformField(
                writeValueByKeys(List("fields", "Original Value", "value"), data.originalBalance.roundedAmount),
              )
              .transformField(writeValueByKeys(List("fields", "Merchant Name", "value"), data.merchantName))
              .transformField(writeValueByKeys(List("fields", "Balance", "value"), data.currentBalance.roundedAmount))
          case PassType.Android =>
            template
              .transformField(writeValueByKeys(List("textModulesData", "Gift Card Details", "body"), data.details))
              .transformField(writeValueByKeys(List("linksModuleData", "Merchant Website", "uri"), data.website))
              .transformField(writeValueByKeys(List("titleModule", "image", "title.string"), data.logoImage))
              .transformField(
                writeValueByKeys(
                  List("titleModule", "Balance", "description.string"),
                  data.currentBalance.roundedAmount.toString,
                ),
              )
              .transformField(writeValueByKeys(List("titleModule", "Balance", "title.string"), data.merchantName))
        }
    }

  private def writeValueByKeys[A](keys: List[String], newValue: Option[A]): PartialFunction[JField, JField] =
    newValue
      .map(a => writeValueByKeys(keys, a))
      .getOrElse {
        case x => identity(x)
      }

  private def writeValueByKeys[A](keys: List[String], newValue: A): PartialFunction[JField, JField] =
    keys match {
      case x :: xs if xs.isEmpty => {
        case (k: String, _: JValue) if k == x =>
          k -> {
            newValue match {
              case v: BigDecimal => JDouble(v.doubleValue)
              case v             => JString(v.toString)
            }
          }
      }

      case x :: xs => {
        case (k: String, values: JValue) if k == x =>
          k -> values.transformField(writeValueByKeys(xs, newValue))
      }

      case _ =>
        throw new IllegalArgumentException("You should call keys with a non empty keys list")
    }

  private def googleMaps(address: Option[String]): Option[String] =
    address.map(a => s"https://maps.google.com?q=$a")
}
