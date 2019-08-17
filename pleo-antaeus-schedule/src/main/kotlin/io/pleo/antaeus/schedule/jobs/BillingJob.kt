package io.pleo.antaeus.core.jobs

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.schedule.services.SchedulingService
import mu.KotlinLogging
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.SchedulerException

private val logger = KotlinLogging.logger {}
/**
 * Actual billing is handled with this job
 */
internal class BillingJob : Job {

    /**
     * {@inheritDoc}
     */
    override fun execute(jobExecutionContext: JobExecutionContext) {
        val invoiceId = jobExecutionContext.mergedJobDataMap.getIntValue("invoiceId")
        val context = jobExecutionContext.scheduler.context
        val billingService = context[SchedulingService.BILLING_SERVICE] as BillingService

        logger.debug("Job for invoice {} started", invoiceId)
        try {
            val invoice = billingService.fetchInvoice(invoiceId)
            billingService.chargeInvoice(invoice)
        } catch (ex: CustomerNotFoundException) {
            logger.error("customer ${ex.id} not found", ex)
            // Customer not found, notify admin for further action or delete customer
            // Business decision will dictate what to do actually
        } catch (ex: CurrencyMismatchException) {
            logger.error("currency in invoice ${ex.invoiceId} does not match currency for customer ${ex.customerId}", ex)
            // Currency mismatch, call an external service for currency conversion or notify sales team for further action
        } catch (ex: NetworkException) {
            logger.warn("Network error, retrying")
            val retry: Int = jobExecutionContext.mergedJobDataMap["retries"] as? Int ?: 0
            handleNetworkError(jobExecutionContext, retry)
        } catch (ex: InvoiceNotFoundException) {
            logger.info("No invoice matching invoice with id $invoiceId")
            // Invoice does not exist. Push this to an offline processing system such as Hadoop for further processing
            // How did an invalid invoice end up in our system?
        }
    }

    // In a real application, you'd implement the retry with exponential backoff so not to overwhelm
    // external system and allow time to recover
    private fun handleNetworkError(jobExecutionContext: JobExecutionContext, retry: Int = 0) {
        var retryForAvailability: Int = retry
        val maxRetries: Int = System.getenv("MAX_RETRIES")?.toInt() ?: 3
        if (retry < maxRetries) {
            retryForAvailability++
            jobExecutionContext.mergedJobDataMap["retries"] = retryForAvailability
            execute(jobExecutionContext)
            return
        }
        // Notify admin to check the
        throw JobExecutionException().apply { setUnscheduleAllTriggers(true) }
    }
}