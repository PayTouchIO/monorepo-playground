package io.paytouch.core.utils
import java.util.Locale.ENGLISH

object RichString {
  def _pascalize(s: String): String = {
    val lst = s.split("_").toList
    (lst.headOption.map(s => s.substring(0, 1).toUpperCase(ENGLISH) + s.substring(1)).get ::
      lst.tail.map(s => s.substring(0, 1).toUpperCase + s.substring(1))).mkString("")
  }
  implicit class RichString(val s: String) extends AnyVal {
    def pascalize: String = _pascalize(s)

    def underscore: String = {
      val spacesPattern = "[-\\s]".r
      val firstPattern = "([A-Z]+)([A-Z][a-z])".r
      val secondPattern = "([a-z\\d])([A-Z])".r
      val replacementPattern = "$1_$2"
      spacesPattern
        .replaceAllIn(
          secondPattern.replaceAllIn(firstPattern.replaceAllIn(s, replacementPattern), replacementPattern),
          "_",
        )
        .toLowerCase
    }

    def dashcase: String = underscore.replace('_', '-')
  }
}
