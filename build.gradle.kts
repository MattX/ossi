plugins {
    kotlin("jvm") version "1.3.70"
    kotlin("kapt") version "1.3.70"
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

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("org.xerial:sqlite-jdbc:3.30.1")
    implementation("io.arrow-kt:arrow-optics:$arrowVersion")
    implementation("io.arrow-kt:arrow-syntax:$arrowVersion")
    kapt("io.arrow-kt:arrow-meta:$arrowVersion")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}