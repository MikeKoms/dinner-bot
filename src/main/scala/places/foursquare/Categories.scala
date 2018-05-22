package places.foursquare

object Categories {
  lazy val fastFood = Category("Fast food", "4bf58dd8d48988d16e941735")
  lazy val italian = Category("Italian food", "4bf58dd8d48988d110941735")
  lazy val japanese = Category("Japanese food", "4bf58dd8d48988d111941735")
  lazy val asian = Category("Asian restaurant", "4bf58dd8d48988d142941735")
  lazy val chinese = Category("Chinese restaurant", "4bf58dd8d48988d145941735")
  lazy val burgers = Category("Burgers", "4bf58dd8d48988d16c941735")
  lazy val cofferoom = Category("Cafe", "4bf58dd8d48988d1e0931735")
  lazy val canteen = Category("Canteen", "4bf58dd8d48988d128941735")
  lazy val kebab = Category("Kebab", "5283c7b4e4b094cb91ec88d8")

  object All {
    lazy val english = Seq(fastFood, italian, japanese, asian, chinese, burgers, cofferoom, canteen, kebab)
    lazy val russian = english.map(translateToRussian)
  }

  lazy val translations =
    Seq(
        fastFood           -> "Фастфуд",
        italian            -> "Итальянские рестораны",
        japanese           -> "Японские рестораны",
        asian              -> "Азиатский ресторан",
        chinese            -> "Китайский ресторан",
        burgers            -> "Бургерная",
        cofferoom          -> "Кофейня",
        canteen            -> "Столовая",
        kebab              -> "Шавуха"
      ).map(p => p._1.id -> p._2).toMap[String, String]

  def translateToRussian(cat: Category) = cat.copy(name = translations(cat.id))
}