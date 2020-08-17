package io.paytouch.core.clients.urbanairship

import io.paytouch.core.clients.urbanairship.entities._

trait ConfigLoader[A <: TemplateData] {
  def extractProjectId(projectIds: ProjectIds): String
}

object ConfigLoader {
  implicit val loyaltyConfigLoader = new ConfigLoader[TemplateData.LoyaltyTemplateData] {
    def extractProjectId(projectIds: ProjectIds) = projectIds.loyaltyProjectId
  }
  implicit val giftCardConfigLoader = new ConfigLoader[TemplateData.GiftCardTemplateData] {
    def extractProjectId(projectIds: ProjectIds) = projectIds.giftCardProjectId
  }
}
