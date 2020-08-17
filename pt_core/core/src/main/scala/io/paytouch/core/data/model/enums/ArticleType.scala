package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait ArticleType extends EnumEntrySnake {

  def isMain =
    Seq(
      ArticleType.Simple,
      ArticleType.Template,
      ArticleType.GiftCard,
      ArticleType.CustomProduct,
    ) contains this

  def isStorable =
    Seq(
      ArticleType.Simple,
      ArticleType.Variant,
      ArticleType.GiftCard,
    ) contains this

  def isSimple = this == ArticleType.Simple
  def isTemplate = this == ArticleType.Template
  def isVariant = this == ArticleType.Variant
  def isGiftCard = this == ArticleType.GiftCard
  def isCustomProduct = this == ArticleType.CustomProduct
}

case object ArticleType extends Enum[ArticleType] {

  case object Simple extends ArticleType
  case object Template extends ArticleType
  case object Variant extends ArticleType
  case object GiftCard extends ArticleType
  case object CustomProduct extends ArticleType

  val values = findValues

  private val searchableValues = Seq(
    ArticleType.Simple,
    ArticleType.Template,
    ArticleType.Variant,
  )

  val storables = searchableValues.filter(_.isStorable)
  val mains = searchableValues.filter(_.isMain)
}

sealed abstract class ArticleTypeAlias(val types: ArticleType*) extends EnumEntrySnake

case object ArticleTypeAlias extends Enum[ArticleTypeAlias] {

  case object Simple extends ArticleTypeAlias(ArticleType.Simple)
  case object Template extends ArticleTypeAlias(ArticleType.Template)
  case object Variant extends ArticleTypeAlias(ArticleType.Variant)
  case object Main extends ArticleTypeAlias(ArticleType.mains: _*)
  case object Storable extends ArticleTypeAlias(ArticleType.storables: _*)

  val values = findValues

  def toArticleTypes(alias: Option[ArticleTypeAlias], aliases: Option[Seq[ArticleTypeAlias]]): Seq[ArticleType] = {
    val selectedAliases = alias.toSeq ++ aliases.getOrElse(Seq.empty)
    if (selectedAliases.isEmpty) Main.types else selectedAliases.flatMap(_.types).distinct
  }

}
