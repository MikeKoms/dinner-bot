package places.foursquare

import scala.concurrent.Future

class FoursquareService {

  import ctx.ImplicitContext._

  private val api = new FoursquarePlacesAPI(new JsonHttpClient())

  def categoriesInRadius(lat: BigDecimal, lng: BigDecimal, radius: Long,
                         categories: Seq[Category] = Seq())
  : Future[Option[Venue]] = {
    val root = api.query(lat, lng, radius, categories)
    root.map(_.response.venues.headOption)
  }
}
