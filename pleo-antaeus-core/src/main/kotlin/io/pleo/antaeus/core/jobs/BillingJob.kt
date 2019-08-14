package io.pleo.antaeus.core.jobs

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.SchedulingService
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

/**
 * Actual billing is handled with this job
 */
internal class BillingJob : Job {
    private val log = LoggerFactory.getLogger(javaClass)

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
            // TODO handle customer not found
        } catch (ex: CurrencyMismatchException) {
            log.error("currency in invoice ${ex.invoiceId} does not match currency for customer ${ex.customerId}", ex)
            // TODO handle currency mismatch
        } catch (ex: NetworkException) {
            log.warn("Network error, retrying")
            // TODO handle network error
        } catch (ex: InvoiceNotFoundException) {
            log.info("No invoice matching invoice with id $invoiceId")
            // TODO handle invoice not found
        }
    }
}