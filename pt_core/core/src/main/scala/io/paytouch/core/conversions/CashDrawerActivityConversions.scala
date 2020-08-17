package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.CashDrawerActivityRecord
import io.paytouch.core.entities.{
  MonetaryAmount,
  UserContext,
  UserInfo,
  CashDrawerActivity => CashDrawerActivityEntity,
}

trait CashDrawerActivityConversions {

  def fromRecordsAndOptionsToEntities(
      records: Seq[CashDrawerActivityRecord],
      userInfoByUserId: Option[Map[UUID, UserInfo]],
    )(implicit
      user: UserContext,
    ): Seq[CashDrawerActivityEntity] =
    records.map { record =>
      val userInfo = record.userId.flatMap(uId => userInfoByUserId.flatMap(_.get(uId)))
      fromRecordAndOptionsToEntity(record, userInfo)
    }

  def fromRecordAndOptionsToEntity(
      record: CashDrawerActivityRecord,
      userInfo: Option[UserInfo],
    )(implicit
      user: UserContext,
    ): CashDrawerActivityEntity =
    CashDrawerActivityEntity(
      id = record.id,
      cashDrawerId = record.cashDrawerId,
      userId = record.userId,
      orderId = record.orderId,
      user = userInfo,
      `type` = record.`type`,
      startingCash = record.startingCashAmount.map(amount => MonetaryAmount(amount)),
      endingCash = record.endingCashAmount.map(amount => MonetaryAmount(amount)),
      payIn = record.payInAmount.map(amount => MonetaryAmount(amount)),
      payOut = record.payOutAmount.map(amount => MonetaryAmount(amount)),
      tipIn = record.tipInAmount.map(amount => MonetaryAmount(amount)),
      tipOut = record.tipOutAmount.map(amount => MonetaryAmount(amount)),
      currentBalance = MonetaryAmount(record.currentBalanceAmount),
      tipForUserId = record.tipForUserId,
      timestamp = record.timestamp,
      notes = record.notes,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )
}
