package io.pleo.antaeus.models

data class Job (
    val group: String = "antaeus",
    val name: String,
    val triggers: List<Trigger> = listOf(Trigger(name = "invoices", cron = "0 0 12 1 1/1 ? *"))
)