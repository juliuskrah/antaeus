package io.pleo.antaeus.core.exceptions

class JobNotFoundException(name: String, group: String) :
        Exception("Job with key $group.$name was not found")