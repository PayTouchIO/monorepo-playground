package io.paytouch.core.resources.imageuploads

import io.paytouch.core.utils.{ DefaultFixtures, FSpec }
import io.paytouch.utils.FileSupport

abstract class ImageUploadFSpec extends FSpec {
  abstract class ImageUploadFormResourceFSpecContext extends FSpecContext with DefaultFixtures {
    val imageUploadDao = daos.imageUploadDao

    lazy val ValidImage = Images.ValidImage
    lazy val InvalidImage = Images.InvalidImage
  }

  abstract class ImageUploadFSpecContext extends FSpecContext with DefaultFixtures {
    val imageUploadDao = daos.imageUploadDao
  }
}

object Images extends FileSupport {
  lazy val ValidImage = loadFile("images/lena.jpeg")
  lazy val InvalidImage = loadFile("images/non-an-image.csv")
}
