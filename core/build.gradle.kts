plugins {
    id("org.jetbrains.kotlin.jvm")
    id("maven-publish")
}

val joorVersion: String by project
val vertxVersion: String by project
val projectVersion: String by project
val protocolVersion = project.properties["protocolVersion"] as String? ?: projectVersion

group = "plus.sourceplus.interface"
version = project.properties["projectVersion"] as String? ?: projectVersion

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
                artifactId = "jetbrains-core"
                version = project.version.toString()

                from(components["kotlin"])

                // Ship the sources jar
                artifact(sourcesJar)
            }
        }
    }
}

dependencies {
    compileOnly("plus.sourceplus:protocol:$protocolVersion")
    compileOnly("io.vertx:vertx-core:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    compileOnly("io.dropwizard.metrics:metrics-core:4.2.30")
    compileOnly("org.jooq:joor:$joorVersion")
}
