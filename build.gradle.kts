plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.flywaydb.flyway") version "9.22.3"
    java
}

group = "github.fekom"
version = "0.0.1-SNAPSHOT"
description = "bond Platform"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-security")
    //FreeBuilder
    implementation("org.inferred:freebuilder:2.4.1")
    annotationProcessor ("org.inferred:freebuilder:2.4.1")


    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // PostgreSQL
    runtimeOnly("org.postgresql:postgresql")

    // Observability (Micrometer)
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.micrometer:micrometer-observation")
    implementation("io.micrometer:micrometer-core")
    implementation("io.micrometer:micrometer-registry-prometheus")

    //UUIDV7
    implementation("com.fasterxml.uuid:java-uuid-generator:5.1.0")


    // Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")


    // Flyway
    implementation("org.flywaydb:flyway-core:9.22.3")



    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

flyway {
    url = "jdbc:postgresql://localhost:5432/bond"
    user = "postgres"
    password = "postgres"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("bond.jar")
    layered {
        enabled.set(true)
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-preview")
}


tasks.withType<Test> {
    jvmArgs("--enable-preview")
}
