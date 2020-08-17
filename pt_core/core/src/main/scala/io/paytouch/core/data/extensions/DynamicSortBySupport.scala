package io.paytouch.core.data.extensions
import slick.ast.Ordering.Direction
import slick.lifted.Rep

object DynamicSortBySupport {
  import slick.ast.Ordering
  import slick.lifted.Query
  import slick.lifted.ColumnOrdered
  import slick.lifted.Ordered

  trait ColumnSelector {
    val sortableFields: Map[String, Rep[_]] //The runtime map between string names and table columns
  }

  case class Sortings(fieldAndDirections: Seq[(String, Direction)])
  object Sortings {
    def parse(fields: Seq[String]): Sortings =
      Sortings(fields.map { field =>
        if (field.startsWith("-")) (field.substring(1), Ordering.Desc)
        else (field, Ordering.Asc)
      })
  }

  implicit class MultiSortableQuery[A <: ColumnSelector, B, C[_]](query: Query[A, B, C]) {
    def dynamicSortBy(sortings: Sortings, defaultOrdering: A => ColumnOrdered[_]): Query[A, B, C] =
      if (sortings.fieldAndDirections.isEmpty) query.sortBy(defaultOrdering)
      else
        sortings.fieldAndDirections.foldRight(query) { // Fold right is reversing order
          case ((sortColumn, sortOrder), queryToSort) =>
            val sortOrderRep: Rep[_] => Ordered = ColumnOrdered(_, Ordering(sortOrder))
            val sortColumnRep: A => Rep[_] = _.sortableFields(sortColumn)
            queryToSort.sortBy(sortColumnRep)(sortOrderRep)
        }
  }
}
