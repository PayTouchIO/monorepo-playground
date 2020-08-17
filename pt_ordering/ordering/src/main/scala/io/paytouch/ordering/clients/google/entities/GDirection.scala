package io.paytouch.ordering.clients.google.entities

final case class GDistanceMatrix(rows: Seq[GRow]) {

  def best: Option[GElement] = GElement.pickBest(rows.flatMap(_.elements))
}

object GDistanceMatrix {

  def apply(
      distance: GDistance,
      duration: GDuration,
      status: GStatus = GStatus.Ok,
    ): GDistanceMatrix = {
    val element = GElement(distance, duration, status)
    val row = GRow(Seq(element))
    apply(Seq(row))
  }
}

final case class GRow(elements: Seq[GElement] = Seq.empty)

final case class GElement(
    distance: GDistance,
    duration: GDuration,
    status: GStatus,
  )

object GElement {

  def pickBest(legs: Seq[GElement]): Option[GElement] =
    legs.filter(_.status == GStatus.Ok).sortBy(_.duration.value).headOption

}

// value is distance in meters
final case class GDistance(text: String = "Unknown", value: Long = -1)

// value is time in seconds
final case class GDuration(text: String = "Unknown", value: Long = -1)
