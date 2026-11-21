import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "9.4.3"
}

group = "dev.watchwolf"
version = "0.4.1"

base {
    archivesName.set("watchwolf-servers-manager")
}

dependencies {
    implementation(files("lib/watchwolf-core-0.3.1.jar"))
    implementation("com.github.docker-java:docker-java:3.3.6")
    implementation("org.apache.logging.log4j:log4j-api:2.23.1")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.8.1")
    testImplementation("com.google.jimfs:jimfs:1.3.0")
    testImplementation("io.github.hakky54:logcaptor:2.6.1")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.yaml:snakeyaml:2.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val integrationTestSourceSet = sourceSets.create("integrationTest") {
    java.srcDir("src/integration-test/java")
    compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
    runtimeClasspath += output + compileClasspath
}

configurations[integrationTestSourceSet.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())
configurations[integrationTestSourceSet.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.testRuntimeOnly.get())

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    include("**/*Should.class")
    exclude("**/IT*.class")
}

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs the Docker-backed integration tests."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath
    useJUnitPlatform()
    include("**/IT*.class")
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(tasks.test)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
}

tasks.jar {
    enabled = false
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
