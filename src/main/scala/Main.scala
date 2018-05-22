import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import places.foursquare.{Categories, FoursquareService}

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val dispatcher = system.dispatcher
    implicit val materializer = ActorMaterializer()
    val serv = new FoursquareService()
    val rad = serv.categoriesInRadius(59.9343, 30.3351, 1000, Seq(Categories.chinese))
    rad.foreach(println)
  }
}
