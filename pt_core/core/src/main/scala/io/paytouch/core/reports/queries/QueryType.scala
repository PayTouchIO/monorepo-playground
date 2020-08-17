package io.paytouch.core.reports.queries

sealed trait QueryType

sealed trait QueryAggrType extends QueryType
case object CountQuery extends QueryAggrType
case object SumQuery extends QueryAggrType
case object AverageQuery extends QueryAggrType

sealed trait QueryTopType extends QueryType
case object TopQuery extends QueryTopType

sealed trait QueryListType extends QueryType
case object ListQuery extends QueryListType
