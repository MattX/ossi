plugins {
    kotlin("jvm") version "1.3.70"
}

group = "io.terbium.ossi"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
    maven(url="https://dl.bintray.com/arrow-kt/arrow-kt/")
}

val ktorVersion = "1.3.2"
val arrowVersion = "0.10.5"
val exposedVersion = "0.23.1"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("org.xerial:sqlite-jdbc:3.30.1")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("com.atlassian.commonmark:commonmark:0.14.0")
    implementation("com.google.guava:guava:28.2-jre")
    implementation("ch.qos.logback:logback-classic:1.2.3")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}