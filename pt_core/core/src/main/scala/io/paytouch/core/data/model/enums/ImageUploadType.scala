package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.entities.enums.{ ImageSize, RegularImageSize }
import io.paytouch.core.utils.EnumEntrySnake

sealed abstract class ImageUploadType(val bucketName: String, val sizes: Seq[ImageSize]) extends EnumEntrySnake

case object ImageUploadType extends Enum[ImageUploadType] {

  case object Category extends ImageUploadType("category_images", RegularImageSize.values)
  case object Product extends ImageUploadType("product_images", RegularImageSize.values)
  case object EmailReceipt extends ImageUploadType("email_receipt_images", RegularImageSize.values)
  case object PrintReceipt extends ImageUploadType("print_receipt_images", RegularImageSize.values)
  case object User extends ImageUploadType("user_images", RegularImageSize.values)
  case object GiftCard extends ImageUploadType("gift_card_images", RegularImageSize.values)
  case object LoyaltyProgram extends ImageUploadType("loyalty_program_images", RegularImageSize.values)
  case object StoreHero extends ImageUploadType("store_hero_images", RegularImageSize.values)
  case object StoreLogo extends ImageUploadType("store_logo_images", RegularImageSize.values)
  case object CfdSplashScreen extends ImageUploadType("cfd_splash_screen", RegularImageSize.values)

  val values = findValues
}
