package io.pleo.antaeus.core.jobs

import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.schedule.services.SchedulingService
import mu.KotlinLogging
import org.quartz.Job
import org.quartz.JobBuilder.newJob
import org.quartz.JobExecutionContext
import org.quartz.TriggerBuilder.newTrigger

private val logger = KotlinLogging.logger {}

/**
 * This job is triggered at the beginning of each billing cycle
 */
internal class PendingInvoiceJob : Job {
    override fun execute(jobExecutionContext: JobExecutionContext) {
        val context = jobExecutionContext.scheduler.context
        val billingService = context[SchedulingService.BILLING_SERVICE] as BillingService
        logger.debug("Fetching invoices to be processed")
        // 1. All invoices are fetched from the database. We schedule each invoice for processing in order
        //    not to keep this thread running too long
        // 2. Use batching so as not to hold too much objects in memory
        billingService.fetchInvoices().forEach{
            // For each invoice schedule a job to process. The jobs are passed to a thread pool managed by quartz
            // Another option is to use coroutines
            jobExecutionContext.scheduler.scheduleJob(
                    newJob(BillingJob::class.java)
                            .usingJobData("invoiceId", it.id)
                            .withIdentity("bill-${it.id}", "antaeus")
                            .build(),
                    newTrigger().withIdentity("bill-${it.id}", "antaeus")
                            .startNow().build()
            )
        }
        logger.debug("Invoices fetched are being processed")
    }

}