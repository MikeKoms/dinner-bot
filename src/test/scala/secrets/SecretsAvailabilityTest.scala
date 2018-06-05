package secrets

import org.scalatest.{FlatSpec, Matchers}

class SecretsAvailabilityTest extends FlatSpec with Matchers {

  it should "get CLIENT_ID from config" in {
    noException should be thrownBy Foursquare.CLIENT_ID
  }

  it should "get CLIENT_SECRET from config" in {
    noException should be thrownBy Foursquare.CLIENT_SECRET
  }

  it should "get TELEGRAM_TOKEN from config" in {
    noException shouldBe thrownBy (Telegram.TOKEN)
  }
}
