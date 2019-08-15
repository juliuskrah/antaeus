/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.pleo.antaeus.core.exceptions.EntityNotFoundException
import io.pleo.antaeus.core.exceptions.JobExistsException
import io.pleo.antaeus.core.exceptions.JobNotFoundException
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.Job
import io.pleo.antaeus.schedule.services.SchedulingService
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class AntaeusRest (
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val schedulingService: SchedulingService
) : Runnable {

    override fun run() {
        app.start(7000)
    }

    // Set up Javalin rest app
    private val app = Javalin
        .create()
        .apply {
            // InvoiceNotFoundException: return 404 HTTP status code
            exception(EntityNotFoundException::class.java) { _, ctx ->
                ctx.status(404)
            }
            // JobNotFoundException: return 404 HTTP status code
            exception(JobNotFoundException::class.java) { _, ctx ->
                ctx.status(404)
            }
            // JobExistsException: return 409 HTTP status code
            exception(JobExistsException::class.java) { _, ctx ->
                ctx.status(409)
            }
            // Unexpected exception: return HTTP 500
            exception(Exception::class.java) { e, _ ->
                logger.error(e) { "Internal server error" }
            }
            // On 404: return message
            error(404) { ctx -> ctx.json("not found") }
        }

    init {
        // Set up URL endpoints for the rest app
        app.routes {
           path("rest") {
               // Route to check whether the app is running
               // URL: /rest/health
               get("health") {
                   it.json("ok")
               }

               // V1
               path("v1") {
                   path("invoices") {
                       // URL: /rest/v1/invoices
                       get {
                           it.json(invoiceService.fetchAll())
                       }

                       // URL: /rest/v1/invoices/{:id}
                       get(":id") {
                          it.json(invoiceService.fetch(it.pathParam("id").toInt()))
                       }
                   }

                   path("customers") {
                       // URL: /rest/v1/customers
                       get {
                           it.json(customerService.fetchAll())
                       }

                       // URL: /rest/v1/customers/{:id}
                       get(":id") {
                           it.json(customerService.fetch(it.pathParam("id").toInt()))
                       }
                   }

                   path("jobs") {
                       // URL: /rest/v1/jobs
                       get {
                           it.json(schedulingService.fetchAll())
                       }
                       // URL: /rest/v1/jobs
                       post {
                           val job: Job = it.body<Job>()
                           it.json(schedulingService.createJob(job))
                       }
                       // URL: /rest/v1/jobs/
                       put {
                           val job: Job = it.body<Job>()
                           it.json(schedulingService.updateJob(job))
                       }
                   }
               }
           }
        }
    }
}
