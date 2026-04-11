package com.example.tsumobilkabeta.Genetic

import com.yandex.mapkit.geometry.Point

data class MenuItem(val name: String, val category: FoodCategory)

enum class FoodCategory {
    MAIN_DISH, SIDE_DISH, DRINK, DESSERT, SUPPLIES
}

data class FoodEstablishment(
    val id: Int,
    val name: String,
    val location: Point,
    val menu: List<MenuItem>,
    val openHour: Int,
    val openMinute: Int,
    val closeHour: Int,
    val closeMinute: Int
) {
    fun isOpenAt(hour: Int, minute: Int): Boolean {
        val now   = hour * 60 + minute
        val open  = openHour  * 60 + openMinute
        val close = closeHour * 60 + closeMinute
        return now in open until close
    }

    fun minutesUntilClose(hour: Int, minute: Int): Int =
        closeHour * 60 + closeMinute - (hour * 60 + minute)
}

object FoodDatabase {

    val allEstablishments = listOf(
        FoodEstablishment(
            id = 0, name = "Старбакс",
            location = Point(56.46966, 84.94598),
            menu = listOf(
                MenuItem("Капучино",  FoodCategory.DRINK),
                MenuItem("Латте",     FoodCategory.DRINK),
                MenuItem("Американо", FoodCategory.DRINK),
                MenuItem("Чизкейк",   FoodCategory.DESSERT),
                MenuItem("Круассан",  FoodCategory.DESSERT)
            ),
            openHour = 8, openMinute = 0, closeHour = 22, closeMinute = 0
        ),
        FoodEstablishment(
            id = 1, name = "Сибирские блины",
            location = Point(56.46945, 84.94658),
            menu = listOf(
                MenuItem("Блины с мясом",    FoodCategory.MAIN_DISH),
                MenuItem("Блины с творогом", FoodCategory.MAIN_DISH),
                MenuItem("Блины с вареньем", FoodCategory.DESSERT),
                MenuItem("Окрошка",          FoodCategory.MAIN_DISH),
                MenuItem("Чай",              FoodCategory.DRINK)
            ),
            openHour = 9, openMinute = 0, closeHour = 20, closeMinute = 0
        ),
        FoodEstablishment(
            id = 2, name = "Ростикс",
            location = Point(56.46919, 84.95106),
            menu = listOf(
                MenuItem("Куриные крылья", FoodCategory.MAIN_DISH),
                MenuItem("Бургер",         FoodCategory.MAIN_DISH),
                MenuItem("Картофель фри",  FoodCategory.SIDE_DISH),
                MenuItem("Кола",           FoodCategory.DRINK),
                MenuItem("Мороженое",      FoodCategory.DESSERT)
            ),
            openHour = 10, openMinute = 0, closeHour = 22, closeMinute = 0
        ),
        FoodEstablishment(
            id = 3, name = "Столовая ТГУ",
            location = Point(56.46952, 84.94651),
            menu = listOf(
                MenuItem("Борщ",            FoodCategory.MAIN_DISH),
                MenuItem("Пюре с котлетой", FoodCategory.MAIN_DISH),
                MenuItem("Салат Цезарь",    FoodCategory.SIDE_DISH),
                MenuItem("Компот",          FoodCategory.DRINK),
                MenuItem("Булочка",         FoodCategory.DESSERT)
            ),
            openHour = 8, openMinute = 30, closeHour = 17, closeMinute = 0
        ),
        FoodEstablishment(
            id = 4, name = "Кафе Минутка",
            location = Point(56.46961, 84.94649),
            menu = listOf(
                MenuItem("Пицца",           FoodCategory.MAIN_DISH),
                MenuItem("Сосиска в тесте", FoodCategory.MAIN_DISH),
                MenuItem("Сок",             FoodCategory.DRINK),
                MenuItem("Пирожок",         FoodCategory.DESSERT)
            ),
            openHour = 8, openMinute = 0, closeHour = 19, closeMinute = 0
        ),
        FoodEstablishment(
            id = 5, name = "Ресто Место",
            location = Point(56.47148, 84.95224),
            menu = listOf(
                MenuItem("Стейк",           FoodCategory.MAIN_DISH),
                MenuItem("Паста Карбонара", FoodCategory.MAIN_DISH),
                MenuItem("Греческий салат", FoodCategory.SIDE_DISH),
                MenuItem("Лимонад",         FoodCategory.DRINK),
                MenuItem("Тирамису",        FoodCategory.DESSERT)
            ),
            openHour = 11, openMinute = 0, closeHour = 23, closeMinute = 0
        ),
        FoodEstablishment(
            id = 6, name = "Сыр Бор",
            location = Point(56.47083, 84.94607),
            menu = listOf(
                MenuItem("Сырники",  FoodCategory.MAIN_DISH),
                MenuItem("Вареники", FoodCategory.MAIN_DISH),
                MenuItem("Морс",     FoodCategory.DRINK),
                MenuItem("Пельмени", FoodCategory.MAIN_DISH)
            ),
            openHour = 9, openMinute = 0, closeHour = 21, closeMinute = 0
        ),
        FoodEstablishment(
            id = 7, name = "Батина Шаурма",
            location = Point(56.47114, 84.94151),
            menu = listOf(
                MenuItem("Шаурма классическая", FoodCategory.MAIN_DISH),
                MenuItem("Шаурма острая",       FoodCategory.MAIN_DISH),
                MenuItem("Фалафель",            FoodCategory.MAIN_DISH),
                MenuItem("Айран",               FoodCategory.DRINK)
            ),
            openHour = 10, openMinute = 0, closeHour = 23, closeMinute = 0
        ),
        FoodEstablishment(
            id = 8, name = "Ярче",
            location = Point(56.47365, 84.94522),
            menu = listOf(
                MenuItem("Сэндвич",           FoodCategory.MAIN_DISH),
                MenuItem("Чипсы",             FoodCategory.SIDE_DISH),
                MenuItem("Вода",              FoodCategory.DRINK),
                MenuItem("Шоколадка",         FoodCategory.DESSERT),
                MenuItem("Одноразовая посуда", FoodCategory.SUPPLIES)
            ),
            openHour = 7, openMinute = 0, closeHour = 23, closeMinute = 0
        ),
        FoodEstablishment(
            id = 9, name = "Белка Кофе",
            location = Point(56.47147, 84.95016),
            menu = listOf(
                MenuItem("Эспрессо",         FoodCategory.DRINK),
                MenuItem("Раф",              FoodCategory.DRINK),
                MenuItem("Какао",            FoodCategory.DRINK),
                MenuItem("Маффин",           FoodCategory.DESSERT),
                MenuItem("Сэндвич с курицей", FoodCategory.MAIN_DISH)
            ),
            openHour = 8, openMinute = 0, closeHour = 21, closeMinute = 0
        ),
        FoodEstablishment(
            id = 10, name = "Петрушка",
            location = Point(56.47547, 84.95069),
            menu = listOf(
                MenuItem("Суп дня",      FoodCategory.MAIN_DISH),
                MenuItem("Гречка с мясом", FoodCategory.MAIN_DISH),
                MenuItem("Винегрет",     FoodCategory.SIDE_DISH),
                MenuItem("Кисель",       FoodCategory.DRINK)
            ),
            openHour = 9, openMinute = 0, closeHour = 18, closeMinute = 0
        ),
        FoodEstablishment(
            id = 11, name = "NOVA",
            location = Point(56.47551, 84.95034),
            menu = listOf(
                MenuItem("Роллы",       FoodCategory.MAIN_DISH),
                MenuItem("Рамен",       FoodCategory.MAIN_DISH),
                MenuItem("Эдамаме",     FoodCategory.SIDE_DISH),
                MenuItem("Зелёный чай", FoodCategory.DRINK),
                MenuItem("Моти",        FoodCategory.DESSERT)
            ),
            openHour = 11, openMinute = 0, closeHour = 22, closeMinute = 0
        ),
        FoodEstablishment(
            id = 12, name = "Подкова",
            location = Point(56.47050, 84.93922),
            menu = listOf(
                MenuItem("Плов",          FoodCategory.MAIN_DISH),
                MenuItem("Лагман",        FoodCategory.MAIN_DISH),
                MenuItem("Самса",         FoodCategory.MAIN_DISH),
                MenuItem("Чай с молоком", FoodCategory.DRINK)
            ),
            openHour = 10, openMinute = 0, closeHour = 21, closeMinute = 0
        ),
        FoodEstablishment(
            id = 13, name = "Пятёрочка",
            location = Point(56.46379, 84.95143),
            menu = listOf(
                MenuItem("Готовая еда (курица)", FoodCategory.MAIN_DISH),
                MenuItem("Хлеб",                FoodCategory.SIDE_DISH),
                MenuItem("Молоко",              FoodCategory.DRINK),
                MenuItem("Печенье",             FoodCategory.DESSERT),
                MenuItem("Одноразовая посуда",  FoodCategory.SUPPLIES),
                MenuItem("Салфетки",            FoodCategory.SUPPLIES)
            ),
            openHour = 7, openMinute = 0, closeHour = 23, closeMinute = 0
        ),
        FoodEstablishment(
            id = 14, name = "Бристоль",
            location = Point(56.47370, 84.94513),
            menu = listOf(
                MenuItem("Снеки",              FoodCategory.SIDE_DISH),
                MenuItem("Энергетик",          FoodCategory.DRINK),
                MenuItem("Вода газированная",  FoodCategory.DRINK),
                MenuItem("Сухарики",           FoodCategory.SIDE_DISH),
                MenuItem("Салфетки",           FoodCategory.SUPPLIES)
            ),
            openHour = 8, openMinute = 0, closeHour = 22, closeMinute = 0
        )
    )

    fun findEstablishmentsForItem(itemName: String): List<FoodEstablishment> =
        allEstablishments.filter { est -> est.menu.any { it.name == itemName } }

    fun getAllMenuItems(): List<String> =
        allEstablishments.flatMap { est -> est.menu.map { it.name } }.distinct().sorted()

    fun categoryDisplayName(category: FoodCategory) = when (category) {
        FoodCategory.MAIN_DISH -> "Основные блюда"
        FoodCategory.SIDE_DISH -> "Гарниры / закуски"
        FoodCategory.DRINK     -> "Напитки"
        FoodCategory.DESSERT   -> "Десерты"
        FoodCategory.SUPPLIES  -> "Принадлежности"
    }
}
