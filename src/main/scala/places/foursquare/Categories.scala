package places.foursquare

object Categories {
  lazy val cafeAndRestaurants = Category("4d4b7105d754a06374d81259")
  lazy val fastFood = Category("4bf58dd8d48988d16e941735")
  lazy val italian = Category("4bf58dd8d48988d110941735")
  lazy val japanese = Category("4bf58dd8d48988d111941735")

  lazy val all = Seq(cafeAndRestaurants, fastFood, italian, japanese)
}


