package io.paytouch.ordering.services.features

import scala.concurrent._

import io.paytouch.ordering.entities.AppContext

trait Expander[S <: EnrichFeature] {
  type Record = S#Record
  type Entity = S#Entity
  type Expansions = S#Expansions
  type Filters = S#Filters

  type RS
  type D1
  type D2

  def retriever(rs: RS)(implicit context: AppContext): Future[D1]

  def toRS(
      records: Seq[Record],
      filters: Filters,
      expansions: Expansions,
    ): RS

  def dataExtractor(record: Record, mapping: D1): D2

  def copier(entity: Entity, d2: D2): Entity
}

trait MandatoryExpander[S <: EnrichFeature] extends Expander[S] {
  type Data
  type RS = Seq[Record]
  type D1 = Map[Record, Data]
  type D2 = Data

  protected def defaultData: Data

  override def dataExtractor(record: Record, mapping: Map[Record, Data]): Data =
    mapping.getOrElse(record, defaultData)

  override def toRS(
      records: Seq[Record],
      filters: Filters,
      expansions: Expansions,
    ): Seq[Record] = records
}

object MandatoryExpander {
  def apply[S <: EnrichFeature, D](
      retrieverF: AppContext => Seq[S#Record] => Future[Map[S#Record, D]],
      copierF: (S#Entity, D) => S#Entity,
      defaultDataValue: D,
    ) =
    new MandatoryExpander[S] {
      type Data = D

      def defaultData: Data = defaultDataValue

      def copier(entity: Entity, data: Data): Entity = copierF(entity, data)

      def retriever(records: RS)(implicit context: AppContext) = retrieverF(context)(records)
    }
}

trait OptionalExpander[S <: EnrichFeature] extends Expander[S] {
  type Data
  type RS = (Seq[Record], Expansions)
  type D1 = Map[Record, Data]
  type D2 = Option[Data]

  def expansionExtractor: Expansions => Boolean

  def optionalRetriever(records: Seq[Record])(implicit context: AppContext): Future[Map[Record, Data]]

  override def retriever(rs: (Seq[Record], Expansions))(implicit context: AppContext): Future[Map[Record, Data]] = {
    val (records, expansions) = rs
    if (expansionExtractor(expansions)) optionalRetriever(records)
    else Future.successful(Map.empty)
  }

  override def dataExtractor(record: Record, mapping: Map[Record, Data]): Option[Data] =
    mapping.get(record)

  override def toRS(
      records: Seq[Record],
      filters: Filters,
      expansions: Expansions,
    ): (Seq[Record], Expansions) =
    records -> expansions
}

object OptionalExpander {
  def apply[S <: EnrichFeature, D](
      retrieverF: AppContext => Seq[S#Record] => Future[Map[S#Record, D]],
      copierF: (S#Entity, Option[D]) => S#Entity,
      expansionExtractorF: S#Expansions => Boolean,
    ) =
    new OptionalExpander[S] {
      type Data = D

      def copier(entity: Entity, data: Option[Data]): Entity = copierF(entity, data)

      def optionalRetriever(records: Seq[S#Record])(implicit context: AppContext) = retrieverF(context)(records)

      override def expansionExtractor: Expansions => Boolean = expansionExtractorF
    }
}
