package io.paytouch.core.calculations

import java.util.UUID

import org.scalacheck.Gen
import org.scalacheck.rng.Seed

trait LookupIdUtils {
  protected def generateLookupId(id: UUID): String = {
    val gen = Gen.listOfN(10, Gen.numChar).map(_.mkString)
    val seedNumb = id.getLeastSignificantBits + id.getMostSignificantBits
    val optSeqT = gen.apply(Gen.Parameters.default, Seed(seedNumb))
    optSeqT.get
  }
}
