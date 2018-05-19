package places.foursquare

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

case class Venue(name: String, location: Location, categories: Seq[Category])
case class Location(lat: BigDecimal, lng: BigDecimal)
case class Category(id: String)

trait VenueJsonSupport extends DefaultJsonProtocol with SprayJsonSupport{
  private implicit val categoryFormat = jsonFormat1(Category)
  private implicit val locationFormat = jsonFormat2(Location)
  implicit val venueFormat = jsonFormat3(Venue)
  implicit val venueUnmarshaller = venueFormat.asScala
}
