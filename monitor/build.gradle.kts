plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.apollographql.apollo3")
}

val vertxVersion: String by project
val kotlinVersion: String by project
val apolloVersion: String by project
val projectVersion: String by project
val slf4jVersion: String by project
val intellijVersion: String by project

repositories {
    maven(url = "https://www.jetbrains.com/intellij-repository/releases") { name = "intellij-releases" }
    maven(url = "https://cache-redirector.jetbrains.com/intellij-dependencies/") { name = "intellij-dependencies" }
}

dependencies {
    if (findProject(":interfaces:jetbrains") != null) {
        compileOnly(project(":interfaces:jetbrains:common"))
    } else {
        compileOnly(project(":common"))
    }

    implementation("plus.sourceplus:protocol:$projectVersion")
    implementation("com.apollographql.apollo3:apollo-runtime:$apolloVersion")
    api("com.apollographql.apollo3:apollo-api:$apolloVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("io.vertx:vertx-core:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("io.dropwizard.metrics:metrics-core:4.2.11")
    implementation("eu.geekplace.javapinning:java-pinning-core:1.2.0")

    compileOnly("com.jetbrains.intellij.platform:ide:$intellijVersion")
    compileOnly("com.google.guava:guava:31.1-jre")
}

apollo {
    packageNamesFromFilePaths("monitor.skywalking.protocol")
}
