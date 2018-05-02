package telegramAPI

import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.api.declarative._
import info.mukel.telegrambot4s.models.{User, _}

import scala.concurrent.{ExecutionContext, Future}
import database._

object DinnerBot extends App{

  /*val fut = Future{Thread.sleep(3); 21+21}.foreach{
    case x: Int => println(x)
  }*/
  val inst = new DinnerBot("493192045:AAEbw_bpNufJ4NeJGRpcVM8R1fWO3qC-ZEY").run()
}

class DinnerBot(token: String) extends ExampleBot(token)
  with Polling
  with Commands
  with GlobalExecutionContext {
  onCommand("/start") { implicit msg =>
    val chatType = msg.chat.`type`
    chatType match {
      //нужно добавить проверку из базы данных на регистрацию пользователя
      case ChatType.Private => reply("Добро пожаловать! Для дальнейшей работы нужна регистрация. Используй команду /register :)")
      case ChatType.Group || ChatType.Supergroup=> reply(
        """Всем привет! Мне надо с вами познакомиться.
          |Зарегистрируйте чат в базе командой /registerGroup,
          |а потом напишите мне @ChoiceOfLunchPlaceBot в лс /start""".stripMargin)
    }
  }

  /** регистрирует пользователя в базе, если его там еще не было
    * если же зареган, то просто предупреждает пользователя об этом
    */
  onCommand("/register") { implicit msg =>
    val chatType = msg.chat.`type`
    chatType match {
      case ChatType.Private => {
        // пока что проблемы с каналом, поскольку вернется None
        val usedID = msg.from.map(_.id.toString).get
        val privateChatID = msg.chat.id.toString
        val resultOfRegistration: Future[Actions] = database.DatabaseApi.addUser(usedID, privateChatID)
        resultOfRegistration.flatMap{
          case _: Created => reply("Спасибо!")
          case _: AlreadyCreated => reply("Вы уже зарегистрированы")
        } (executor = ExecutionContext.Implicits.global)
      }
      case _ => reply("Возможно, что-то пошло не так.")
    }
  }

  /** регистрирует группу в баззе данных и заносит в качестве члена админа
    *
    */
  onCommand("/registerGroup"){ implicit msg =>
    val chatType = msg.chat.`type`
    chatType match {
      case ChatType.Group || ChatType.Supergroup => {
        // надеемся, что у нас есть информаиация про юзера и мы не Tg - канале
        val db_user: Future[Option[database.User]] = ??? // обращаемся к базе, находим юзера

        db_user.flatMap{
          case Some(x) => {
            val groupChatID = msg.chat.id.toString
            val createdPool: Future[Actions] = database.DatabaseApi.createPool(x, groupChatID)
            createdPool match {
              case _: Created => reply("Ваша группа зарегистрирована! Напишите в чатике группы /addToPoll , чтобы принять участие в голосовании :)")
              case _: AlreadyCreated => reply("Ваша группа уже есть в базе данных")
            }
          }
          case None => reply("Возможно, вам сначала надо зарегистрироваться в боте(напишите мне в лс /register)")
        }

      }
      case _ => reply("Возможно, вы хотели зарегистрировать себя, а не чат. Используйте команду /register")
    }
  }

  onCommand("/addToPoll"){ implicit msg =>
    val chatType = msg.chat.`type`
    chatType match {
      case ChatType.Group || ChatType.Supergroup => ???
      case ChatType.Private => reply("Для добавления Вас в опрос необходимо написать в чатике группы /addToPoll")
    }

  }
}