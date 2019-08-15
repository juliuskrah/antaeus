package io.pleo.antaeus.models

data class Trigger (
        val group: String = "antaeus",
        val name: String,
        val cron: String
)