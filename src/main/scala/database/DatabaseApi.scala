package database

import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait Actions

case class Deleted() extends Actions

case class Created() extends Actions

case class AlreadyCreated() extends Actions

case class AlreadyDeleted() extends Actions

case class Completed() extends Actions

case class Updated() extends Actions

case class NotExistingUser() extends Actions

case class NotExistingPool() extends Actions

case class NotExistingUserAndPool() extends Actions

case class Result(result: String) extends Actions

private case class Empty() extends Actions

object DatabaseApi {
  //type Category = String
  import T._


  /** Create user
    * Return : Future[Created()] or Future[AlreadyCreated()] */
  def addUser(telegramId: String, privateChatId: String): Future[Actions] = {
    val action = exists(User(telegramId, privateChatId)).flatMap {
      case false => (users += User(telegramId, privateChatId))
        .map(_ => Created())
      case true => DBIO.successful(AlreadyCreated())
    }
    db.run(action.transactionally)
  }

  /** Delete user from db
    * Return : Future[Deleted] or Future[AlreadyDeleted] */
  def deleteUser(user: User): Future[Actions] = {
    val deletingAction = users.filter(x =>
      x.telegramId === user.telegramId &&
        x.privateChatId === user.privateChatId).delete
    db.run(deletingAction).map {
      case 1 => Deleted()
      case _ => AlreadyDeleted()
    }
  }

  /** Create Pool
    * Return Future[Created] or  Future[AlreadyCreated]  */
  def createPool(user: User, chatId: String): Future[Actions] = {
    val mock = Pool(0, chatId,true, None)
    val action = exists(mock).flatMap {
      case false => users.filter(_.telegramId === user.telegramId)
        .map(_.id).result.headOption.flatMap {
        case Some(id) =>
          ((pools returning pools.map(_.id)) += Pool(id, chatId, false)).flatMap(
            poolId => createVoteAction(poolId, id).map(_ => Created()))
        case None => DBIO.successful(NotExistingUser())
      }
      case true => DBIO.successful(AlreadyCreated())
    }
    db.run(action.transactionally)
  }

  /** Update or create vote
    * Return NotExistingUserAndPool() or NotExistingUser()
    * or NotExistingPool() or Completed()  */
  def voteInsertOrUpdate(user: User, choice: String, pool: Pool, is_voted: Boolean = true): Future[Actions] = {
    val qUserId = users.filter(_.telegramId === user.telegramId).map(_.id).result.headOption
    val qPoolId = pools.filter(_.chatId === pool.chatId).map(_.id).result.headOption
    val zipped = qUserId zip qPoolId

    val action = zipped.flatMap {
      case (None, None) => DBIO.successful(NotExistingUserAndPool())
      case (None, _) => DBIO.successful(NotExistingUser())
      case (_, None) => DBIO.successful(NotExistingPool())
      case (Some(userId), Some(poolId)) =>
        val insertOrUpdate = votes.filter(x => x.poolId === poolId && x.userId === userId)
          .insertOrUpdate(Vote(poolId, userId, choice, is_voted))
        insertOrUpdate.map(_ => Completed())
    }
    db.run(action.transactionally)
  }

  /** Added vote for user in pool
    * Return Seq[Future [Actions] ]
    * Possibly Actions view in  voteInsertOrUpdate*/
  def updatePool(pool: Pool, users_arg: Seq[User]): Seq[Future[Actions]] =
    users_arg.map(user =>
      voteInsertOrUpdate(user, defaultString, pool, false)
    )

  //NotVoted,Completed, return users  and his action
  // TODO
  def getResult(pool: Pool): (Future[Actions], Future[(User, Actions)]) = ???

  /** Ignore unvoted  persons and return evaluated result
    * Return: Future[NotExistingPool()] or Future[Result(string)]
    * */
  def forceResult(pool: Pool): Future[Actions] = {
    val qPoolId = pools.filter(_.chatId === pool.chatId).map(_.id).result.headOption
    val action = qPoolId.flatMap {
      case Some(poolId) =>
        votes.filter(x=>x.poolId === poolId && x.choice=!=defaultString)
          .map(_.choice).result
      case None => DBIO.successful(Seq.empty)
    }
    db.run(action).map(x =>
      if (x.isEmpty) NotExistingPool()
      else Result(evaluateResult(x))
    )
  }

  private def exists(row: Row) = row match {
    case User(telegramId, privateChatId, _) =>
      users.filter(
        x => x.telegramId === telegramId && x.privateChatId === privateChatId)
        .exists.result

    case Vote(poolId, userId, _, _, _) =>
      votes.filter(
        x => x.poolId === poolId && x.userId === userId).exists.result

    case Pool(_, chatId,  _, _) =>
      pools.filter(
        x => x.chatId === chatId).exists.result
  }


  private def createVoteAction(poolId: Long, userId: Long) =
    votes += Vote(poolId, userId, defaultString, false)

  private def evaluateResult(seq: Seq[String]) =
    seq.groupBy(identity).maxBy(_._2.size)._1

}
