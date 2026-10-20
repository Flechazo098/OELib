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

dependencies {
}

moduleDependencies {
    api(":modules:config")
    api(":modules:data")
    api(":modules:event")
    api(":modules:misc")
    api(":modules:network")
    api(":modules:registry")
    api(":modules:renderer")
    api(":modules:bless")
}

configureCommonSourceArtifacts()

// Compatibility artifacts are embedded in the main mod only.
tasks.withType<org.gradle.api.publish.maven.tasks.AbstractPublishToMaven>().configureEach {
    enabled = false
}
