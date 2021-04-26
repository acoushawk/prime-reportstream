import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.apache.tools.ant.filters.ReplaceTokens
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    kotlin("jvm") version "1.4.32"
    id("org.flywaydb.flyway") version "7.8.1"
    id("nu.studer.jooq") version "5.2"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("com.microsoft.azure.azurefunctions") version "1.5.1"
}

group = "gov.cdc.prime"
version = "0.1-SNAPSHOT"
description = "prime-router"
val azureAppName = "prime-data-hub-router"
val azureFunctionsDir = "azure-functions"

// Set the Java compiler JVM target
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// Set the Kotlin compiler JVM target
val compileKotlin: KotlinCompile by tasks
val compileTestKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "1.8"
compileTestKotlin.kotlinOptions.jvmTarget = "1.8"

// Local database information
val dbUser ="prime"
val dbPassword ="changeIT!"
val dbUrl ="jdbc:postgresql://localhost:5432/prime_data_hub"
val jooqSourceDir = "build/generated-src/jooq/src/main/java"
val jooqPackageName = "gov.cdc.prime.router.azure.db"

sourceSets.main {
    // Add the location of the generated database classes
    java.srcDirs(jooqSourceDir)
    // Exclude SQL files from being copied to resulting package
    resources.exclude("**/*.sql")
}

tasks.test {
    // Use JUnit 5 for running tests
    useJUnitPlatform()
    dependsOn("compileKotlin")
}

tasks.processResources {
    // Set the proper build values in the build.properties file
    filesMatching("build.properties") {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val formattedDate = LocalDateTime.now().format(dateFormatter)
        val tokens = mapOf("version" to version, "timestamp" to formattedDate)
        filter(ReplaceTokens::class, mapOf("beginToken" to "@", "endToken" to "@", "tokens" to tokens))
    }
}

tasks.jar {
    manifest {
        /* We put the CLI main class in the manifest at this step as a convenience to allow this jar to be
        run by the ./prime script. It will be overwritten by the Azure host or the CLI fat jar package. */
        attributes("Main-Class" to "gov.cdc.prime.router.cli.MainKt")
    }
}

azurefunctions {
    appName = azureAppName
//    setRuntime(closureOf<com.microsoft.azure.plugin.functions.gradle.configuration.GradleRuntimeConfiguration> {
//        os = "linux"
//        javaVersion = "11"
//    })
    setAppSettings(closureOf<MutableMap<String, String>> {
        this["WEBSITE_RUN_FROM_PACKAGE"] = "1"
        this["FUNCTIONS_EXTENSION_VERSION"] = "3"
        this["FUNCTIONS_WORKER_RUNTIME"] = "java"
    })
}

tasks.azureFunctionsPackage {
    dependsOn("test")
}

val azureResourcesTmpDir = File(rootProject.buildDir.path, "${azureFunctionsDir}-resources/${azureAppName}")
val azureResourcesFinalDir = File(rootProject.buildDir.path, "${azureFunctionsDir}/${azureAppName}")
tasks.register<Copy>("gatherAzureResources") {
    from("./")
    into(azureResourcesTmpDir)
    include("metadata/**/*.yml")
    include("metadata/**/*.schema")
    include("metadata/**/*.valuesets")
    include("metadata/**/*.csv")
    include("settings/**/*.yml")
    include("assets/**/*__inline.html")
}

tasks.register("copyAzureResources") {
    dependsOn("gatherAzureResources")
    doLast() {
        // We need to use a regular copy, so Gradle does not delete the existing folder
        org.apache.commons.io.FileUtils.copyDirectory(azureResourcesTmpDir, azureResourcesFinalDir)
    }
}

val azureScriptsTmpDir = File(rootProject.buildDir.path, "${azureFunctionsDir}-scripts/${azureAppName}")
val azureScriptsFinalDir = rootProject.buildDir
tasks.register<Copy>("gatherAzureScripts") {
    from("./")
    into(azureScriptsTmpDir)
    include("prime")
    include("start_func.sh")
}

tasks.register("copyAzureScripts") {
    dependsOn("gatherAzureScripts")
    doLast() {
        // We need to use a regular copy, so Gradle does not delete the existing folder
        org.apache.commons.io.FileUtils.copyDirectory(azureScriptsTmpDir, azureScriptsFinalDir)
    }
}

// Configuration for Flyway migration tool
flyway {
    url = dbUrl
    user = dbUser
    password = dbPassword
}

