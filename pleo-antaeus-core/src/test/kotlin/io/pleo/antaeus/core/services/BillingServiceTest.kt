package io.pleo.antaeus.core.services

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
class BillingServiceTest {
    @MockK
    lateinit var paymentProvider: PaymentProvider
    @MockK
    lateinit var invoiceService: InvoiceService
    private val invoice = Invoice(1, 1, Money(BigDecimal
            .valueOf(500L), currency = Currency.DKK), InvoiceStatus.PENDING)

    @BeforeEach
    internal fun setUp() {
        every { paymentProvider.charge(any()) } returns true
        every { invoiceService.update(any()) } returns invoice.copy(status = InvoiceStatus.PAID)
        every { invoiceService.fetch(any()) } returns invoice
    }

    @Test
    fun `should set invoice status to paid when charge is true`() {
        val billingService = BillingService(paymentProvider, invoiceService)
        billingService.chargeInvoice(invoice)

        verify { paymentProvider.charge(invoice) }
        verify { invoiceService.update(invoice.copy(status = InvoiceStatus.PAID)) }
        confirmVerified(paymentProvider, invoiceService)
    }

    @Test
    fun `should fetch invoice by id`() {
        val billingService = BillingService(paymentProvider, invoiceService)
        val fetchedInvoice = billingService.fetchInvoice(2)

        verify { invoiceService.fetch(2) }
        confirmVerified(invoiceService)

        assertEquals(fetchedInvoice, invoice)
    }
}