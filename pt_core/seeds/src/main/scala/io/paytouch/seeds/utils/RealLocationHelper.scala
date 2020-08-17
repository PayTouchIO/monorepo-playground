package io.paytouch.seeds.utils

import java.time.ZoneId

import io.paytouch.core.entities.Address
import io.paytouch.utils.FileSupport

object RealLocationHelper extends FileSupport {
  lazy val locations: Seq[RealLocation] = loadAsJsonFromFile[Seq[RealLocation]]("locations/locations.json")
}

final case class RealLocation(
    name: String,
    address: Address,
    timezone: ZoneId,
  )
