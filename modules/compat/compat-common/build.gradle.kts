import cc.sighs.gradle.configureCommonSourceArtifacts

plugins {
    id("module-common")
    id("net.neoforged.moddev.legacyforge")
}

val mcVersion: String = property("minecraft_version") as String
val parchmentMc: String = property("parchment_minecraft") as String
val parchmentVer: String = property("parchment_version") as String

legacyForge {
    mcpVersion = mcVersion

    val at = file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from("src/main/resources/META-INF/accesstransformer.cfg")
    }

    parchment {
        minecraftVersion = parchmentMc
        mappingsVersion = parchmentVer
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
