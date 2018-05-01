package database


import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FlatSpec, Matchers}
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class DatabaseApiTest extends FlatSpec with Matchers with ScalaFutures  {
  implicit val defaultPatience =
    PatienceConfig(timeout = Span(120, Seconds), interval = Span(10, Millis))

  lazy val api = new DatabaseApi("test")

  val users = TableQuery[Users]
  val votes = TableQuery[Votes]
  val pools = TableQuery[Pools]
  val defaultString = "empty"

  "Create Tables with data" should "create tables with data " in {
    val schema = users.schema ++ votes.schema ++ pools.schema
    val setup = DBIO.seq(
      schema.create,
      users += User(telegramId = "nikita", privateChatId = "123"),
      users += User(telegramId = "vova", privateChatId = "456"),
      users += User(telegramId = "semen", privateChatId = "789"),
      users += User(telegramId = "mike", privateChatId = "010"),

      pools += Pool(1, "chatid1", false),
      pools += Pool(2, "chatid2", false),
      pools += Pool(3, "chatid3", false),

      votes += Vote(1, 1, "chinese" ), //nikita, chatid1
      votes += Vote(1, 2, "chinese" ), //vova chatid1
      votes += Vote(2, 3, "france" ), //semen chaid2
      votes += Vote(2, 4, "chinese") //mike chatid2
    )
    Await.result(api.db.run(setup), Duration.Inf)
  }

  "Add.New user" should "add in db and return Action[Created]" in {
    api.addUser("new", "new").futureValue shouldBe Created()
  }
  "Add.Already existing user" should "not add in db and return Action[Alreary Created]" in {
    api.addUser("nikita", "123").futureValue shouldBe AlreadyCreated()
  }
  "Exist. New user" should "exist in db" in {
    api.db.run(users.filter(_.telegramId === "new").result)
      .futureValue shouldBe Vector(User("new", "new", Some(5)))
  }

  "Delete. Existing user " should "delete him from db" in {
    api.deleteUser(User("new", "new")).futureValue shouldBe Deleted()
  }
  "Delete. Fake user" should "return Action[Already deleted]" in {
    api.deleteUser(User("fake", "fake")).futureValue shouldBe AlreadyDeleted()
  }

  "Create pool. When exist pool for chat" should "not create and return AlreadyCreated" in {
    api.createPool(User("new", "new"), "chatid2").futureValue shouldBe AlreadyCreated()
  }

  "Create pool. For fake chat with fake user" should "not create and return AlreadyCreated" in {
    api.createPool(User("fake", "fake"), "chatid2").futureValue shouldBe AlreadyCreated()
  }

  "Create pool. With fake creator" should "not create and return NotExistingUser" in {
    api.createPool(User("fake", "fake"), "chatid7").futureValue shouldBe NotExistingUser()
  }

  "Create pool. For chat" should "create pool for db and vote for creator" in {
    val action: Actions = api.createPool(User("mike", "010"), "chatid4").futureValue
    val id: Option[Long] = Await.result(api.db.run(pools.filter(_.chatId === "chatid4").
      map(_.id).result.headOption), Duration.Inf)
    val vote: Option[Vote] = Await.result(api.db.run(votes.filter(_.poolId === id).
      result.headOption), Duration.Inf)
    (action, vote) shouldBe(
      Created(),
      Some(Vote(4, 4, defaultString,  Some(5))))

  }

  "VoteInsertOrUpdate.NotExistingUserAndPool " should
    " do nothing with db and return NotExistingUserAndPool()" in {
    api.voteInsertOrUpdate(User("fake", "fake"), "chinese", "fake").
      futureValue shouldBe NotExistingUserAndPool()

  }

  "VoteInsertOrUpdate.NotExistingUser " should
    " do nothing with db and return NotExistingUser()" in {
    api.voteInsertOrUpdate(User("fake", "fake"), "chinese", "chatid2").
      futureValue shouldBe NotExistingUser()
  }

  "VoteInsertOrUpdate.NotExistingPool " should
    " do nothing with db and return NotExistingPool()" in {
    api.voteInsertOrUpdate(User("mike", "010"), "chinese", "fake").
      futureValue shouldBe NotExistingPool()

  }

  "VoteInsertOrUpdate.Update " should
    " update  row in db and return Completed()" in {
    val previousChoice = api.db.run(
      votes.filter(x => x.poolId === 1L && x.userId === 1L).map(_.choice).result
    ).futureValue.head

    val action: Actions = Await.result(
      api.voteInsertOrUpdate(User("nikita", "123"), "france", "chatid1"),
      Duration.Inf)
    val currChoice = api.db.run(votes.filter(x => x.poolId === 1L && x.userId === 1L).map(_.choice).result
    ).futureValue.head

    (previousChoice, action, currChoice) shouldBe("chinese", Completed(), "france")

  }


  "VoteInsertOrUpdate.Create " should
    " create  row in db and return Completed()" in {

    val action: Actions = api.voteInsertOrUpdate(User("nikita", "123"), "italy", "chatid3").futureValue
    val currChoice = api.db.run(votes.filter(x => x.poolId === 3L && x.userId === 1L).map(_.choice).result
    ).futureValue.head
    (action, currChoice) shouldBe(Completed(), "italy")


  }


  "UpdatePool.Seq of new users" should
    " create votes for them and return Complete for all users" in {
    api.updatePool("chatid4", Seq(
      User(telegramId = "nikita", privateChatId = "123"),
      User(telegramId = "vova", privateChatId = "456"),
      User(telegramId = "semen", privateChatId = "789")
    )).foreach(_.futureValue shouldBe Completed())
  }
  "UpdatePool.Seq of fake users" should
    " not create votes for them and return NotExistingUser for all users" in {
    api.updatePool("chatid4", Seq(
      User(telegramId = "fake", privateChatId = "123"),
      User(telegramId = "fake", privateChatId = "456"),
      User(telegramId = "fake", privateChatId = "789")
    )).foreach(_.futureValue shouldBe NotExistingUser())
  }


  "ForceResult. From not existing chat" should "return NoExistingPool" in {
    api.forceResult("fake").futureValue shouldBe NotExistingPool()
  }
  "ForceResult. For chat " should "return Result" in {
    api.forceResult("chatid1").futureValue shouldBe Result("france")
  }
  "ForceResult. For new chat" should "return Result" in {
    api.voteInsertOrUpdate(User("nikita", "123"), "italy", "chatid4").futureValue
    api.forceResult("chatid4").futureValue shouldBe Result("italy")
  }



}




