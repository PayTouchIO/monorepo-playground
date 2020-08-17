package io.paytouch.utils

import java.io.File
import java.net.URL

import scala.io.Source

import com.github.tototoshi.csv.CSVWriter

import io.paytouch.core.json.JsonSupport
import io.paytouch.core.utils._

trait FileSupport extends JsonSupport {
  def writeToFile(data: List[String]*): File = {
    val file = File.createTempFile(s"test-${UtcTime.now}", ".csv")
    val writer = CSVWriter.open(file)
    writer.writeAll(data)
    file
  }

  def loadFile(path: String): File =
    new File(url(path).getFile)

  private def url(path: String): URL =
    getClass.getClassLoader.getResource(path)

  def wordsFromFile(path: String): Seq[String] =
    Source
      .fromURL(url(path))
      .getLines()
      .flatMap { line =>
        // removing all punctuation and words with less than 3 letters
        line.toLowerCase.replaceAll("""([\p{Punct}&&[^.]]|\b\p{IsLetter}{1,2}\b)\s*""", "").split("\\W+")
      }
      .filter(_.nonEmpty)
      .toSeq

  def loadFileAsString(path: String): String =
    Source.fromURL(url(path)).getLines().mkString

  def loadAsJsonFromFile[T: Manifest](path: String): T =
    fromJsonStringToEntity[T](loadFileAsString(path))
}
