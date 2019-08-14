package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.jobs.PendingInvoiceJob
import org.quartz.JobBuilder
import org.quartz.Scheduler
import org.quartz.SimpleScheduleBuilder
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory
import org.slf4j.LoggerFactory

class SchedulingService(
     billingService: BillingService
) {
    companion object {
        const val BILLING_SERVICE = "billingService"

        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)

    }
    private val scheduler: Scheduler = StdSchedulerFactory.getDefaultScheduler()

    init {
        scheduler.context[BILLING_SERVICE] = billingService

        if (!scheduler.isStarted) scheduler.start()
    }

    fun close() {
        log.info("Shutting down scheduler")
        if (!scheduler.isShutdown) scheduler.shutdown()
    }

    fun scheduleInvoicePayment() {
        // TODO in a clustered environment, fetch job by key. If not exists, create job
        scheduler.scheduleJob(buildJobDetail(), buildTrigger())
    }

    // TODO Create data class to map trigger(s)
    private fun buildTrigger() = TriggerBuilder
            .newTrigger()
            .withIdentity("dummy")
            // TODO change to 1st of each month
            //.withSchedule(CronScheduleBuilder.monthlyOnDayAndHourAndMinute(1, 12, 0))
            .withSchedule(SimpleScheduleBuilder.repeatMinutelyForever())
            .build()

    // TODO Create data class to map Job Detail
    private fun buildJobDetail() = JobBuilder
            .newJob()
            .ofType(PendingInvoiceJob::class.java)
            .withIdentity("dummy")
            .usingJobData("key", "value")
            .build()
}