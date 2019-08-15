plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-core"))
    implementation("org.quartz-scheduler:quartz:2.3.1")
    implementation("org.postgresql:postgresql:42.2.6")
    compile(project(":pleo-antaeus-models"))
}