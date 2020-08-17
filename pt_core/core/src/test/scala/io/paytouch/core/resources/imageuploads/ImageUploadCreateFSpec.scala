package io.paytouch.core.resources.imageuploads

class ImageUploadCreateFSpec extends GenericImageUploadCreateFSpec {

  val regularSizeFileName = Map(
    "original" -> "original",
    "thumbnail" -> "200x200",
    "small" -> "400x400",
    "medium" -> "800x800",
    "large" -> "1200x1200",
  )

  assertImageUploadCreation("category", "category_images", regularSizeFileName)

  assertImageUploadCreation("product", "product_images", regularSizeFileName)

  assertImageUploadCreation("email_receipt", "email_receipt_images", regularSizeFileName)

  assertImageUploadCreation("print_receipt", "print_receipt_images", regularSizeFileName)

  assertImageUploadCreation("user", "user_images", regularSizeFileName)

  assertImageUploadCreation("gift_card", "gift_card_images", regularSizeFileName)
}
