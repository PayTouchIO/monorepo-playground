package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ GiftCardPassRecord, LoyaltyMembershipRecord }
import io.paytouch.core.entities.enums.PassType
import io.paytouch.core.entities.enums.PassType.{ Android, Ios }
import io.paytouch.core.services.LoyaltyMembershipService

import scala.concurrent._

trait PassLoader {
  type Record
  def updatedPassInstalledAtField(itemId: UUID, orderId: Option[UUID])(implicit daos: Daos): Future[Option[Record]]
  def validate(itemId: UUID)(implicit daos: Daos): Future[Option[Record]]
  def urlForType(passType: PassType, record: Record): Option[String]
}

class PassLoaderFactory(val loyaltyMembershipService: LoyaltyMembershipService) {
  lazy val loyaltyMembershipPassLoader = new PassLoader {
    type Record = LoyaltyMembershipRecord

    def validate(itemId: UUID)(implicit daos: Daos) =
      daos.loyaltyMembershipDao.findById(itemId)

    def updatedPassInstalledAtField(itemId: UUID, orderId: Option[UUID])(implicit daos: Daos): Future[Option[Record]] =
      loyaltyMembershipService.enrolViaCustomer(itemId, orderId)

    def urlForType(passType: PassType, record: Record) =
      passType match {
        case Android => record.androidPassPublicUrl
        case Ios     => record.iosPassPublicUrl
      }
  }

  lazy val giftCardPassLoader = new PassLoader {
    type Record = GiftCardPassRecord

    def validate(itemId: UUID)(implicit daos: Daos) =
      daos.giftCardPassDao.findById(itemId)

    def updatedPassInstalledAtField(itemId: UUID, orderId: Option[UUID])(implicit daos: Daos): Future[Option[Record]] =
      daos.giftCardPassDao.updatedPassInstalledAtField(itemId)

    def urlForType(passType: PassType, record: Record) =
      passType match {
        case Android => record.androidPassPublicUrl
        case Ios     => record.iosPassPublicUrl
      }
  }
}
