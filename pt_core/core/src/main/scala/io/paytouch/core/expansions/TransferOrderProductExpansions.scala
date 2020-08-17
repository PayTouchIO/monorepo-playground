package io.paytouch.core.expansions

final case class TransferOrderProductExpansions(withOptions: Boolean) extends BaseExpansions {

  val toArticleExpansions = ArticleExpansions.empty.copy(withVariants = withOptions)
}

object TransferOrderProductExpansions {
  def empty: TransferOrderProductExpansions = TransferOrderProductExpansions(withOptions = false)
}
