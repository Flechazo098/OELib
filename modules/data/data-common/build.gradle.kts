import cc.sighs.gradle.configureCommonSourceArtifacts

plugins {
    id("module-common")
    id("net.neoforged.moddev")
}

val NFMVersion = property("neo_form_version") as String

neoForge {
    neoFormVersion = NFMVersion

    val at = file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from("src/main/resources/META-INF/accesstransformer.cfg")
    }
}

extra["mavenDependencyWhitelist"] = listOf("org.mvel:mvel2")
dependencies {
    implementation("org.mvel:mvel2:2.5.0.Final")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    testImplementation(files(sourceSets.main.get().compileClasspath))
}

moduleDependencies {
    api(":modules:event")
    api(":modules:network")
    api(":modules:misc")
}

configureCommonSourceArtifacts()

configurations.testImplementation { extendsFrom(configurations.compileOnly.get()) }
tasks.withType<Test>().configureEach { useJUnitPlatform() }
