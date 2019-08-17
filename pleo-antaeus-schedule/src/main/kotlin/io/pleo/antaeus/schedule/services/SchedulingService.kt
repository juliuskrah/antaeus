package io.pleo.antaeus.schedule.services

import io.pleo.antaeus.core.exceptions.JobExistsException
import io.pleo.antaeus.core.exceptions.JobNotFoundException
import io.pleo.antaeus.core.jobs.PendingInvoiceJob
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.models.Job
import io.pleo.antaeus.models.Trigger
import mu.KotlinLogging
import org.quartz.CronScheduleBuilder.cronSchedule
import org.quartz.JobBuilder.newJob
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.SchedulerException
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.impl.StdSchedulerFactory
import org.quartz.impl.matchers.GroupMatcher.anyJobGroup

private val logger = KotlinLogging.logger {}

class SchedulingService(
     billingService: BillingService
) {
    companion object {
        const val BILLING_SERVICE = "billingService"
    }
    private val scheduler: Scheduler = StdSchedulerFactory.getDefaultScheduler()

    init {
        // Shortcut to pass objects to the scheduler. In a full blown app,
        // this will probably be handled by a DI library
        scheduler.context[BILLING_SERVICE] = billingService

        if (!scheduler.isStarted) scheduler.start()
    }

    fun close() {
        logger.info("Shutting down scheduler")
        if (!scheduler.isShutdown) scheduler.shutdown()
    }

    fun scheduleInvoicePayment() {
        val jobDetail = Job(name = "invoices")
        val key = JobKey.jobKey(jobDetail.name, jobDetail.group)
        // Create job only if job with given key does not exist in the database
        if (!scheduler.checkExists(key)) {
            scheduler.scheduleJob(buildJobDetail(jobDetail), buildTriggers(jobDetail), false)
        }
    }

    fun fetchAll(): Set<Job> {
        val jobs = LinkedHashSet<Job>()
        try {
            val keys: Set<JobKey> = scheduler.getJobKeys(anyJobGroup())
            for (key: JobKey in keys) {
                val jobDetail = scheduler.getJobDetail(key)
                val triggers = scheduler.getTriggersOfJob(key)
                val triggersVm = triggers.map {
                    Trigger(name = it.key.name, group = it.key.group, cron = it.startTime?.toString()!!)
                }
                val job = Job(name = jobDetail.key.name, group = jobDetail.key.group, triggers = triggersVm)
                jobs.add(job)
            }
        } catch (ex: SchedulerException) {
            // Exception is handled by Javalin
            throw RuntimeException(ex)
        }
        return jobs
    }

    fun createJob(jobDetail: Job): Job {
        val key = JobKey.jobKey(jobDetail.name, jobDetail.group)
        if (scheduler.checkExists(key))
            throw JobExistsException(key.name, key.group)
        scheduler.scheduleJob(buildJobDetail(jobDetail), buildTriggers(jobDetail), true)
        return jobDetail
    }

    fun updateJob(jobDetail: Job): Job {
        val key = JobKey.jobKey(jobDetail.name, jobDetail.group)
        if (!scheduler.checkExists(key))
            throw JobNotFoundException(key.name, key.group)
        scheduler.scheduleJob(buildJobDetail(jobDetail), buildTriggers(jobDetail), true)
        return jobDetail
    }

    private fun buildTriggers(jobDetail: Job) = jobDetail.triggers.map{
        newTrigger()
                .withIdentity(it.name, it.group)
                // In a real application, validate cron expression
                // CronExpression.validateExpression(it.cron)
                .withSchedule(cronSchedule(it.cron))
                .build()
    }.toSet()


    private fun buildJobDetail(jobDetail: Job) = newJob()
            .ofType(PendingInvoiceJob::class.java)
            .withIdentity(jobDetail.name, jobDetail.group)
            .build()
}