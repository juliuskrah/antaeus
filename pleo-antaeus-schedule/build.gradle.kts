plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-core"))
    compile(project(":pleo-antaeus-models"))
    implementation("org.quartz-scheduler:quartz:2.3.1")
    runtime("org.postgresql:postgresql:42.2.6")
}