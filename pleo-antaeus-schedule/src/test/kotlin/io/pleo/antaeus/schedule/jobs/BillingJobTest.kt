package io.pleo.antaeus.schedule.jobs

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.jobs.BillingJob
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.schedule.services.SchedulingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.quartz.*
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
class BillingJobTest {
    @MockK
    lateinit var jobExecutionContext: JobExecutionContext
    @MockK
    lateinit var billingService: BillingService
    @MockK
    lateinit var scheduler: Scheduler
    lateinit var schedulerContext: SchedulerContext

    @BeforeEach
    fun setUp() {
        val invoice = Invoice(1, 1, Money(BigDecimal
                .valueOf(500L), currency = Currency.DKK), InvoiceStatus.PENDING)
        val jobDataMap = JobDataMap().apply {
            this["invoiceId"] = 1
        }
        schedulerContext = SchedulerContext().apply {
            this[SchedulingService.BILLING_SERVICE] = billingService
        }

        every { jobExecutionContext.scheduler } returns scheduler
        every { jobExecutionContext.mergedJobDataMap } returns jobDataMap
        every { scheduler.context } returns schedulerContext
        every { billingService.chargeInvoice(any()) } just Runs
        every { billingService.fetchInvoice(any()) } returns invoice
    }

    @Test
    fun `should execute billing job`() {
        BillingJob().execute(jobExecutionContext)

        verify { jobExecutionContext.scheduler }
        verify { jobExecutionContext.mergedJobDataMap }
        verify { scheduler.context }
        verify { billingService.chargeInvoice(any()) }
        verify { billingService.fetchInvoice(any()) }
        confirmVerified(jobExecutionContext, scheduler, billingService)
    }

    @Test
    fun `should retry on network error`() {
        every { billingService.chargeInvoice(any()) } throws NetworkException()

        assertThrows<JobExecutionException> {
            BillingJob().execute(jobExecutionContext)
        }

        verify { jobExecutionContext.scheduler }
        verify { jobExecutionContext.mergedJobDataMap }
        verify { scheduler.context }
        verify { billingService.chargeInvoice(any()) }
        verify { billingService.fetchInvoice(any()) }
        confirmVerified(jobExecutionContext, scheduler, billingService)
    }
}