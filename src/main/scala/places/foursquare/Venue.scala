package places.foursquare

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

case class Venue(name: String, location: Location, categories: Seq[Category]) {
  override def toString: String =
    (name +: location.toString +: categories.map(_.toString)).mkString("\n")
}

case class Location(lat: BigDecimal, lng: BigDecimal, distance: Int, formattedAddress: Seq[String]) {
  override def toString: String =
    (s"Расстояние: $distance" +: formattedAddress).mkString("\n")
}

case class Category(id: String) {
  override def toString: String = Categories.Translatioins.russian(this)
}

trait VenueJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  private implicit val categoryFormat = jsonFormat1(Category)
  private implicit val locationFormat = jsonFormat4(Location)
  implicit val venueFormat = jsonFormat3(Venue)
}
