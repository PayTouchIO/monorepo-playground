package io.paytouch.core.reports.queries
import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.plainApi._
import io.paytouch.core.entities.Pagination
import io.paytouch.core.reports.async.exporters.CsvMsg
import io.paytouch.core.reports.entities.enums.CsvExports
import io.paytouch.core.utils.{ Formatters, UtcTime }
import slick.dbio.DBIO
import slick.jdbc.GetResult

sealed trait CsvExport[Q <: CsvExports] {
  implicit val getResult: GetResult[List[String]] =
    GetResult(r => header.map(_ => r.nextStringOption().getOrElse("")))
  def paginatedQuery(merchantId: UUID, pagination: Pagination): DBIO[Vector[List[String]]]
  def header: List[String]
  def buildFilename(msg: CsvMsg[_]): String
}

object CsvExport {
  // to implement a new export just add the correspodning CsvExport instance, testing is handled by ExportsSingleViewFSpec
  implicit val customersQuery: CsvExport[CsvExports.Customers.type] =
    new CsvExport[CsvExports.Customers.type] {
      def paginatedQuery(merchantId: UUID, pagination: Pagination): DBIO[Vector[List[String]]] =
        sql"""
             |select
             |    c.first_name,
             |    c.last_name,
             |    c.email,
             |    cl.total_spend_amount,
             |    cl.total_visits,
             |    product_data.total_products_purchased,
             |    product_data.last_purchase_date,
             |    c.dob,
             |    c.anniversary,
             |    c.phone_number,
             |    c.address_line_1,
             |    c.address_line_2,
             |    c.city,
             |    c.state,
             |    c.country,
             |    c.postal_code
             |from customer_merchants c
             |left join customer_locations cl
             |on c.customer_id = cl.customer_id
             |left join (
             |    select o.customer_id, SUM(oi.quantity) as total_products_purchased, MAX(o.received_at) as last_purchase_date
             |    from order_items oi, orders o
             |    where oi.order_id = o.id
             |    and oi.payment_status = 'paid'
             |    and o.merchant_id = $merchantId
             |    group by o.customer_id
             |) product_data
             |on product_data.customer_id = c.customer_id
             |where c.merchant_id = $merchantId
             |limit ${pagination.limit}
             |offset ${pagination.offset};
       """.stripMargin.as[List[String]]

      def header: List[String] =
        List(
          "First Name",
          "Last Name",
          "Email",
          "Total Spend Amount",
          "Total Visits",
          "Total Products Purchased",
          "Last Purchase Data",
          "Date of Birth",
          "Anniversary",
          "Phone Number",
          "Address Line 1",
          "Address Line 2",
          "City",
          "State",
          "Country",
          "Postal Code",
        )

      def buildFilename(msg: CsvMsg[_]) =
        Seq(msg.filename, UtcTime.now.format(Formatters.LocalDateFormatter)).mkString("_")
    }

  implicit val reimportableProductsQuery: CsvExport[CsvExports.ReimportableProducts.type] =
    new CsvExport[CsvExports.ReimportableProducts.type] {
      // NB: Double dollar signs on the `string_to_array` lines are actually escape sequence for a single dollar symbol
      def paginatedQuery(merchantId: UUID, pagination: Pagination): DBIO[Vector[List[String]]] =
        sql"""
             |SELECT
             |  p.id,
             |  p.name,
             |  p.description,
             |  p.price_amount,
             |  p.cost_amount,
             |  c.name,
             |  (string_to_array((string_to_array(ai.variant_options, ':$$:'))[1], ':*:'))[1],
             |  (string_to_array((string_to_array(ai.variant_options, ':$$:'))[1], ':*:'))[2],
             |  (string_to_array((string_to_array(ai.variant_options, ':$$:'))[2], ':*:'))[1],
             |  (string_to_array((string_to_array(ai.variant_options, ':$$:'))[2], ':*:'))[2],
             |  (string_to_array((string_to_array(ai.variant_options, ':$$:'))[3], ':*:'))[1],
             |  (string_to_array((string_to_array(ai.variant_options, ':$$:'))[3], ':*:'))[2]
             |FROM products p
             |JOIN article_identifiers ai ON p.id = ai.id
             |LEFT JOIN product_categories pc ON pc.product_id IN (p.id, p.is_variant_of_product_id)
             |LEFT JOIN categories c ON c.id = pc.category_id
             |WHERE p.merchant_id = $merchantId
             |AND p.deleted_at IS NULL
             |AND p.type in ('simple', 'variant')
             |ORDER BY p.name, ai.variant_options
             |limit ${pagination.limit}
             |offset ${pagination.offset};
       """.stripMargin.as[List[String]]

      def header: List[String] =
        List(
          "product_id",
          "product name",
          "description",
          "price",
          "cost",
          "category",
          "variant name",
          "variant option",
          "variant name",
          "variant option",
          "variant name",
          "variant option",
        )

      def buildFilename(msg: CsvMsg[_]) =
        Seq(msg.filename, UtcTime.now.format(Formatters.LocalDateFormatter)).mkString("_")
    }

