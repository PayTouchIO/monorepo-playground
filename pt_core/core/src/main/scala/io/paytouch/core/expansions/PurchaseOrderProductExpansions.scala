package io.paytouch.core.expansions

final case class PurchaseOrderProductExpansions(withOptions: Boolean) extends BaseExpansions {

  val toArticleExpansions = ArticleExpansions.empty.copy(withVariants = withOptions)
}

object PurchaseOrderProductExpansions {

  val empty = PurchaseOrderProductExpansions(withOptions = false)
}
