package com.example.tsumobilkabeta.Clustering.model
import com.example.tsumobilkabeta.Genetic.FoodDatabase

data class Point(
    val x: Double,
    val y: Double
)

data class Establishments(
    val id: Int,
    val coordinate: Point,
    val name: String
)

object DataProvider{
    fun getEstablishments(): List<Establishments>{
        return FoodDatabase.allEstablishments.map{food ->
            Establishments(
                id=food.id,
                name=food.name,
                coordinate = Point(
                    x=food.location.longitude,
                    y=food.location.latitude
                )
            )
        }
    }
}


