package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus.*
import org.slf4j.LoggerFactory

/**
 * Billing Service contains methods to fetch and charge invoices
 */
class BillingService(
        private val paymentProvider: PaymentProvider,
        private val invoiceService: InvoiceService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Charge a customer's account the amount from the invoice. Rethrows exceptions from {@link PaymentProvider#charge}
     *
     * @throws
     *      `CustomerNotFoundException`: when no customer has the given id.
     *      `CurrencyMismatchException`: when the currency does not match the customer account.
     *      `NetworkException`: when a network error happens.
     */
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

    /**
     * Fetch all invoices
     *
     * @return all invoices
     */
    fun fetchInvoices(): List<Invoice> {
        return invoiceService.fetchAll()
    }

    /**
     * Fetch an invoice by id
     *
     * @return
     *      the invoice identified by the given id.
     * @throws
     *      `InvoiceNotFoundException`: when no invoice has the given id.
     */
    fun fetchInvoice(id: Int): Invoice {
        return invoiceService.fetch(id)
    }
}