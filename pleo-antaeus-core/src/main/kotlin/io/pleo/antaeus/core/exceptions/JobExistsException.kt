package io.pleo.antaeus.core.exceptions

class JobExistsException(name: String, group: String) :
        Exception("Job with key $group.$name already exists")