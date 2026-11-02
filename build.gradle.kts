import cc.sighs.gradle.CreateModuleTask

plugins {
    id("me.modmuss50.mod-publish-plugin") version "2.2.1"
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.141" apply false
}

// Set version from gradle.properties (mod_version) for all projects
val modVersion: String = property("mod_version") as String
allprojects { version = modVersion }

// Load module properties from gradle.extra (set by settings.gradle.kts)
@Suppress("UNCHECKED_CAST")
val moduleProps: Map<String, Map<String, String>> = gradle.extra["moduleProps"] as? Map<String, Map<String, String>> ?: emptyMap()

// Apply per-module extra properties before subproject build scripts evaluate
subprojects {
    val path = this.path
    if (path.startsWith(":modules:")) {
        val parts = path.split(":")
        if (parts.size >= 3) {
            val moduleName = parts[2]
            moduleProps[moduleName]?.forEach { (key, value) ->
                extra[key] = value
            }
        }
    }
}

// Reuse the module directory snapshot collected during settings evaluation.
// Keeping one snapshot avoids repeated filesystem scans while preserving the
// current discovery order and the set of directories selected by settings.
@Suppress("UNCHECKED_CAST")
val moduleDirs: List<File> = gradle.extra["moduleDirs"] as? List<File> ?: emptyList()
val discoveredModules = moduleDirs.associateBy { it.name }
extra["discoveredModules"] = discoveredModules

// Convenience task: create a new module from template
val modId: String = property("mod_id") as String
tasks.register<CreateModuleTask>("createModule") {
    parentModId = modId
}

val curseforgeReleaseVersion = "${modVersion}-hotfix3"

fun curseforgeArtifact(loader: String, archiveTask: String): Provider<RegularFile> {
    val archiveName = "oelib-$loader-${property("minecraft_version")}-$modVersion.jar"
    val releaseName = "oelib-$loader-${property("minecraft_version")}-$curseforgeReleaseVersion.jar"
    val prepare = tasks.register<Sync>("prepareCurseforge${loader.replaceFirstChar { it.uppercaseChar() }}") {
        dependsOn(":$loader:$archiveTask")
        from(project(":$loader").layout.buildDirectory.file("libs/$archiveName"))
        into(layout.buildDirectory.dir("curseforge/$loader"))
        rename { releaseName }
    }
    return layout.file(prepare.map { it.destinationDir.resolve(releaseName) })
}

val curseforgeFabricJar = curseforgeArtifact("fabric", "jar")
val curseforgeNeoforgeJar = curseforgeArtifact("neoforge", "jar")

publishMods {
    dryRun.set(providers.gradleProperty("curseforgeDryRun").map { it.toBooleanStrict() }.orElse(true))
    version.set(curseforgeReleaseVersion)
    type.set(me.modmuss50.mpp.ReleaseType.STABLE)
    changelog.set("Fixed a bug that prevented MVEL functions from being registered. This may allow Smart Key Prompts to work properly.")

    val curseforgeDefaults = curseforgeOptions {
        accessToken.set(providers.environmentVariable("CURSEFORGE_TOKEN"))
        projectId.set("1319210")
        projectSlug.set("oelib")
        minecraftVersions.addAll("26.1", "26.1.1", "26.1.2")
        client.set(true)
        server.set(true)
    }

    curseforge("curseforgeFabric") {
        from(curseforgeDefaults)
        file.set(curseforgeFabricJar)
        displayName.set("OELib $curseforgeReleaseVersion-Fabric-26.1")
        modLoaders.add("fabric")
    }

    curseforge("curseforgeNeoforge") {
        from(curseforgeDefaults)
        file.set(curseforgeNeoforgeJar)
        displayName.set("OELib $curseforgeReleaseVersion-NeoForge-26.1")
        modLoaders.add("neoforge")
    }
}
