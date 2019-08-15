package io.pleo.antaeus.core.jobs

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.schedule.services.SchedulingService
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

/**
 * Actual billing is handled with this job
 */
internal class BillingJob : Job {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * {@inheritDoc}
     */
    override fun execute(jobExecutionContext: JobExecutionContext) {
        val invoiceId = jobExecutionContext.mergedJobDataMap.getIntValue("invoiceId")
        val context = jobExecutionContext.scheduler.context
        val billingService = context[SchedulingService.BILLING_SERVICE] as BillingService

        log.debug("Job for invoice {} started", invoiceId)
        try {
            val invoice = billingService.fetchInvoice(invoiceId)
            billingService.chargeInvoice(invoice)
        } catch (ex: CustomerNotFoundException) {
            log.error("customer ${ex.id} not found", ex)
            // Customer not found, notify admin for further action or delete customer
            // Business decision will dictate what to do actually
        } catch (ex: CurrencyMismatchException) {
            log.error("currency in invoice ${ex.invoiceId} does not match currency for customer ${ex.customerId}", ex)
            // Currency mismatch, call an external service for currency conversion or notify sales team for further action
        } catch (ex: NetworkException) {
            log.warn("Network error, retrying")
            val retry: Int = jobExecutionContext.mergedJobDataMap.getIntValue("retries")
            handleNetworkError(jobExecutionContext, retry)
        } catch (ex: InvoiceNotFoundException) {
            log.info("No invoice matching invoice with id $invoiceId")
            // Invoice does not exist. Push this to an offline processing system such as Hadoop for further processing
            // How did an invalid invoice end up in our system?
        }
    }

    // TODO exponential backoff
    private fun handleNetworkError(jobExecutionContext: JobExecutionContext, retry: Int = 0) {
        var retryForAvailability: Int = retry
        var maxRetries: Int = System.getenv("MAX_RETRIES")?.toInt() ?: "3".toInt()
        if (retry < maxRetries) {
            retryForAvailability++
            jobExecutionContext.mergedJobDataMap["retries"] = retryForAvailability
            execute(jobExecutionContext)
        }
    }
}