package com.example.gettahoofin

data class Program(
    val name: String,
    val description: String,
    val numberOfWeeks: Int,
    val weeks: List<Week>
)