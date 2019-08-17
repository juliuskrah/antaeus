package io.pleo.antaeus.schedule.services

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.OverrideMockKs
import io.mockk.junit5.MockKExtension
import io.pleo.antaeus.core.exceptions.JobExistsException
import io.pleo.antaeus.core.exceptions.JobNotFoundException
import io.pleo.antaeus.core.jobs.PendingInvoiceJob
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.models.Job
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.quartz.JobBuilder
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.TriggerBuilder

@ExtendWith(MockKExtension::class)
class SchedulingServiceTest {
    @MockK
    lateinit var billingService: BillingService
    @MockK(relaxed = true)
    lateinit var scheduler: Scheduler
    @OverrideMockKs
    lateinit var schedulingService: SchedulingService

    @Test
    fun `should not schedule when job exists`() {
        val key = JobKey("invoices", "antaeus")
        every { scheduler.checkExists(key)} returns true
        every { scheduler.scheduleJob( any(), any(), false) } just Runs

        schedulingService.scheduleInvoicePayment()

        verify { scheduler.checkExists(key) }
        verify(exactly = 0) { scheduler.scheduleJob(any(), setOf(), false) }
        confirmVerified(scheduler)
    }

    @Test
    fun `should schedule when job does not exists`() {
        val key = JobKey("invoices", "antaeus")
        every { scheduler.checkExists(key)} returns false
        every { scheduler.scheduleJob(any(), setOf(), false) } just Runs

        schedulingService.scheduleInvoicePayment()

        verify { scheduler.checkExists(key) }
        verify(exactly = 1) { scheduler.scheduleJob(any(), any(), false) }
        confirmVerified(scheduler)
    }

    @Test
    fun `should return scheduled jobs`() {
        val keys = setOf(JobKey("jobOne", "antaeus"),
                JobKey("jobTwo", "antaeus"))
        every { scheduler.getJobKeys(any()) } returns keys
        every { scheduler.getJobDetail(any()) } returns JobBuilder.newJob(PendingInvoiceJob::class.java).build()
        every { scheduler.getTriggersOfJob(any()) } returns listOf(TriggerBuilder.newTrigger().build())

        val jobs = schedulingService.fetchAll()

        verify(exactly = 2) { scheduler.getJobDetail(any()) }
        verify(exactly = 2) { scheduler.getTriggersOfJob(any()) }
        verify { scheduler.getJobKeys(any()) }
        confirmVerified(scheduler)

        assertEquals(1, jobs.size) // No duplicates in Set
    }

    @Test
    fun `should throw if job exist when creating`() {
        val key = JobKey("job", "antaeus")
        every { scheduler.checkExists(key) } returns true
        assertThrows<JobExistsException> { schedulingService.createJob(Job(name = "job")) }

        verify { scheduler.checkExists(key) }
        confirmVerified(scheduler)
    }

    @Test
    fun `should create job if not exists`() {
        val key = JobKey("job", "antaeus")
        every { scheduler.checkExists(key) } returns false
        every { scheduler.scheduleJob( any(), any(), true) } just Runs

        schedulingService.createJob(Job(name = "job"))

        verify { scheduler.checkExists(key) }
        verify { scheduler.scheduleJob(any(), any(), true) }
        confirmVerified(scheduler)
    }

    @Test
    fun `should throw if job does not exist when updating`() {
        val key = JobKey("job", "antaeus")
        every { scheduler.checkExists(key) } returns false
        assertThrows<JobNotFoundException> { schedulingService.updateJob(Job(name = "job")) }

        verify { scheduler.checkExists(key) }
        confirmVerified(scheduler)
    }

    @Test
    fun `should update job if exists`() {
        val key = JobKey("job", "antaeus")
        every { scheduler.checkExists(key) } returns true
        every { scheduler.scheduleJob( any(), any(), true) } just Runs

        schedulingService.updateJob(Job(name = "job"))

        verify { scheduler.checkExists(key) }
        verify { scheduler.scheduleJob(any(), any(), true) }
        confirmVerified(scheduler)
    }
}