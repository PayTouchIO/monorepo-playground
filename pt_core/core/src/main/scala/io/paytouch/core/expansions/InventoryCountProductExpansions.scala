package io.paytouch.core.expansions

final case class InventoryCountProductExpansions(withOptions: Boolean) extends BaseExpansions {

  val toArticleExpansions = ArticleExpansions.empty.copy(withVariants = withOptions)
}
