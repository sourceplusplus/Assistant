import org.jetbrains.changelog.markdownToHTML
import java.net.URL

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog") version "2.1.2"
}

val joorVersion: String by project
val jacksonVersion: String by project
val vertxVersion: String by project
val kotlinVersion: String by project
val projectVersion: String by project
val jupiterVersion: String by project
val protocolVersion = project.properties["protocolVersion"] as String? ?: projectVersion

// Import variables from gradle.properties file
val pluginGroup: String by project
val pluginName: String by project
val pluginSinceBuild: String by project

val platformType: String by project
val ideVersion: String by project
val platformPlugins: String by project
val platformDownloadSources: String by project

group = pluginGroup
version = projectVersion

tasks {
    buildPlugin { enabled = true }
    buildSearchableOptions { enabled = true }
    downloadRobotServerPlugin { enabled = true }
    jarSearchableOptions { enabled = true }
    patchPluginXml { enabled = true }
    prepareSandbox { enabled = true }
    prepareTestingSandbox { enabled = true }
    prepareUiTestingSandbox { enabled = true }
    publishPlugin { enabled = true }
    runIde { enabled = true }
    runIdeForUiTests { enabled = true }
    runPluginVerifier { enabled = true }
    signPlugin { enabled = true }
    verifyPlugin { enabled = true }
    listProductsReleases { enabled = true }
    instrumentCode { enabled = true }
}

tasks.getByName<JavaExec>("runIde") {
    //systemProperty("spp.debug.unblocked_threads", true)
    systemProperty("ide.enable.slow.operations.in.edt", false)
    systemProperty("idea.log.debug.categories", "#spp.jetbrains")
}

sourceSets.main {
    java.srcDirs("src/main/kotlin")
}

changelog {
    version.set(projectVersion)
}

dependencies {
    implementation(projectDependency(":commander")) {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation(projectDependency(":commander:kotlin-compiler-wrapper")) {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation(projectDependency(":core"))
    implementation(projectDependency(":insight"))
    implementation(projectDependency(":marker"))
    runtimeOnly(projectDependency(":marker:js-marker"))
    runtimeOnly(projectDependency(":marker:jvm-marker"))
    runtimeOnly(projectDependency(":marker:py-marker"))
    runtimeOnly(projectDependency(":marker:rs-marker"))
    runtimeOnly(projectDependency(":marker:ult-marker"))
    implementation("plus.sourceplus:protocol:$protocolVersion")

    implementation("org.apache.commons:commons-text:1.10.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("io.vertx:vertx-core:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion") {
        exclude(group = "org.jetbrains.kotlinx")
    }
    implementation("io.vertx:vertx-web:$vertxVersion")
    implementation("io.vertx:vertx-service-discovery:$vertxVersion")
    implementation("io.vertx:vertx-service-proxy:$vertxVersion")
    implementation("io.vertx:vertx-tcp-eventbus-bridge:$vertxVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("io.dropwizard.metrics:metrics-core:4.2.30")
    implementation("org.jooq:joor:$joorVersion")
    implementation("org.eclipse.mylyn.github:org.eclipse.egit.github.core:2.1.5")
    implementation("org.apache.commons:commons-lang3:3.13.0")
    implementation("eu.geekplace.javapinning:java-pinning-core:1.2.0")
    implementation("info.debatty:java-string-similarity:2.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:$jupiterVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    patchPluginXml {
        version.set(projectVersion)
        sinceBuild.set(pluginSinceBuild)

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
            File(file(projectDir).parent, "./README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md file:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString("\n").run { markdownToHTML(this) }
        )

        // Get the latest available change notes from the changelog file
        changeNotes.set(project.provider {
            val pluginChangesHeader = "### [JetBrains Plugin](https://github.com/sourceplusplus/interface-jetbrains)\n"
            val fullChangelog = URL("https://raw.githubusercontent.com/sourceplusplus/documentation/master/docs/changelog/README.md")
                .readText()
            if (fullChangelog.contains(pluginChangesHeader)) {
                val changelog = fullChangelog.substringAfter(pluginChangesHeader)
                    .substringBefore("\n### ").substringBefore("\n## ")
                    .trim()
                markdownToHTML(changelog)
            } else {
                markdownToHTML("")
            }
        })
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(listOf(projectVersion.split('-').getOrElse(1) { "default" }.split('.').first()))
    }

    runIde {
        dependsOn("getKotlinCompilerWrapper")
        jvmArgs = listOf("-Xmx2G")
    }

    register<Copy>("getKotlinCompilerWrapper") {
        mustRunAfter("prepareSandbox")
        if (findProject(":interfaces:jetbrains") != null) {
            dependsOn(":interfaces:jetbrains:commander:kotlin-compiler-wrapper:installDist")
        } else {
            dependsOn(":commander:kotlin-compiler-wrapper:installDist")
        }
        val wrapperBuildDir = if (findProject(":interfaces:jetbrains") != null) {
            project(":interfaces:jetbrains:commander:kotlin-compiler-wrapper").buildDir
        } else {
            project(":commander:kotlin-compiler-wrapper").buildDir
        }
        from(File(wrapperBuildDir, "install/kotlin-compiler-wrapper/lib"))
        into(File(buildDir, "idea-sandbox/plugins/interface-jetbrains/kotlin-compiler"))
    }
    getByName("buildPlugin") {
        dependsOn("getKotlinCompilerWrapper")
    }
    getByName("verifyPlugin") {
        dependsOn("getKotlinCompilerWrapper")
    }
    getByName("buildSearchableOptions") {
        dependsOn("getKotlinCompilerWrapper")
    }

    register("getPluginChangelog") {
        doFirst {
            val pluginChangesHeader = "### [JetBrains Plugin](https://github.com/sourceplusplus/interface-jetbrains)\n"
            val fullChangelog = URL("https://raw.githubusercontent.com/sourceplusplus/documentation/master/docs/changelog/README.md")
                .readText()
            if (fullChangelog.contains(pluginChangesHeader)) {
                val changelog = fullChangelog.substringAfter(pluginChangesHeader)
                    .substringBefore("\n### ").substringBefore("\n## ")
                    .trim()
                println(changelog)
            }
        }
    }

    test {
        useJUnitPlatform()
    }
}

fun projectDependency(name: String): ProjectDependency {
    return if (rootProject.name.contains("jetbrains")) {
        DependencyHandlerScope.of(rootProject.dependencies).project(name)
    } else {
        DependencyHandlerScope.of(rootProject.dependencies).project(":interfaces:jetbrains$name")
    }
}
