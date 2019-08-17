package io.pleo.antaeus.schedule.jobs

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.pleo.antaeus.core.jobs.PendingInvoiceJob
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.schedule.services.SchedulingService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.quartz.JobExecutionContext
import org.quartz.Scheduler
import org.quartz.SchedulerContext
import java.math.BigDecimal
import java.util.*

@ExtendWith(MockKExtension::class)
class PendingInvoiceJobTest {
    @MockK
    lateinit var jobExecutionContext: JobExecutionContext
    @MockK
    lateinit var billingService: BillingService
    @MockK(relaxed = true)
    lateinit var scheduler: Scheduler
    lateinit var schedulerContext: SchedulerContext

    @Test
    fun `should execute pending invoices job`() {
        val invoice = Invoice(1, 1, Money(BigDecimal
                .valueOf(500L), currency = Currency.DKK), InvoiceStatus.PENDING)
        schedulerContext = SchedulerContext().apply {
            this[SchedulingService.BILLING_SERVICE] = billingService
        }

        every { jobExecutionContext.scheduler } returns scheduler
        every { scheduler.context } returns schedulerContext
        every { scheduler.scheduleJob(any(), any()) } returns Date()
        every { billingService.fetchInvoices() } returns listOf(invoice, invoice)

        PendingInvoiceJob().execute(jobExecutionContext)

        verify { jobExecutionContext.scheduler }
        verify { scheduler.context }
        verify(exactly = 2) { scheduler.scheduleJob(any(), any()) }
        verify { billingService.fetchInvoices() }
        confirmVerified(jobExecutionContext, scheduler, billingService)
    }

}