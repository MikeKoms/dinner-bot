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

case class NotVoted() extends Actions

case class Voted() extends Actions

trait Api

//for production = "prod"
class DatabaseApi(dbname: String = "prod") extends Api {
  type Category = String

  import T._

  val db = Database.forConfig(dbname)

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
    * Return Future[Created] or  Future[AlreadyCreated]
    * or NotExistingUser
    * */
  def createPool(user: User, chatId: String): Future[Actions] = {
    val mock = Pool(0, chatId, true, None)
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
  def voteInsertOrUpdate(user: User, choice: Category, chatId: String): Future[Actions] = {
    val qUserId = users.filter(_.telegramId === user.telegramId).map(_.id).result.headOption
    val qPoolId = pools.filter(_.chatId === chatId).map(_.id).result.headOption
    val zipped = qUserId zip qPoolId

    val action = zipped.flatMap {
      case (None, None) => DBIO.successful(NotExistingUserAndPool())
      case (None, _) => DBIO.successful(NotExistingUser())
      case (_, None) => DBIO.successful(NotExistingPool())
      case (Some(userId), Some(poolId)) =>
        for {
          existing <- votes.filter(v => v.poolId === poolId && v.userId === userId).result.headOption
          row = existing.map(_.copy(choice = choice)) getOrElse Vote(poolId, userId, choice)
          result <- votes.insertOrUpdate(row).map(_ => Completed())
        } yield result
    }
    db.run(action.transactionally)
  }

  /**
    * Return option users associated to chatId
    */
  def getUsersByChatId(chatId: String): Future[Option[Seq[User]]] = db.run(usersByChatIdQ(chatId))

  private def usersByChatIdQ(chatId: String) = {
    val action = pools.filter(_.chatId === chatId).map(_.id).result.headOption.flatMap {
      case Some(id) => (
        for {
          (user, _) <- users join votes.filter(_.poolId === id) on (_.id === _.userId)
        } yield (user.telegramId, user.privateChatId)
        ).result.map(xs => Some(xs.map(x => User(x._1, x._2))))
      case None => DBIO.successful(None)
    }
    action
  }

  /// def deleteVote(user:User(),pool:Pool,) //
  def getUserByTelegramId(telegramId: String): Future[Option[User]] =
    db.run(users.filter(_.telegramId === telegramId).result.headOption)

  def getCreatorByChatId(chatId: String): Future[Option[User]] =
    db.run(
      pools.filter(_.chatId === chatId).map(_.creatorId).result.headOption.flatMap {
        case None => DBIO.successful(None)
        case Some(id) => users.filter(_.id === id).result.headOption
      }.transactionally)


  /** Added vote for user in pool
    * Return Seq[Future [Actions] ]
    * Possibly Actions view in voteInsertOrUpdate */
  def updatePool(chatId: String, users_arg: Seq[User]): Seq[Future[Actions]] =
    users_arg.map(user =>
      voteInsertOrUpdate(user, defaultString, chatId)
    )

  /**
    * Delete Pool by id and associated with him votes
    * return Deleted()/AlreadyDeleted()
    * Recommendation: Run after you got result
    **/
  def deletePoolByChatId(chatId: String): Future[Actions] =
    db.run(pools.filter(_.chatId === chatId).delete.transactionally).map {
      case 1 => Deleted()
      case _ => AlreadyDeleted()
    }

  /**
    * Set flag isFinished for this pool = True
    * Return number of updated rows
    */
  def finishPool(chatId: String): Future[Int] = {
    val action = pools.filter(_.chatId === chatId).map(_.isFinished).update(true).transactionally
    db.run(action)
  }

  def getStatusOfPool(chatId: String): Future[Option[Boolean]] =
    db.run(pools.filter(_.chatId === chatId).map(_.isFinished).result.headOption.transactionally)

  def getChatIdByCreatorTelegram(telegramId: String): Future[Option[String]] = {
    var action = users.filter(_.telegramId === telegramId).map(_.id).result.headOption
      .flatMap {
        case Some(id) => pools.filter(_.creatorId === id).map(_.chatId).result.headOption
        case _ => DBIO.successful(None)
      }
    db.run(action.transactionally)
  }

  /**
    * Evaluate result for chat and return Action with seq of users and they actions
    * (Completed/NotVoted)
    * if all voted then  return  Future [Result(result) and sequence of users with Voted]
    * otherwise Future[ NotVoted with seq of users and they actions - Voted/NotVoted]
    **/
  def getResult(chatId: String): Future[(Actions, Seq[(User, Actions)])] = {
    val qPoolId = pools.filter(_.chatId === chatId).map(_.id).result.headOption
    val c = qPoolId.flatMap {
      case Some(id) =>
        val seq = (for {
          (user, vote) <- users join votes.filter(_.poolId === id) on (_.id === _.userId)
        } yield (vote.choice, user.telegramId, user.privateChatId)
          ).result
        seq
      case _ => DBIO.successful(Seq.empty[(String, String, String)])
    }.map {
      case xs if xs.isEmpty => (NotExistingPool(), Seq.empty[(User, Actions)])
      case xs if xs.forall(_._1 != defaultString) =>
        val result = evaluateResult(xs.map(_._1))
        val usersAndActions = xs.map(x => (User(x._2, x._3), Voted()))
        (Result(result), usersAndActions)
      case xs =>
        (
          NotVoted(),
          xs.map(x =>
            if (x._1 == defaultString) (User(x._2, x._3), NotVoted())
            else (User(x._2, x._3), Voted())
          )
        )
    }
    db.run(c.transactionally)
  }

  /** Ignore unvoted  persons and return evaluated result
    * Return: Future[NotExistingPool()] or Future[Result(string)]
    * **NotExisting could mean that nobody even voted
    * */
  def forceResult(chatId: String): Future[Actions] = {
    val qPoolId = pools.filter(_.chatId === chatId).map(_.id).result.headOption
    val action = qPoolId.flatMap {
      case Some(poolId) =>
        votes.filter(x => x.poolId === poolId && x.choice =!= defaultString)
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

    case Vote(poolId, userId, _, _) =>
      votes.filter(
        x => x.poolId === poolId && x.userId === userId).exists.result

    case Pool(_, chatId, _, _) =>
      pools.filter(
        x => x.chatId === chatId).exists.result
  }


  private def createVoteAction(poolId: Long, userId: Long) =
    votes += Vote(poolId, userId, defaultString)

  private def evaluateResult(seq: Seq[String]) =
    seq.groupBy(identity).maxBy(_._2.size)._1
}
