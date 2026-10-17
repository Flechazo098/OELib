import cc.sighs.gradle.configureJarJarFilenameNormalization
import cc.sighs.gradle.registerGeneratedResources

plugins {
    id("module-loader")
    id("net.neoforged.moddev")
}

val modId: String = extra["mod_id"] as String
val neoVer: String = property("neoforge_version") as String

neoForge {

    val commonProject = project(":modules:config:config-common")
    val at = commonProject.file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }

    version = neoVer

    runs {
        create("client") { client() }
        create("data") {
            clientData()
            programArguments.addAll(
                "--mod", modId,
                "--all",
                "--output", project.file("src/generated/resources/").absolutePath,
                "--existing", project.file("src/main/resources/").absolutePath
            )
        }
        create("server") { server() }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

registerGeneratedResources()

extra["mavenDependencyWhitelist"] = emptyList<String>()
dependencies {
    compileOnlyApi("de.marhali:json5-java:3.0.0")

    jarJar("de.marhali:json5-java:3.0.0")

    runtimeOnly("de.marhali:json5-java:3.0.0")
}

configureJarJarFilenameNormalization()
