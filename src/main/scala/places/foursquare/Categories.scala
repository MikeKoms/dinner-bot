package places.foursquare

object Categories {
  lazy val cafeAndRestaurants = Category("Cafe and restaurants", "4d4b7105d754a06374d81259")
  lazy val fastFood = Category("Fast food", "4bf58dd8d48988d16e941735")
  lazy val italian = Category("Italian food", "4bf58dd8d48988d110941735")
  lazy val japanese = Category("Japanese food", "4bf58dd8d48988d111941735")
  lazy val asian = Category("Asian restaurant", "4bf58dd8d48988d142941735")
  lazy val chinese = Category("Chinese restaurant", "4bf58dd8d48988d145941735")
  lazy val burgers = Category("Burgers", "4bf58dd8d48988d16c941735")
  lazy val cafe = Category("Cafe", "4bf58dd8d48988d16d941735")
  lazy val canteen = Category("Canteen", "4bf58dd8d48988d128941735")

  object All {
    lazy val english = Seq(cafeAndRestaurants, fastFood, italian, japanese, asian, chinese)
    lazy val russian = english.map(translateToRussian)
  }

  private lazy val translations =
    Seq(
        cafeAndRestaurants -> "Кафе и рестораны",
        fastFood           -> "Фастфуд",
        italian            -> "Итальянские рестораны",
        japanese           -> "Японские рестораны",
        asian              -> "Азиатский ресторан",
        chinese            -> "Китайский ресторан"
      ).map(p => p._1.id -> p._2).toMap[String, String]

  def translateToRussian(cat: Category) =
    cat.copy(name = translations(cat.id))
}