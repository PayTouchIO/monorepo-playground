package io.paytouch.utils

object BaconIpsumHelper extends FileSupport {
  lazy val words: Seq[String] = wordsFromFile("bacon-ipsum/dictionary.txt")
}
