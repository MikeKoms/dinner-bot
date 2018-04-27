import com.typesafe.config.ConfigFactory

package object secrets {
  private lazy val secretConfig = ConfigFactory.load("secrets")

  object Foursquare {
    lazy val CLIENT_ID = secretConfig.getString("foursquare.client_id")
    lazy val CLIENT_SECRET = secretConfig.getString("foursquare.client_secret")
  }
}
