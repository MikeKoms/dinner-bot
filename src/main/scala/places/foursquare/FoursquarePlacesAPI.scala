package places.foursquare

import java.text.SimpleDateFormat
import java.util.Calendar

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.stream.ActorMaterializer
import secrets._

import scala.concurrent.Future

trait Json extends VenueJsonSupport {

  case class Root(meta: Meta, response: Response)

  case class Meta(code: Int)

  case class Response(venues: Seq[Venue])

  private implicit val metaFormat = jsonFormat1(Meta)
  private implicit val responseFormat = jsonFormat1(Response)
  implicit val rootFormat = jsonFormat2(Root)
}

private[foursquare]
class FoursquarePlacesAPI(val client: HttpClient) extends Json {
  private implicit val system = ActorSystem()
  private implicit val dispatcher = system.dispatcher
  private implicit val materializer = ActorMaterializer()

  private val API_REQUEST_PATH = "https://api.foursquare.com/v2/venues/search"
  private val CLIENT_ID = Foursquare.CLIENT_ID
  private val CLIENT_SECRET = Foursquare.CLIENT_SECRET
  private val date = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime)

  def query(lat: BigDecimal,
            lng: BigDecimal,
            radius: Long,
            categories: Seq[Category]): Future[Root] = {

    val catParams =
      if (categories.isEmpty) {
        Map[String, String]()
      } else {
        Map("categoryId" -> categories.map(_.id).mkString(","))
      }

    val params = Map(
      "client_id"     -> CLIENT_ID,
      "client_secret" -> CLIENT_SECRET,
      "ll"            -> s"$lat,$lng",
      "radius"        -> radius.toString,
      "v"             -> date,
      "limit"         -> 1.toString,
      "intent"        -> "browse"
    ) ++ catParams

    val req = HttpRequest(uri = Uri(API_REQUEST_PATH).withQuery(Query(params)))

    client.request[Root](req)
  }
}
