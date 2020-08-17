package io.paytouch.core.utils

import scala.util.matching.Regex

object RegexUtils {

  // via http://stackoverflow.com/a/14166194
  val singleUuidRe = """[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}""".r
  val channelNameRe = """[-a-zA-Z0-9_=@,.;]+""".r
  val socketIdRe = """\A\d+\.\d+\Z""".r

  implicit class RichRegex(val underlying: Regex) extends AnyVal {
    def matches(s: String) = underlying.pattern.matcher(s).matches
  }

  def isValidUUID(x: String): Boolean = singleUuidRe.matches(x.toLowerCase)
}
