import places.foursquare.FoursquarePlacesAPI

import scala.concurrent.Await
import scala.concurrent.duration._

object Sample extends App {
  val api = new FoursquarePlacesAPI()

  val result = api.placesQuery(59.9343, 30.3351, 10000, None)
  Await.result(result, 10.seconds)
  println(result)
}
