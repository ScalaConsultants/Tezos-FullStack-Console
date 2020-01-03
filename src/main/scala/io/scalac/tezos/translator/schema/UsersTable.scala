package io.scalac.tezos.translator.schema

import io.scalac.tezos.translator.model.UserModel
import io.scalac.tezos.translator.model.types.Auth.{ PasswordHash, Username }
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ ProvenShape, Tag }
import slick.sql.SqlProfile.ColumnOption.NotNull

object UsersTable {
  val users = TableQuery[UsersTable]
}

class UsersTable(tag: Tag) extends Table[UserModel](tag, "users") {

  override def * : ProvenShape[UserModel] = (username, passwordHash) <> (UserModel.tupled, UserModel.unapply)

  def username: Rep[Username] = column[Username]("username", NotNull, O.Unique, O.SqlType("VARCHAR(30)"))

  def passwordHash: Rep[PasswordHash] = column[PasswordHash]("password_hash", NotNull, O.SqlType("TEXT"))

}
