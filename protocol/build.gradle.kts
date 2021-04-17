plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.4.32"
    kotlin("kapt")
    id("org.jetbrains.kotlin.plugin.noarg") version "1.4.32"
    id("java")
}

val vertxVersion = ext.get("vertxVersion")

kotlin {
    jvm { }
    js {
        browser { }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("io.vertx:vertx-core:$vertxVersion")
                implementation("io.vertx:vertx-codegen:$vertxVersion")
                implementation("io.vertx:vertx-service-proxy:$vertxVersion")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.2")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.12.2")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava:2.12.2")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.2")
                implementation("org.jooq:jooq:3.14.8")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.vertx:vertx-core:$vertxVersion")
                implementation("com.google.guava:guava:30.1.1-jre")
                implementation("junit:junit:4.13.2")
                implementation(project(":protocol"))
                //todo: shouldn't be 2.10.3
                implementation("com.fasterxml.jackson.core:jackson-core:2.12.2")
                implementation("com.fasterxml.jackson.core:jackson-databind:2.12.2")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.2")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.12.2")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava:2.12.2")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.2")
                implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.32")
            }
        }
    }
}

dependencies {
    "kapt"("io.vertx:vertx-codegen:$vertxVersion:processor")
}

tasks {
    configure<SourceSetContainer> {
        named("main") {
            java.srcDir("$buildDir/generated/source/kapt/main")

            dependencies {
                implementation("io.vertx:vertx-core:$vertxVersion")
                implementation("io.vertx:vertx-codegen:$vertxVersion")
                implementation("io.vertx:vertx-service-proxy:$vertxVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
                compileOnly(project(":protocol"))
            }
        }
    }

    register("makeExternalJar") {
        mustRunAfter("jar")
        dependsOn("mergeJars")
        doFirst {
            file("$buildDir/libs/protocol-jvm.jar").delete()
            file("$buildDir/libs/protocol-jvm-final.jar")
                .renameTo(file("$buildDir/libs/protocol-jvm.jar"))
        }
    }
    register<Jar>("mergeJars") {
        dependsOn("jar")
        from(zipTree("$buildDir/libs/protocol.jar"))
        from(zipTree("$buildDir/libs/protocol-jvm.jar"))
        archiveBaseName.set("protocol-jvm-final")
    }
}

tasks.register<Copy>("setupJsonMappers") {
    from(file("$projectDir/src/jvmMain/resources/META-INF/vertx/json-mappers.properties"))
    into(file("$buildDir/tmp/kapt3/src/main/resources/META-INF/vertx"))
}
tasks.getByName<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlinJvm") {
    kotlinOptions {
        javaParameters = true
    }
    dependsOn("setupJsonMappers")
}

tasks.getByName("build").dependsOn("makeExternalJar")

configure<org.jetbrains.kotlin.noarg.gradle.NoArgExtension> {
    annotation("kotlinx.serialization.Serializable")
}