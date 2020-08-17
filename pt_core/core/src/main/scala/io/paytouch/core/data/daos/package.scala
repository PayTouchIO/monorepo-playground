package io.paytouch.core.data

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions._
import io.paytouch.core.data.model._
import io.paytouch.core.data.queries._
import io.paytouch.core.data.tables._

package object daos {
  implicit def toItemIdQuery[R <: SlickRecord, T <: SlickTable[R] with ItemIdColumn](
      q: Query[T, T#TableElementType, Seq],
    ): ItemIdQuery[R, T] =
    new ItemIdQuery[R, T](q)

  implicit def toLocationIdQuery[R <: SlickRecord, T <: SlickTable[R] with LocationIdColumn](
      q: Query[T, T#TableElementType, Seq],
    )(implicit
      locationDao: LocationDao,
    ): LocationIdQuery[R, T] =
    new LocationIdQuery[R, T](q)

  implicit def toOptLocationIdQuery[R <: SlickRecord, T <: SlickTable[R] with OptLocationIdColumn](
      q: Query[T, T#TableElementType, Seq],
    )(implicit
      locationDao: LocationDao,
    ): OptLocationIdQuery[R, T] =
    new OptLocationIdQuery[R, T](q)

  implicit def toSlickQuery[R <: SlickRecord, T <: SlickTable[R]](
      q: Query[T, T#TableElementType, Seq],
    ): SlickQuery[R, T] =
    new SlickQuery[R, T](q)

  implicit def toSlickMerchantQuery[R <: SlickMerchantRecord, T <: SlickMerchantTable[R]](
      q: Query[T, T#TableElementType, Seq],
    ): SlickMerchantQuery[R, T] =
    new SlickMerchantQuery[R, T](q)

  implicit def toOrdersTableQuery[R <: OrderRecord, T <: OrdersTable](
      q: Query[T, T#TableElementType, Seq],
    )(implicit
      customerMerchantDao: CustomerMerchantDao,
      onlineOrderAttributeDao: OnlineOrderAttributeDao,
      paymentTransactionDao: PaymentTransactionDao,
    ): OrdersTableQuery[R, T] =
    new OrdersTableQuery[R, T](q)
}
