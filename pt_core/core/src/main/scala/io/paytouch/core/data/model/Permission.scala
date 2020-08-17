package io.paytouch.core.data.model

final case class Permission(
    read: Boolean = false,
    create: Boolean = false,
    edit: Boolean = false,
    delete: Boolean = false,
  ) { self =>

  val representation = Permission.fieldsSequence.map(_.andThen(booleanAsRepresentation)(self)).mkString("")

  private def booleanAsRepresentation(b: Boolean): String = if (b) "1" else "0"
}

object Permission {
  def fieldsSequence: Seq[Permission => Boolean] =
    Seq(
      _.read,
      _.create,
      _.edit,
      _.delete,
    )

  def fromRepresentation(rep: String): Permission = {
    val r = representationParser(rep, 0)
    val c = representationParser(rep, 1)
    val e = representationParser(rep, 2)
    val d = representationParser(rep, 3)
    Permission(read = r, create = c, edit = e, delete = d)
  }

  private def representationParser(rep: String, idx: Int): Boolean =
    if (idx < rep.length)
      rep.charAt(idx) == '1'
    else
      false
}

case class PermissionUpdate(
    read: Option[Boolean] = None,
    create: Option[Boolean] = None,
    edit: Option[Boolean] = None,
    delete: Option[Boolean] = None,
  ) {
  def toRecord: Permission = updateRecord(Permission())

  def updateRecord(record: Permission): Permission =
    Permission(
      read = read.getOrElse(record.read),
      create = create.getOrElse(record.create),
      edit = edit.getOrElse(record.edit),
      delete = delete.getOrElse(record.delete),
    )
}

object PermissionUpdate {
  val Empty = PermissionUpdate()
  val ReadOnly = PermissionUpdate(read = Some(true))
  val ReadAndCreate = PermissionUpdate(read = Some(true), create = Some(true))
  val ReadAndEdit = PermissionUpdate(read = Some(true), edit = Some(true))
  val ReadAndWrite = PermissionUpdate(read = Some(true), create = Some(true), edit = Some(true))
  val All = PermissionUpdate(read = Some(true), create = Some(true), edit = Some(true), delete = Some(true))
}
