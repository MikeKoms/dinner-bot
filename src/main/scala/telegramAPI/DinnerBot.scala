package telegramAPI

import akka.actor.Actor
import database.T.defaultString
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.api.declarative._
import info.mukel.telegrambot4s.models._

import scala.concurrent.{ExecutionContext, Future}
import database.{User, _}
import info.mukel.telegrambot4s.methods.SendMessage

object DinnerBot extends App{
  val countries = Seq("China", "Russia", "America", "Japan")
  val inst = new DinnerBot("493192045:AAEbw_bpNufJ4NeJGRpcVM8R1fWO3qC-ZEY").run()

}

class DinnerBot(token: String, databaseApi: DatabaseApi = new database.DatabaseApi) extends ExampleBot(token)
  with Polling
  with Commands
  with BotBase
  with Callbacks {

  /**
    * Print callback buttons in chat
    * @param user The user to whom the message will be sent
    * @param chatID ID of group chat where poll was created
    * @return object of send message
    */
  def pollButtons(user: database.User, chatID: Long): Future[Message] = {
    val buttons = DinnerBot.countries.map(x => InlineKeyboardButton.callbackData(x, x))
    request(SendMessage(ChatId(user.telegramId), s"Куда вы хотите? (голосование чата c id $chatID",
      replyMarkup = Some(InlineKeyboardMarkup.singleColumn(buttons))))
  }

  /**
    * Optional: returns result of poll, if the vote ended
    * @param chatID
    * @return Some(res) - if the vote ended
    *         None - if the vote not 1ended
    */
  def getPollResult(chatID: String) = {
    val pollStatus = databaseApi.getResult(chatID)
    for{
      x <- pollStatus
    } yield x._1 match {
      case Result(res) => Some(res)
      case _: NotVoted => None
    }
  }

  def endOfPoll(chatID: String, result: String) = {
    request(SendMessage(ChatId(chatID),
      """ Голосование завершено, спасибо! :)
        | Осталось разобраться, где искать подходящие для вас варианты.
        | Пришлите мне место на карте, где вы планируете оказаться.
      """.stripMargin))
  }

  /**
    * Print info if /start was written in GroupChat
    * Register user in database if was written in Private chat
    */

  onCommand("/start") { implicit msg =>
    val chatType = msg.chat.`type`
    chatType match {
      //нужно добавить проверку из базы данных на регистрацию пользователя
      case ChatType.Private => {
        val usedID = msg.from.map(_.id.toString).get
        val privateChatID = msg.chat.id.toString
        val resultOfRegistration: Future[Actions] = databaseApi.addUser(usedID, privateChatID)
        resultOfRegistration.flatMap{
          case _: Created => reply("Спасибо!")
          case _: AlreadyCreated => reply("Вы уже зарегистрированы")
        } (executor = ExecutionContext.Implicits.global)
      }
      case ChatType.Group | ChatType.Supergroup => reply(
        """Всем привет! Мне надо с вами познакомиться.
          |Зарегистрируйте чат в базе командой /registerGroup,
          |а потом напишите мне @ChoiceOfLunchPlaceBot в лс /start""".stripMargin)
    }
  }

  /** register group chat in database
    * creator is remembered as creator
    */
  onCommand("/registerGroup") { implicit msg =>
    val chatType = msg.chat.`type`
    chatType match {
      case ChatType.Group | ChatType.Supergroup => {
        // надеемся, что у нас есть информаиация про юзера и мы не Tg - канале
        val db_user: Future[Option[database.User]] = databaseApi.getUserByTelegramId(msg.from.get.id.toString)

        db_user.flatMap {
          case Some(x) => {
            val groupChatID = msg.chat.id.toString
            val createdPool: Future[Actions] = databaseApi.createPool(x, groupChatID)
            createdPool.flatMap {
              case _: Created => reply("Ваша группа зарегистрирована! Напишите в чатике группы /addToPoll , чтобы принять участие в голосовании :)")
              case _: AlreadyCreated => reply("Ваша группа уже есть в базе данных")
              case _ => reply("Ошибка")
            }
          }
          case None => reply("Возможно, вам сначала надо зарегистрироваться в боте(напишите мне в лс /register)")
        }
      }
      case _ => reply("Возможно, вы хотели зарегистрировать себя, а не чат. Используйте команду /register")
    }
  }

  /**
    * register user in poll
    */
  onCommand("/addToPoll") { implicit msg =>
    val chatType = msg.chat.`type`
    chatType match {
      case ChatType.Group | ChatType.Supergroup => {
        val db_user: Future[Option[database.User]] = databaseApi.getUserByTelegramId(msg.from.get.id.toString)

        db_user.flatMap {
          case Some(x) => {
            val chatID = msg.chat.id.toString
            databaseApi.voteInsertOrUpdate(x, defaultString, chatID).flatMap {
              case _: NotExistingUserAndPool => reply("Ошибка, пользователь и опрос не найдены")
              case _: NotExistingUser => reply(msg.from.get.firstName + ", зарегистрируйтесь в лс бота")
              case _: NotExistingPool => reply("Зарегистрируйте чатик командой /registerGroup")
              case _ => reply(msg.from.get.firstName + ", вы теперь в опросике :)")
            }
          }
          case None => reply("Возможно, вам сначала надо зарегистрироваться в боте(напишите мне в лс /register)")
        }
      }
      case ChatType.Private => reply("Для добавления Вас в опрос необходимо написать в чатике группы /addToPoll")
    }
  }

  /** Start poll
    * Send callback buttons to users
    */
  onCommand("/startPoll") { implicit msg =>
    // допущение, что мы не в канале
    val userWhoStarts = msg.from.get.id.toString
    databaseApi.getCreatorByChatId(msg.chat.id.toString).foreach {
      case Some(x) => {
        if (x.telegramId == userWhoStarts) {
          val allUserToReply: Future[Option[Seq[database.User]]] = databaseApi.getUsersByChatId(msg.chat.id.toString)
          allUserToReply.flatMap {
            case Some(x) => {
              x.map { user =>
                pollButtons(user, msg.chat.id)
              }
              reply("Я разослал вам сообщения, голосуйте :)")
            }
            case None => reply("Группа не зарегистрирована")
          }
        }
        else
          reply("Вам нельзя начинать голосование :)")
      }
      case None => reply(msg.from.get.firstName + ", вас вообще нет в базе, зарегистрируйтесь, пожалуйста")
    }
  }

  /**
    * Handler of poll callbacks
    */
  onCallbackQuery { implicit cbq =>
    val from = cbq.from
    val vote = cbq.data.get
    val mess: String = cbq.message.get.text.get.split(" ").last
    val userForDatabase = database.User(from.id.toString, cbq.message.get.chat.id.toString)
    databaseApi.voteInsertOrUpdate(userForDatabase, vote, mess).flatMap({
      case _: NotExistingUserAndPool => request(SendMessage(ChatId(cbq.from.id), "Вас нет в несуществующем голосовании"))
      case _: NotExistingUser => request(SendMessage(ChatId(cbq.from.id), "Вас нет в голосовании, видимо вас исключили :("))
      case _: NotExistingPool => request(SendMessage(ChatId(cbq.from.id), "Голование закончено"))
      case _: Completed => request(SendMessage(ChatId(cbq.from.id), "Ваш голос учтен!"))
    }).onComplete(_ => getPollResult(mess).foreach{
      case Some(x) => endOfPoll(mess, x)
      case None => Unit
    })
  }

  /*onMessage{ implicit msg =>
    if (msg.location != None ){
      getPollResult(msg.chat.id.toString).foreach{
        case Some(x) => {
          val latitude = msg.location.get.latitude
          val longitude = msg.location.get.longitude
          reply("OK")
          //тут помещается логика взаимодействия с foursquare API
        }
        case None => Unit
      }
    }
  }*/


  /**
    * initiate end of poll before all the people have voted
    */
  onCommand("/result"){ implicit msg =>
    val chatType = msg.chat.`type`
    chatType match {
      case ChatType.Group | ChatType.Supergroup => {
        ???
      }
      case _ => reply("Команда доступна только в групповом чате!")
    }
  }
}
