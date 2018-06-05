package places.foursquare

import org.scalatest.{FlatSpec, Matchers}
import akka.http.scaladsl.unmarshalling.Unmarshal

class UnmarshallerTest extends FlatSpec with Json with Matchers {
  import ctx.ImplicitContext._

  it should "unmarshall JSON from 'https://developer.foursquare.com/docs/api/venues/search#response'" in {
    Unmarshal(jsonString).to[Root] should be
      Root(
        Meta(200),
        Response(
          Seq(
            Venue("Mr. Purple",
              Location(40.72173744277209, -73.98800687282996, 8,
                Seq(
                  "180 Orchard St (btwn Houston & Stanton St)",
                  "New York, NY 10002",
                  "United States"
                )
              ),
              Seq(Category("4bf58dd8d48988d1d5941735"))
            )
          )
        )
      )

    lazy val jsonString = """{
                       |  "meta": {
                       |    "code": 200,
                       |    "requestId": "5ac51d7e6a607143d811cecb"
                       |  },
                       |  "response": {
                       |    "venues": [
                       |      {
                       |        "id": "5642aef9498e51025cf4a7a5",
                       |        "name": "Mr. Purple",
                       |        "location": {
                       |          "address": "180 Orchard St",
                       |          "crossStreet": "btwn Houston & Stanton St",
                       |          "lat": 40.72173744277209,
                       |          "lng": -73.98800687282996,
                       |          "labeledLatLngs": [
                       |            {
                       |              "label": "display",
                       |              "lat": 40.72173744277209,
                       |              "lng": -73.98800687282996
                       |            }
                       |          ],
                       |          "distance": 8,
                       |          "postalCode": "10002",
                       |          "cc": "US",
                       |          "city": "New York",
                       |          "state": "NY",
                       |          "country": "United States",
                       |          "formattedAddress": [
                       |            "180 Orchard St (btwn Houston & Stanton St)",
                       |            "New York, NY 10002",
                       |            "United States"
                       |          ]
                       |        },
                       |        "categories": [
                       |          {
                       |            "id": "4bf58dd8d48988d1d5941735",
                       |            "name": "Hotel Bar",
                       |            "pluralName": "Hotel Bars",
                       |            "shortName": "Hotel Bar",
                       |            "icon": {
                       |              "prefix": "https://ss3.4sqi.net/img/categories_v2/travel/hotel_bar_",
                       |              "suffix": ".png"
                       |            },
                       |            "primary": true
                       |          }
                       |        ],
                       |        "venuePage": {
                       |          "id": "150747252"
                       |        }
                       |      }
                       |    ]
                       |  }
                       |}""".stripMargin
  }
}
