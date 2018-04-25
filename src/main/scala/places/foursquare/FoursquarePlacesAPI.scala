package places.foursquare

import java.text.SimpleDateFormat
import java.util.Calendar

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer

import scala.concurrent.Future


class FoursquarePlacesAPI extends VenueJsonSupport {
  private implicit val system = ActorSystem()
  private implicit val dispatcher = system.dispatcher
  private implicit val materializer = ActorMaterializer()

  private val API_REQUEST_PATH = "https://api.foursquare.com/v2/venues/search"
  private val CLIENT_ID = ???
  private val CLIENT_SECRET = ???
  private val date = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime)

  case class RespRoot(meta: Meta, response: Response)
  case class Meta(code: Int)
  case class Response(venues: Seq[Venue])

  implicit val metaFormat = jsonFormat1(Meta)
  implicit val responseFormat = jsonFormat1(Response)
  implicit val respFormat = jsonFormat2(RespRoot)



  def placesQuery(lat: BigDecimal,
                  lng: BigDecimal,
                  radius: Long,
                  category: Option[Category]): Future[RespRoot] = {
    val params = Map(
      "client_id" -> CLIENT_ID,
      "client_secret" -> CLIENT_SECRET,
      "ll" -> s"$lat,$lng",
      "radius" -> s"$radius",
      "v" -> date) ++
      category.map("categoryId" -> _.id).toMap

    val resp = Http().singleRequest(HttpRequest(
      uri = Uri(API_REQUEST_PATH).withQuery(Query(params))
    ))

    resp.flatMap(resp => Unmarshal(resp.entity).to[RespRoot])
  }
}
