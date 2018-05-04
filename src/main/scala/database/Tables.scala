package database

//import slick.driver.H2Driver.api._
import slick.jdbc.H2Profile.api._

// table query
object T {
  val users = TableQuery[Users]
  val votes = TableQuery[Votes]
  val pools = TableQuery[Pools]
  val defaultString = "empty"

}


//case classes for db
abstract class Row

case class User(telegramId: String, privateChatId: String, id: Option[Long] = None)
  extends Row

case class Vote(poolId: Long, userId: Long, choice: String, id: Option[Long] = None)
  extends Row

case class Pool(creatorId: Long, chatId: String, isFinished: Boolean, id: Option[Long] = None)
  extends Row

//Tables

class Users(tag: Tag) extends Table[User](tag, "USERS") {
  def id: Rep[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)

  def telegramId: Rep[String] = column[String]("TELEGRAMID")

  def privateChatId: Rep[String] = column[String]("PRIVATECHATID")

  override def * = (telegramId, privateChatId, id.?) <> (User.tupled, User.unapply)
}

class Pools(tag: Tag) extends Table[Pool](tag, "POLLS") {

  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

  def chatId = column[String]("CHATID")

  def creatorId = column[Long]("USER_ID")

  def isFinished = column[Boolean]("ISFINISHED", O.Default(false))

  def * = (creatorId, chatId, isFinished, id.?) <> (Pool.tupled, Pool.unapply)

  def creator = foreignKey(
    "FK_CREATOR", creatorId, TableQuery[Users])(
    user => user.id, onDelete = ForeignKeyAction.NoAction
  )
}

class Votes(tag: Tag) extends Table[Vote](tag, "VOTES") {
  def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)

  def choice = column[String]("CHOICE", O.Default(T.defaultString))

  def poolId = column[Long]("POOL_ID")

  def userId = column[Long]("USER_ID")


  def * = (poolId, userId, choice, id.?) <> (Vote.tupled, Vote.unapply)

  def user = foreignKey(
    "FK_USER", userId, TableQuery[Users])(
    user => user.id, onDelete = ForeignKeyAction.NoAction
  )

  def pool = foreignKey("FK_POOL", poolId, TableQuery[Pools])(_.id,
    onDelete = ForeignKeyAction.Cascade)
}

