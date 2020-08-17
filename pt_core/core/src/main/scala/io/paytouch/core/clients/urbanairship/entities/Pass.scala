package io.paytouch.core.clients.urbanairship.entities

sealed trait PassResponse

final case class Pass(
    fields: Map[String, Any],
    serialNumber: String,
    publicUrl: PassPublicUrl,
    externalId: String,
    id: String,
    templateId: String,
    url: String,
    createdAt: String,
    updatedAt: String,
    tags: Seq[Any],
    status: String,
  ) extends PassResponse

final case class PassPublicUrl(
    path: String,
    image: Option[String],
    `type`: String,
  )

final case class PassUpsertion(
    headers: Map[String, FieldValueUpdate] = Map.empty,
    fields: Map[String, FieldValueUpdate] = Map.empty,
    beacons: Seq[BeaconValue] = Seq.empty,
    publicUrl: PublicUrlUpsertion = PublicUrlUpsertion(),
  )

final case class PublicUrlUpsertion(`type`: String = "single")

final case class PassUpdateResponse(ticketId: String) extends PassResponse
