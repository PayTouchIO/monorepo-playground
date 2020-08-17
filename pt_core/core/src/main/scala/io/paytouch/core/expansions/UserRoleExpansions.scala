package io.paytouch.core.expansions

final case class UserRoleExpansions(withUsersCount: Boolean, withPermissions: Boolean) extends BaseExpansions

object UserRoleExpansions {
  val empty: UserRoleExpansions = UserRoleExpansions(false, false)

  def withoutPermissions(withUsersCount: Boolean): UserRoleExpansions =
    UserRoleExpansions(withUsersCount, withPermissions = false)

  def withPermissions(withUsersCount: Boolean): UserRoleExpansions =
    UserRoleExpansions(withUsersCount, withPermissions = true)
}
