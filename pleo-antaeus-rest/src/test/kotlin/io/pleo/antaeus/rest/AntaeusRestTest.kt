package io.pleo.antaeus.rest

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.httpPut
import com.github.kittinunf.fuel.jackson.responseObject
import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.Job
import io.pleo.antaeus.schedule.services.SchedulingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AntaeusRestTest {

    @BeforeAll
    fun setUp() {
        val invoiceService = mockk<InvoiceService>()
        val customerService = mockk<CustomerService>()
        val schedulingService = mockk<SchedulingService>() {
            every { fetchAll() } returns setOf(Job(name = "job"))
            every { createJob(any()) } returns  Job(name = "job")
            every { updateJob(any()) } returns Job(name = "job")
        }

        AntaeusRest(
                customerService = customerService,
                invoiceService = invoiceService,
                schedulingService = schedulingService
        ).run()

        FuelManager.instance.basePath = "http://localhost:7000/"
    }

    @Test
    fun `should return list containing one item`() {
        val (_, _, result) = "rest/v1/jobs".httpGet().responseObject<Set<Job>>()
        assertEquals(1, result.get().size)
    }

    @Test
    fun `should return job when update`() {
        val (_, _, result) = "rest/v1/jobs".httpPut()
                .jsonBody("{ \"name\" : \"job\" }")
                .responseObject<Job>()
        assertEquals(Job(name="job"), result.get())
    }

    @Test
    fun `should return job when create`() {
        val (_, _, result) = "rest/v1/jobs".httpPost()
                .jsonBody("{ \"name\" : \"job\" }")
                .responseObject<Job>()
        assertEquals(Job(name="job"), result.get())
    }
}