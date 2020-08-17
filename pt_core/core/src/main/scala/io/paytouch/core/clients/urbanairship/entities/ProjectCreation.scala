package io.paytouch.core.clients.urbanairship.entities

import io.paytouch.core.clients.urbanairship.entities.enums.ProjectType

final case class ProjectCreation(
    name: String,
    projectType: ProjectType,
    description: String,
    settings: Option[ProjectSettingsCreation],
  )

final case class ProjectSettingsCreation(
    barcode_alt_text: String,
    barcode_label: String,
    barcode_default_value: String,
    barcode_encoding: String,
    barcode_type: String,
  )
