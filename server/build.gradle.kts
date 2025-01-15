import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

ext["spring-security.version"] = "5.8.3"

plugins {
    id("org.springframework.boot") version "2.7.13"
    id("io.spring.dependency-management") version "1.0.15.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
    kotlin("plugin.jpa") version "1.6.21"
    id("org.sonarqube") version "4.2.1.3168"
    jacoco
}

group = "org.elaastic.questions"
version = "6.3.7"
var uiComponentsVersion = "0.0.19"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

sonar {
    properties {
        property("sonar.projectKey", "elaastic_elaastic-questions-server")
        property("sonar.organization", "elaastic")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("com.mysql:mysql-connector-j")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.9")


    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-rest")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.hibernate:hibernate-jcache")
    implementation("org.ehcache:ehcache:3.6.3")
//	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-cas")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity5")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-allopen")
    implementation("org.springframework.data:spring-data-rest-hal-explorer")
    implementation("org.apache.commons:commons-lang3:3.9")
    implementation("com.icegreen:greenmail:1.5.10")
    implementation(files("lib/ApacheJMeter_oauth-v2.jar"))
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("commons-io:commons-io:2.7")
    implementation("org.apache.commons:commons-csv:1.5")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.togglz:togglz-spring-boot-starter:3.0.0")
    implementation("org.togglz:togglz-spring-security:3.0.0")
    implementation("org.togglz:togglz-kotlin:3.0.0")
    implementation("org.togglz:togglz-console:3.0.0")
    implementation("com.github.heneke.thymeleaf:thymeleaf-extras-togglz:1.0.1.RELEASE")
    implementation("com.toedter:spring-hateoas-jsonapi:1.6.0")
    implementation("org.jsoup:jsoup:1.16.1")


    testImplementation("org.springframework.security:spring-security-test")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "junit")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.hamcrest:hamcrest-library")
    testImplementation("org.exparity:hamcrest-date:1.1.0")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0")
    testImplementation("org.mockito:mockito-inline:2.13.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    implementation("io.cucumber:cucumber-spring:7.14.1")
    testImplementation("io.cucumber:cucumber-java:7.14.1")
    testImplementation("io.cucumber:cucumber-junit:7.14.1")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.14.1")
    testImplementation("org.junit.platform:junit-platform-suite:1.10.1")

}

allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.MappedSuperclass")
    annotation("javax.persistence.Embeddable")
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}



tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
    reports {
        xml.required.set(true)
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.getByName<Jar>("jar") {
    enabled = false
}

tasks.register("dockerBuild") {
    group = "Docker"
    description = "Builds the Docker image aligned with the elaastic version"
    doLast {
        exec {
            commandLine(
                "docker", "build",
                "-t", "elaastic/elaastic-questions-server:v$version",
                "-t", "elaastic/elaastic-questions-server:latest",
                "."
            )
        }
    }
}

tasks.register<Exec>("updateVueComponents") {
    group = "UI"
    description = "Update the Vue.js components bundle"

    doFirst {
        // Get the version from local variable
        val version = uiComponentsVersion

        // Verify version format
        if (!version.matches(Regex("""^\d+\.\d+\.\d+$"""))) {
            throw GradleException("Invalid version format. Version should be in format: X.Y.Z")
        }

        workingDir("../ui-components/scripts")
        val isWindowsOS = System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows")

        // Verify script exists before execution
        val scriptFile = if (isWindowsOS) {
            file("../ui-components/scripts/update-bundle.bat")
        } else {
            file("../ui-components/scripts/update-bundle.sh")
        }

        if (!scriptFile.exists()) {
            throw GradleException("Script file not found at: ${scriptFile.absolutePath}")
        } else {
            println("Script file found at: ${scriptFile.absolutePath}")
        }

        // Execute the script based on OS
        if (isWindowsOS) {
            commandLine("cmd", "/c", "update-bundle.bat", version)
        } else {
            commandLine("./update-bundle.sh", version)
        }

        // Warn the user to update the application.properties with the new version
        println("Please update the application.properties with the new version: $version")
    }
}
