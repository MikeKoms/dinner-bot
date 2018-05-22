package telegramAPI

import secrets._
import database.T.defaultString
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.api.declarative._
import info.mukel.telegrambot4s.models._

import scala.concurrent.{Await, ExecutionContext, Future}
import database.{User, _}
import info.mukel.telegrambot4s.methods.{SendLocation, SendMessage}
import places.foursquare.{Venue, _}

import scala.concurrent.duration.Duration



object DinnerBot extends App{
  val countries = Seq("Кафе и рестораны", "Фастфуд" , "Итальянская кухня", "Японская кухня")
  val categories = Map("Кафе и рестораны" -> Categories.cafeAndRestaurants,
                      "Фастфуд" -> Categories.fastFood,
                      "Итальянская кухня" -> Categories.italian,
                      "Японская кухня" -> Categories.japanese)

  //val inst = new DinnerBot(Telegram.TOKEN).run()
  import slick.jdbc.H2Profile.api._

  val users = TableQuery[Users]
  val votes = TableQuery[Votes]
  val pools = TableQuery[Pools]
  lazy val api = new DatabaseApi("test")
  val schema = users.schema ++ votes.schema ++ pools.schema
  Await.result(api.db.run(schema.create), Duration.Inf)

  val inst = new DinnerBot(Telegram.TOKEN, api).run()


}

class DinnerBot(token: String,
                databaseApi: DatabaseApi = new database.DatabaseApi,
                foursquareApi: FoursquareService = new FoursquareService) extends ExampleBot(token)
  with Polling
  with Commands
  with BotBase
  with Callbacks {

  /**
    * Print callback buttons in chat
    * @param user The user to whom the message will be sent
    * @param chatName ID of group chat where poll was created
    * @return object of send message
    */
  def pollButtons(user: database.User, chatID: Long, chatName: String): Future[Message] = {
    val buttons = DinnerBot.countries.map(x => InlineKeyboardButton.callbackData(x, x))
    request(SendMessage(ChatId(user.telegramId), s"Куда вы хотите? \nchat info: $chatName, id $chatID",
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

  def foursquareResult(result: String, lng: BigDecimal, lat: BigDecimal) = {
    foursquareApi.categoriesInRadius(lat, lng, 1000, Seq(DinnerBot.categories(result)))
  }

  /** end of the poll
    * @param chatID
    * @param result result of poll
    * @return
    */
  def endOfPoll(chatID: String, result: String) = {
    databaseApi.finishPool(chatID)
    request(SendMessage(ChatId(chatID),
      """ Голосование завершено, спасибо! :)
        |Осталось совсем немного.
      """.stripMargin))
    val admin: Future[Option[User]] = databaseApi.getCreatorByChatId(chatID)
    admin.flatMap{
      case None => Future{println("Странная ошибка")}
      case Some(userAdmin) => {
        request(SendMessage(ChatId(userAdmin.telegramId), "Пришлите местоположение, в радиусе километра от которого вы с компанией хотите оказаться"))
      }
    }
  }

  def showResult(chatID: String, result: String, lng: BigDecimal, lat: BigDecimal) = {
    val places: Future[Seq[Venue]] = foursquareResult(result, lng, lat)
    places.map(x =>
      x.foreach(
        venue => {
          //venue.toString - вся надежда на Гришу
          request(SendMessage(ChatId(chatID), venue.toString))
          request(SendLocation(ChatId(chatID), venue.location.lat.toDouble, venue.location.lng.toDouble))
        }
      )
    )
    databaseApi.deletePoolByChatId(chatID)
  }

  /**
    * Print info if /start was written in GroupChat
    * Register user in database if was written in Private chat
    */

  onCommand("/start", "/start@ChoiceOfLunchPlaceBot") { implicit msg =>
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
  onCommand("/registerGroup", "/registergroup", "/registergroup@ChoiceOfLunchPlaceBot",
          "/registerGroup@ChoiceOfLunchPlaceBot") { implicit msg =>
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
          case None => reply("Возможно, вам сначала надо зарегистрироваться в боте(напишите мне в лс /start)")
        }
      }
      case _ => reply("Возможно, вы хотели зарегистрировать себя, а не чат. Используйте команду /start")
    }
  }

  /**
    * register user in poll
    */
  onCommand("/addToPoll", "/addtopoll", "/addToPoll@ChoiceOfLunchPlaceBot",
          "/addtopoll@ChoiceOfLunchPlaceBot") { implicit msg =>
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
          case None => reply("Возможно, вам сначала надо зарегистрироваться в боте(напишите мне в лс /start)")
        }
      }
      case ChatType.Private => reply("Для добавления Вас в опрос необходимо написать в чатике группы /addToPoll")
    }
  }

  /** Start poll
    * Send callback buttons to users
    */
  onCommand("/startPoll", "/startpoll", "/startPoll@ChoiceOfLunchPlaceBot",
    "/startpoll@ChoiceOfLunchPlaceBot") { implicit msg =>
    // допущение, что мы не в канале
    val userWhoStarts = msg.from.get.id.toString
    databaseApi.getCreatorByChatId(msg.chat.id.toString).foreach {
      case Some(x) => {
        if (x.telegramId == userWhoStarts) {
          val allUserToReply: Future[Option[Seq[database.User]]] = databaseApi.getUsersByChatId(msg.chat.id.toString)
          allUserToReply.flatMap {
            case Some(x) => {
              x.map { user =>
                pollButtons(user, msg.chat.id, msg.chat.firstName.get)
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
    }).onComplete(_ => {
      getPollResult(mess).foreach {
        case Some(x) => endOfPoll(mess, x)
        case None => Unit
      }
    })
  }

  onMessage{ implicit msg =>
    if (msg.location != None){
      // здесь должна быть функция, которая вернет чатик
      //val chatId = "-216449573"
      val chatId = databaseApi.getChatIdByCreatorTelegram(msg.from.get.id.toString).flatMap {
        case Some(id) => {
          databaseApi.getStatusOfPool(id).flatMap {
            case Some(true) => {
              databaseApi.forceResult(id).flatMap {
                case Result(res) => {
                  showResult(id, res, msg.location.get.longitude, msg.location.get.latitude)
                }
                case NotExistingPool() => reply("Как вы вообще получили эту ошибку?")
              }
            }
            case Some(false) => reply("Ввм следует сначала завершить голосование")
            case None => reply("Здорово, спасибо за ваше местоположение")
          }
        }
        case None => reply("Вы не являетесь администратором ни одного голосования ")
      }
    }

  }
  /**
    * initiate end of poll before all the people have voted
    */
  onCommand("/result", "/result@ChoiceOfLunchPlaceBot"){ implicit msg =>
    val chatType = msg.chat.`type`
    chatType match {
      case ChatType.Group | ChatType.Supergroup => {
        val userWhoStarts = msg.from.get.id.toString
        databaseApi.getCreatorByChatId(msg.chat.id.toString).foreach {
          case Some(x) => {
            if (x.telegramId == userWhoStarts) {
              val res: Future[Actions] = databaseApi.forceResult(msg.chat.id.toString)
              res.flatMap{
                case Result(res) => {
                  endOfPoll(msg.chat.id.toString, res)
                }
                case NotExistingPool() => reply("Что - то пошло не так, возможно никто еще не проголосовал")
              }
            }
            else reply("Вы не имеете права завершать голосование")
            }
          case None => reply(msg.from.get.firstName + ", вас вообще нет в базе, зарегистрируйтесь, пожалуйста")
          }
        }
      case _ => reply("Команда доступна только в групповом чате!")
    }
  }
}
