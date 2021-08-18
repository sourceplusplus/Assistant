plugins {
    id("org.jetbrains.kotlin.jvm")
    id("maven-publish")
}

val kotlinVersion = ext.get("kotlinVersion")
val pluginGroup: String by project
val markerVersion: String by project

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = pluginGroup
            artifactId = "marker"
            version = markerVersion

            from(components["java"])
        }
    }
}

repositories {
    maven(url = "https://www.jetbrains.com/intellij-repository/releases") { name = "intellij-releases" }
    maven(url = "https://cache-redirector.jetbrains.com/intellij-dependencies/") { name = "intellij-dependencies" }
}

dependencies {
    val intellijVersion = "212.4746.92"

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.google.guava:guava:30.1.1-jre")
    implementation("org.jetbrains:annotations:22.0.0")
    compileOnly("org.slf4j:slf4j-api:1.7.32")
    compileOnly("org.jetbrains.intellij.deps.jcef:jcef:89.0.12-g2b76680-chromium-89.0.4389.90-api-1.6")
    compileOnly("com.jetbrains.intellij.platform:util-ui:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:analysis:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.java:java-analysis:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:ide:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:ide-impl:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:core:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:core-ui:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:editor:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:lang:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:lang-impl:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:project-model:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:code-style:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.groovy:groovy-psi:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:uast:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.java:java-indexing:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.java:java-indexing-impl:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:util:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.java:java-psi:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:extensions:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:util-rt:$intellijVersion") { isTransitive = false }
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion") { isTransitive = false }
    compileOnly("org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion") { isTransitive = false }
    testImplementation("junit:junit:4.13.2")
}
