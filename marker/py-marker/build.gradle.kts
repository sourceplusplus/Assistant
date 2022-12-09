plugins {
    kotlin("jvm")
    id("maven-publish")
}

val vertxVersion: String by project
val projectVersion: String by project
val jupiterVersion: String by project
val protocolVersion = project.properties["protocolVersion"] as String? ?: projectVersion

group = "plus.sourceplus.interface"
version = project.properties["projectVersion"] as String? ?: projectVersion

intellij {
    type.set("IC")
    plugins.set(listOf("PythonCore:222.3739.68"))
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(project.the<SourceSetContainer>()["main"].allSource)
}

configure<PublishingExtension> {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/sourceplusplus/interface-jetbrains")
            credentials {
                username = System.getenv("GH_PUBLISH_USERNAME")?.toString()
                password = System.getenv("GH_PUBLISH_TOKEN")?.toString()
            }
        }
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = project.group.toString()
                artifactId = "jetbrains-marker-py"
                version = project.version.toString()

                from(components["kotlin"])

                // Ship the sources jar
                artifact(sourcesJar)
            }
        }
    }
}

dependencies {
    implementation(projectDependency(":common"))
    implementation(projectDependency(":marker"))
    implementation("plus.sourceplus:protocol:$protocolVersion")

    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("com.google.guava:guava:31.1-jre")
    compileOnly("org.jetbrains:annotations:23.1.0")
    compileOnly("io.vertx:vertx-core:$vertxVersion")

    testRuntimeOnly(projectDependency(":marker:ult-marker"))
    testImplementation(projectDependency(":common"))
    testImplementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$jupiterVersion")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

fun projectDependency(name: String): ProjectDependency {
    return if (rootProject.name.contains("jetbrains")) {
        DependencyHandlerScope.of(rootProject.dependencies).project(name)
    } else {
        DependencyHandlerScope.of(rootProject.dependencies).project(":interfaces:jetbrains$name")
    }
}