// Database code generation configuration
jooq {
    configurations {
        create("main") {  // name of the jOOQ configuration
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = dbUrl
                    user = dbUser
                    password = dbPassword
                }
                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                        includes = ".*"
                    }
                    generate.apply {
                        isImmutablePojos = false
                        isFluentSetters = true
                        isPojos = true
                        isPojosEqualsAndHashCode = true
                        isPojosToString = true
                        isJavaTimeTypes = true
                    }
                    target.apply {
                        packageName = jooqPackageName
                        directory = jooqSourceDir
                    }
                }
            }
        }
    }
}

// Convenience tasks
tasks.register("compile") {
    dependsOn("compileKotlin")
}

tasks.register("migrate") {
    dependsOn("flywayMigrate")
}

tasks.register("packageAzure") {
    dependsOn("azureFunctionsPackage")
    dependsOn("copyAzureResources")
    dependsOn("copyAzureScripts")
}

tasks.register("packageLocal") {
    dependsOn("shadowJar")
}

tasks.register("package") {
    dependsOn("packageLocal")
    dependsOn("packageAzure")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://kotlin.bintray.com/kotlinx")
    }
    maven {
        url = uri("https://jitpack.io")
    }
//    maven {
//        url = uri("https://repo1.maven.org/maven2/")
//    }
//    maven {
//        url = uri("http://jsch.sf.net/maven2/")
//    }
//
//    maven {
//        url = uri("https://repo.maven.apache.org/maven2/")
//    }
}



dependencies {
    jooqGenerator("org.postgresql:postgresql:42.2.19")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.32")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common:1.4.32")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.32")
    implementation("com.microsoft.azure.functions:azure-functions-java-library:1.4.2")
    implementation("com.azure:azure-core:1.15.0")
    implementation("com.azure:azure-core-http-netty:1.9.1")
    implementation("com.azure:azure-storage-blob:12.10.2") {
        exclude(group = "com.azure", module = "azure-core")
    }
    implementation("com.azure:azure-storage-queue:12.8.0") {
        exclude(group = "com.azure", module = "azure-core")
    }
    implementation("com.azure:azure-security-keyvault-secrets:4.2.7") {
        exclude(group = "com.azure", module = "azure-core")
        exclude(group = "com.azure", module = "azure-core-http-netty")
    }
    implementation("com.azure:azure-identity:1.2.5") {
        exclude(group = "com.azure", module = "azure-core")
        exclude(group = "com.azure", module = "azure-core-http-netty")
    }
    implementation("org.apache.logging.log4j:log4j-api:[2.13.2,)")
    implementation("org.apache.logging.log4j:log4j-core:[2.13.2,)")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:[2.13.2,)")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.0.0")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:0.15.1")
    implementation("tech.tablesaw:tablesaw-core:0.38.2")
    implementation("com.github.ajalt.clikt:clikt-jvm:3.1.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.3")
    implementation("com.github.javafaker:javafaker:1.0.2")
    implementation("ca.uhn.hapi:hapi-base:2.3")
    implementation("ca.uhn.hapi:hapi-structures-v251:2.3")
    implementation("com.googlecode.libphonenumber:libphonenumber:8.12.21")
    implementation("org.thymeleaf:thymeleaf:3.0.12.RELEASE")
    implementation("com.sendgrid:sendgrid-java:4.7.2")
    implementation("com.okta.jwt:okta-jwt-verifier:0.5.1")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1") {
        exclude(group = "org.json", module = "json")
    }
    implementation("com.github.kittinunf.fuel:fuel-json:2.3.1")
    implementation("org.json:json:20210307")
    implementation("com.hierynomus:sshj:0.31.0")
    implementation("org.bouncycastle:bcprov-jdk15on:1.68")
    implementation("com.jcraft:jsch:0.1.55")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("commons-codec:commons-codec:1.15")
    implementation("commons-io:commons-io:2.8.0")
    implementation("org.postgresql:postgresql:42.2.19")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.flywaydb:flyway-core:7.7.3")
    implementation("com.github.kayr:fuzzy-csv:1.6.48")

    runtimeOnly("com.okta.jwt:okta-jwt-verifier-impl:0.5.1")
    runtimeOnly("com.github.kittinunf.fuel:fuel-jackson:2.3.1")

    testImplementation(kotlin("test-junit5"))
    testImplementation("com.github.KennethWussmann:mock-fuel:1.3.0"){
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
        exclude(group = "com.github.kittinunf.fuel", module = "fuel")
    }
    testImplementation("io.mockk:mockk:1.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
}




