package io.paytouch.core.entities.enums

import enumeratum.Enum
import io.paytouch.core.{ ServiceConfigurations => Conf }

sealed trait RegularImageSize extends ImageSize

case object RegularImageSize extends Enum[RegularImageSize] {

  case object Original extends RegularImageSize {
    val size = None
    val cloudinaryFormatString = "f_auto,q_auto"
  }

  case object Thumbnail extends RegularImageSize {
    val size = Some(Conf.Regular.thumbnailImgSize)
    val cloudinaryFormatString = s"f_auto,q_auto,w_${size.get},h_${size.get},c_fill,g_auto"
  }

  case object Small extends RegularImageSize {
    val size = Some(Conf.Regular.smallImgSize)
    val cloudinaryFormatString = s"f_auto,q_auto,w_${size.get},h_${size.get},c_fill,g_auto"
  }

  case object Medium extends RegularImageSize {
    val size = Some(Conf.Regular.mediumImgSize)
    val cloudinaryFormatString = s"f_auto,q_auto,w_${size.get},h_${size.get},c_fill,g_auto"
  }

  case object Large extends RegularImageSize {
    val size = Some(Conf.Regular.largeImgSize)
    val cloudinaryFormatString = s"f_auto,q_auto,w_${size.get},h_${size.get},c_fill,g_auto"
  }

  val values = findValues
}
