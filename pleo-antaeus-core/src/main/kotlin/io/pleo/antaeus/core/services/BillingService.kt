package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus.*
import org.slf4j.LoggerFactory

class BillingService(
        private val paymentProvider: PaymentProvider,
        private val invoiceService: InvoiceService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun chargeInvoice(invoice: Invoice) {
        // Only bill PENDING invoices
        if (invoice.status == PENDING) {
            if (paymentProvider.charge(invoice)) {
                invoiceService.update(invoice.copy(status = PAID))
                // send email to customer about invoice charge
            } else {
                // Could not charge invoice on customer's account
                // Notify sales or admin for further action
                // send email to customer about failure
            }
        } else {
            log.info("invoice ${invoice.id} is paid. Skipping")
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return invoiceService.fetchAll()
    }

    fun fetchInvoice(id: Int): Invoice {
        return invoiceService.fetch(id)
    }
}