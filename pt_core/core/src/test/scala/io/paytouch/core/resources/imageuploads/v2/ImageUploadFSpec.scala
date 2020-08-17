package io.paytouch.core.resources.imageuploads.v2

import io.paytouch.core.utils.{ DefaultFixtures, FSpec }

abstract class ImageUploadFSpec extends FSpec {
  abstract class ImageUploadFSpecContext extends FSpecContext with DefaultFixtures {
    val imageUploadDao = daos.imageUploadDao
  }
}
