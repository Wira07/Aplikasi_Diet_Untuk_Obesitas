package com.farhan.aplikasidietuntukobesitas.data

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val weight: Double = 0.0,
    val height: Double = 0.0,
    val age: Int = 0,
    val gender: String = "",
    val bmi: Double = 0.0
)