package io.paytouch.ordering.utils

import java.util.Locale.ENGLISH

object StringHelper extends StringHelper

trait StringHelper {

  implicit def toRichString(s: String): RichString = new RichString(s)

  class RichString(s: String) {
    def pascalize: String = {
      val lst = s.split("_").toList
      (lst.headOption.map(s => s.substring(0, 1).toUpperCase(ENGLISH) + s.substring(1)).get ::
        lst.tail.map(s => s.substring(0, 1).toUpperCase + s.substring(1))).mkString("")
    }

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

    def urlsafe: String = {
      val spacesPattern = "[-_\\s\\+]+".r
      val stripPattern = "[^A-Za-z\\d\\-]".r
      val doubleDashPattern = "-{2,}".r

      doubleDashPattern
        .replaceAllIn(
          stripPattern
            .replaceAllIn(
              spacesPattern.replaceAllIn(s, "-"),
              "",
            ),
          "-",
        )
        .toLowerCase
    }

  }
}
