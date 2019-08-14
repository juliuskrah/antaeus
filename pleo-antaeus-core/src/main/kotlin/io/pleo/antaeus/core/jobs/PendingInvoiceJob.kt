package io.pleo.antaeus.core.jobs

import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.SchedulingService
import org.quartz.Job
import org.quartz.JobBuilder.newJob
import org.quartz.JobExecutionContext
import org.quartz.TriggerBuilder.newTrigger
import org.slf4j.LoggerFactory

/**
 * This job is triggered at the beginning of each billing cycle
 */
internal class PendingInvoiceJob : Job {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun execute(jobExecutionContext: JobExecutionContext) {
        val context = jobExecutionContext.scheduler.context
        val billingService = context[SchedulingService.BILLING_SERVICE] as BillingService
        log.debug("Fetching invoices to be processed")
        // All invoices are fetched from the database. We schedule each invoice for processing in order
        // not to keep this thread running too long
        // TODO flush buffer after every 'N' records fetched
        billingService.fetchInvoices().forEach{
            jobExecutionContext.scheduler.scheduleJob(
                    newJob(BillingJob::class.java)
                            .usingJobData("invoiceId", it.id)
                            .withIdentity("dummy-${it.id}").build(),
                    newTrigger().withIdentity("dummy-${it.id}").startNow().build()
            )
        }
        log.debug("Invoices fetched are being processed")
    }

}