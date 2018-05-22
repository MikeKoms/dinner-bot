package places.foursquare

object Categories {
  lazy val fastFood = Category("4bf58dd8d48988d16e941735")
  lazy val italian = Category("4bf58dd8d48988d110941735")
  lazy val japanese = Category("4bf58dd8d48988d111941735")
  lazy val asian = Category("4bf58dd8d48988d142941735")
  lazy val chinese = Category("4bf58dd8d48988d145941735")
  lazy val burgers = Category("4bf58dd8d48988d16c941735")
  lazy val cofferoom = Category("4bf58dd8d48988d1e0931735")
  lazy val canteen = Category("4bf58dd8d48988d128941735")
  lazy val kebab = Category("5283c7b4e4b094cb91ec88d8")

  lazy val ids = Seq(fastFood, italian, japanese, asian, chinese, burgers, cofferoom, canteen, kebab)

  object Translatioins {
    lazy val russian =
      Seq(
        fastFood -> "Фастфуд",
        italian -> "Итальянские рестораны",
        japanese -> "Японские рестораны",
        asian -> "Азиатский ресторан",
        chinese -> "Китайский ресторан",
        burgers -> "Бургерная",
        cofferoom -> "Кофейня",
        canteen -> "Столовая",
        kebab -> "Шавуха"
      ).toMap[Category, String]

    lazy val inverse = russian.map(_.swap)
  }
}