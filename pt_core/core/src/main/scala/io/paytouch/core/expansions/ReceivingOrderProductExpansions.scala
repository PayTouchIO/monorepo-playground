package io.paytouch.core.expansions

final case class ReceivingOrderProductExpansions(withOptions: Boolean) extends BaseExpansions {

  val toArticleExpansions = ArticleExpansions.empty.copy(withVariants = withOptions)
}
