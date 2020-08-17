package io.paytouch.core.calculations

import java.util.UUID

import org.scalacheck.Gen
import org.scalacheck.rng.Seed

trait PasswordResetTokenUtils {
  protected def generateToken(seed: UUID): String = {
    val gen = Gen.listOfN(64, Gen.alphaNumChar).map(_.mkString)
    val seedNumb = seed.getLeastSignificantBits + seed.getMostSignificantBits
    val optSeqT = gen.apply(Gen.Parameters.default, Seed(seedNumb))
    optSeqT.get
  }
}