  implicit val inventoryCountQuery: CsvExport[CsvExports.InventoryCount.type] =
    new CsvExport[CsvExports.InventoryCount.type] {
      def paginatedQuery(merchantId: UUID, pagination: Pagination): DBIO[Vector[List[String]]] =
        sql"""
             |SELECT p.id, p.name, variants.name, p.sku, p.upc, p.price_amount, p.cost_amount, COALESCE(SUM(CASE oi.payment_status = 'paid' OR oi.payment_status IS NULL WHEN TRUE THEN oi.quantity WHEN FALSE THEN 0 END), 0), COALESCE(s.quantity, 0)
             |FROM products p
             |LEFT JOIN order_items oi ON p.id = oi.product_id
             |LEFT JOIN orders o ON o.id = oi.order_id
             |LEFT JOIN stocks s ON p.id = s.product_id
             |LEFT OUTER JOIN (
             |    SELECT pvo.product_id, string_agg(CONCAT(vot.name, '#', vo.name), '%') as name
             |    FROM product_variant_options pvo
             |    JOIN variant_options vo ON vo.id = pvo.variant_option_id
             |    JOIN variant_option_types vot ON vot.id = vo.variant_option_type_id
             |    GROUP BY pvo.product_id
             |) AS variants ON p.id = variants.product_id
             |
             |WHERE p.merchant_id = $merchantId
             |AND p.deleted_at IS NULL
             |AND p.type in ('simple', 'variant')
             |GROUP BY p.id, variants.name, s.quantity
             |ORDER BY p.name, variants.name
             |limit ${pagination.limit}
             |offset ${pagination.offset};
       """.stripMargin.as[List[String]]

      def header: List[String] =
        List(
          "product_id",
          "product name",
          "variant name/options",
          "sku",
          "upc",
          "price",
          "cost",
          "sales_count",
          "current_quantity",
        )

      def buildFilename(msg: CsvMsg[_]) =
        Seq(msg.filename, UtcTime.now.format(Formatters.LocalDateFormatter)).mkString("_")
    }

  def cashDrawersQuery(from: LocalDateTime, to: LocalDateTime): CsvExport[CsvExports.CashDrawers.type] =
    new CsvExport[CsvExports.CashDrawers.type] {
      val formattedFrom = from.format(Formatters.LocalDateFormatter)
      val formattedTo = to.format(Formatters.LocalDateFormatter)

      def paginatedQuery(merchantId: UUID, pagination: Pagination): DBIO[Vector[List[String]]] =
        sql"""
             |SELECT cd.id,
             |       CONCAT(u.first_name, ' ', u.last_name) as employee_name,
             |       cd.name,
             |       cd.started_at,
             |       cd.ended_at,
             |       cd.starting_cash_amount,
             |       cd.ending_cash_amount,
             |       cd.cash_sales_amount,
             |       cd.cash_refunds_amount,
             |       cd.paid_in_and_out_amount,
             |       cd.expected_amount,
             |       pay_ins.total as paid_in_amount,
             |       pay_outs.total as paid_out_amount,
             |       cd.tipped_in_amount,
             |       cd.tipped_out_amount,
             |       cd.status,
             |       debit_card.total as debit_card_total,
             |       credit_card.total as credit_card_total
             |FROM cash_drawers cd,
             |     users u
             |LEFT JOIN LATERAL
             |  ( SELECT COALESCE(ROUND(SUM( CASE pt.type IN ('refund', 'void') WHEN TRUE THEN (pt.payment_details->>'amount')::numeric * -1 WHEN FALSE THEN (pt.payment_details->>'amount')::numeric  END ), 2),  0.00) as total FROM payment_transactions pt join orders o ON pt.order_id = o.id WHERE o.user_id = u.id AND pt.paid_at BETWEEN cd.started_at AND COALESCE(cd.ended_at, NOW()) AND pt.payment_type = 'debit_card') debit_card ON true
             |LEFT JOIN LATERAL
             |  ( SELECT COALESCE(ROUND(SUM( CASE pt.type IN ('refund', 'void') WHEN TRUE THEN (pt.payment_details->>'amount')::numeric * -1 WHEN FALSE THEN (pt.payment_details->>'amount')::numeric  END ), 2),  0.00) as total FROM payment_transactions pt join orders o ON pt.order_id = o.id WHERE o.user_id = u.id AND pt.paid_at BETWEEN cd.started_at AND COALESCE(cd.ended_at, NOW()) AND pt.payment_type = 'credit_card') credit_card ON true
             |LEFT JOIN LATERAL
             |  ( SELECT COALESCE(ROUND(SUM(cda.pay_in_amount), 2),  0.00) as total FROM cash_drawer_activities cda WHERE cda.cash_drawer_id = cd.id AND cda.type = 'pay_in') pay_ins ON true
             |LEFT JOIN LATERAL
             |  ( SELECT COALESCE(ROUND(SUM(cda.pay_out_amount), 2),  0.00) as total FROM cash_drawer_activities cda WHERE cda.cash_drawer_id = cd.id AND cda.type = 'pay_out') pay_outs ON true
             |WHERE cd.employee_id = u.id
             |  AND cd.merchant_id = $merchantId
             |  AND cd.created_at BETWEEN $formattedFrom::date AND ($formattedTo::date + 1)
             |limit ${pagination.limit}
             |offset ${pagination.offset};
       """.stripMargin.as[List[String]]

      def header: List[String] =
        List(
          "id",
          "employee_name",
          "name",
          "started_at",
          "ended_at",
          "starting_cash_amount",
          "ending_cash_amount",
          "cash_sales_amount",
          "cash_refunds_amount",
          "paid_in_and_out_amount",
          "expected_amount",
          "paid_in_amount",
          "paid_out_amount",
          "tipped_in_amount",
          "tipped_out_amount",
          "status",
          "debit_card_total",
          "credit_card_total",
        )

      def buildFilename(msg: CsvMsg[_]) =
        Seq(msg.filename, formattedFrom, formattedTo).mkString("_")
    }
}
