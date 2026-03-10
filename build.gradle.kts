plugins {
    `java-library`
    id("io.spring.dependency-management") version "1.1.4"
    `maven-publish`
    signing
}

group = "io.github.fekom"
version = "0.0.1-SNAPSHOT"
description = "Bond - Byte-based rate limiter with token bucket for Spring Boot"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.2.0")
    }
}

dependencies {
    // Spring Boot (core)
    api("org.springframework.boot:spring-boot-starter-validation")

    // JPA (optional - consumers add if they want JpaBucketStore)
    compileOnly("org.springframework.boot:spring-boot-starter-data-jpa")

    // Web (optional - consumers add if they want BondRateLimitFilter)
    compileOnly("org.springframework.boot:spring-boot-starter-web")

    // Spring Boot auto-configuration
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Jackson (used for TokenBucket JSON serialization)
    api("com.fasterxml.jackson.core:jackson-databind")

    // PostgreSQL (optional - only needed for JpaBucketStore)
    compileOnly("org.postgresql:postgresql")

    // UUID v7
    api("com.fasterxml.uuid:java-uuid-generator:5.1.0")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-preview")
}

tasks.withType<Javadoc> {
    val opts = options as StandardJavadocDocletOptions
    opts.addStringOption("-release", "21")
    opts.addBooleanOption("-enable-preview", true)
    opts.addStringOption("Xdoclint:none", "-quiet")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("Bond")
                description.set("Byte-based rate limiter with token bucket for Spring Boot")
                url.set("https://github.com/FeKom/bond")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("fekom")
                        name.set("FeKom")
                        url.set("https://github.com/FeKom")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/FeKom/bond.git")
                    developerConnection.set("scm:git:ssh://github.com/FeKom/bond.git")
                    url.set("https://github.com/FeKom/bond")
                }
            }
        }
    }
    repositories {
        maven {
            name = "OSSRH"
            val releasesUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsUrl else releasesUrl
            credentials {
                username = findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME") ?: ""
                password = findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD") ?: ""
            }
        }
    }
}

signing {
    // Only sign when publishing to Maven Central (not for local builds)
    isRequired = gradle.taskGraph.hasTask("publish")
    sign(publishing.publications["mavenJava"])
}
