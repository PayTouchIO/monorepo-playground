package io.paytouch.core.clients.urbanairship.entities

import java.time.ZonedDateTime

import io.paytouch.core.clients.urbanairship.entities.enums.ProjectType

final case class Project(
    id: String,
    projectType: ProjectType,
    name: String,
    description: String,
    templates: Seq[String], // TODO - add template representation
    settings: ProjectSettings,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  )

final case class ProjectSettings(
    id: Option[String],
    barcodeAltText: String,
    barcodeLabel: String,
    barcodeDefaultValue: String,
    barcodeEncoding: String,
    barcodeType: String,
    passbookCount: Option[String],
    googleCount: Option[String],
  )
