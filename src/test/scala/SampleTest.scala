
import database._
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object SampleTest {
  def main(args: Array[String]): Unit = {
    val user = User("a","s")
    val db = Database.forConfig("prod")
    try {
      val users = TableQuery[Users]
      val pools = TableQuery[Pools]
      val votes = TableQuery[Votes]
      //val votesUsers = TableQuery[VotesUsers]
      val schema = users.schema ++ votes.schema ++pools.schema //# ++ votesUsers.schema
      schema.create.statements.foreach(println)
            val setup = DBIO.seq(
              //users  +=User ("123","123")
              schema.create,
              users += User( telegramId = "nikita", privateChatId = "123"),
              users += User( telegramId = "vova", privateChatId = "456"),
              users += User(telegramId = "semen", privateChatId = "789"),
              users += User(telegramId = "mike", privateChatId = "0"),
              pools += Pool(1,"1","first",isFinished = false),
              pools += Pool(2,"2","second",isFinished = false),

              votes += Vote(1,1,"sold",isVoted = true),
              votes += Vote(1,2,"sold",isVoted = true) ,
              votes += Vote(2,3,"bra",isVoted = true),
              votes += Vote(2,4,"bra",isVoted = true)

        //votesUsers += VoteUser(1,1),
        //votesUsers += VoteUser(1,2),

        //votesUsers += VoteUser(1,3),
        //votesUsers += VoteUser(1,4)

      )
      val setupFuture = db.run(setup).flatMap { _ =>
        println("USers")
        db.run(users.result).map(_.foreach{
          case User(a,b,c) =>println(s"$a | $b | $c  ")
        })
      }.flatMap{_=>
        println("Pools")
        db.run(pools.result).map(_.foreach{
          case Pool(a,b,c,d,e) =>println(s"$a | $b |$c | $d |$e ")
        })
      }.flatMap{_=>
        println("Votes")
        db.run(votes.result).map(_.foreach{
          case Vote(a,b,c,d,e) =>println(s"$a | $b |$c | $d |$e ")
        })
      }
      Await.result(setupFuture,Duration.Inf)


      val q1 = for(c <- users)
        yield LiteralColumn("  ") ++ c.telegramId ++ "\t" ++ c.id.asColumnOf[String] ++
          "\t" ++ c.privateChatId.asColumnOf[String]
      Await.result(db.stream(q1.result).foreach(println),Duration.Inf)

    }
    finally db.close()
  }

}