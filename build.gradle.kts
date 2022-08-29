import java.util.Calendar

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("app.cash.licensee:licensee-gradle-plugin:1.5.0")
    }
}

plugins {
    id("com.diffplug.spotless") apply false
    id("org.jetbrains.kotlin.jvm") apply false
    id("io.gitlab.arturbosch.detekt") apply false
    id("org.jetbrains.intellij") version "1.8.1"
}

val pluginGroup: String by project
val pluginName: String by project
val projectVersion: String by project
val pluginSinceBuild: String by project
val vertxVersion: String by project

val platformType: String by project
val ideVersion: String by project
val platformPlugins: String by project
val platformDownloadSources: String by project

group = pluginGroup
version = projectVersion

allprojects {
    repositories {
        mavenCentral()
        maven(url = "https://www.jetbrains.com/intellij-repository/releases") { name = "intellij-releases" }
        maven(url = "https://cache-redirector.jetbrains.com/intellij-dependencies/") { name = "intellij-dependencies" }
    }

    apply(plugin = "org.jetbrains.intellij")

    intellij {
        pluginName.set("interface-jetbrains")
        version.set(ideVersion)
        type.set(platformType)
        downloadSources.set(platformDownloadSources.toBoolean())
        updateSinceUntilBuild.set(false)

        plugins.set(platformPlugins.split(',').map(String::trim).filter(String::isNotEmpty).toMutableList())
        //plugins.add("com.intellij.zh:202.413") //test chinese locale
    }

    tasks {
        // All these tasks don't make sense for non-root subprojects
        // Root project (i.e. `:plugin`) enables them itself if needed
        runIde { enabled = false }
        prepareSandbox { enabled = false }
        buildSearchableOptions { enabled = false }
    }
}

subprojects {
    repositories {
        mavenCentral()
        maven(url = "https://pkg.sourceplus.plus/sourceplusplus/protocol")
        maven(url = "https://pkg.sourceplus.plus/sourceplusplus/interface-booster-ui")
    }

    if (!this.toString().contains("commander")) {
        apply(plugin = "app.cash.licensee")
        configure<app.cash.licensee.LicenseeExtension> {
            ignoreDependencies("plus.sourceplus", "protocol")
            ignoreDependencies("plus.sourceplus", "protocol-jvm")
            ignoreDependencies("plus.sourceplus.interface", "interface-booster-ui")
            allow("Apache-2.0")
            allow("MIT")
            allow("EPL-1.0")
            allow("LGPL-2.1-only")
            allowUrl("https://raw.githubusercontent.com/apollographql/apollo-kotlin/main/LICENSE") //MIT
            allowUrl("https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html") //LGPL 2.1
            allowUrl("https://www.bouncycastle.org/licence.html") //MIT
            allowUrl("https://api.github.com/licenses/apache-2.0") //Apache 2.0
            allowUrl("http://www.jcraft.com/jsch/LICENSE.txt") //BSD-style
            allowUrl("http://www.jcraft.com/jzlib/LICENSE.txt") //BSD-style
            allowUrl("http://jgrapht.org/LGPL.html") //LGPL 2.1
            allowUrl("http://www.eclipse.org/legal/epl-v20.html") //EPL 2.0
            allowDependency("net.jcip", "jcip-annotations", "1.0") {
                because("Creative Commons")
            }
        }
    }

    apply<io.gitlab.arturbosch.detekt.DetektPlugin>()
    val detektPlugins by configurations
    dependencies {
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.21.0")
    }

    tasks {
        withType<io.gitlab.arturbosch.detekt.Detekt> {
            parallel = true
            buildUponDefaultConfig = true
            config.setFrom(arrayOf(File(project.rootDir, "detekt.yml")))
        }

        withType<JavaCompile> {
            sourceCompatibility = "1.8"
            targetCompatibility = "1.8"
        }
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.jvmTarget = "11"
            kotlinOptions.freeCompilerArgs +=
                listOf(
                    "-Xno-optimized-callable-references",
                    "-Xjvm-default=compatibility"
                )
        }

        withType<Test> {
            testLogging {
                events("passed", "skipped", "failed")
                setExceptionFormat("full")

                outputs.upToDateWhen { false }
                showStandardStreams = true
            }
        }
    }

    apply(plugin = "com.diffplug.spotless")
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            targetExclude("**/generated/**", "**/liveplugin/**")

            val startYear = 2022
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val copyrightYears = if (startYear == currentYear) {
                "$startYear"
            } else {
                "$startYear-$currentYear"
            }

            val jetbrainsProject = findProject(":interfaces:jetbrains") ?: rootProject
            val licenseHeader = Regex("( . Copyright [\\S\\s]+)")
                .find(File(jetbrainsProject.projectDir, "LICENSE").readText())!!
                .value.lines().joinToString("\n") {
                    if (it.trim().isEmpty()) {
                        " *"
                    } else {
                        " * " + it.trim()
                    }
                }
            val formattedLicenseHeader = buildString {
                append("/*\n")
                append(
                    licenseHeader.replace(
                        "Copyright [yyyy] [name of copyright owner]",
                        "Source++, the open-source live coding platform.\n" +
                                " * Copyright (C) $copyrightYears CodeBrig, Inc."
                    ).replace(
                        "http://www.apache.org/licenses/LICENSE-2.0",
                        "    http://www.apache.org/licenses/LICENSE-2.0"
                    )
                )
                append("/")
            }
            licenseHeader(formattedLicenseHeader)
        }
    }

    fun projectDependency(name: String): ProjectDependency {
        return if (rootProject.name != "jetbrains") {
            DependencyHandlerScope.of(rootProject.dependencies).project(":interfaces:jetbrains$name")
        } else {
            DependencyHandlerScope.of(rootProject.dependencies).project(name)
        }
    }
}
