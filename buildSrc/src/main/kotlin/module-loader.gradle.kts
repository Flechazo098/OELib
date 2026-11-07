import cc.sighs.gradle.ModuleDependenciesExtension
import cc.sighs.gradle.SharedDependenciesExtension

plugins {
    id("module-common")
}

// Shared dependencies DSL — add loader-specific deps for all modules
// Usage: sharedDependencies { fabric("com.terraformersmc:modmenu:7.2.2") }
fun sharedDependencies(action: SharedDependenciesExtension.() -> Unit) {
    SharedDependenciesExtension(project).action()
}

val projectName = project.name
val commonProjectName = when {
    projectName.endsWith("-fabric") -> projectName.removeSuffix("-fabric") + "-common"
    projectName.endsWith("-forge") -> projectName.removeSuffix("-forge") + "-common"
    projectName.endsWith("-neoforge") -> projectName.removeSuffix("-neoforge") + "-common"
    else -> throw GradleException("Module loader project name must end with -fabric, -forge or -neoforge, got: $projectName")
}

val parentPath = project.path.substringBeforeLast(":")
val commonProjectPath = "${parentPath}:${commonProjectName}"
val modmenuVersion = property("modmenu_version") as String

configurations {
    register("commonJava") {
        isCanBeResolved = true
    }
    register("commonResources") {
        isCanBeResolved = true
    }
}

dependencies {
    compileOnly(project(commonProjectPath))
    add("commonJava", project(commonProjectPath, "commonJava"))
    add("commonResources", project(commonProjectPath, "commonResources"))
}

sharedDependencies {
    fabric("com.terraformersmc:modmenu:${modmenuVersion}")
    fabric("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    fabric("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
}

// Forward module deps declared in -common via moduleDependencies { api() / implementation() }
val suffix = ModuleDependenciesExtension.getSuffix(projectName)
project.evaluationDependsOn(commonProjectPath)
val commonProj = project(commonProjectPath)
val apiDeps = ModuleDependenciesExtension.getApiDeps(commonProj)
val implDeps = ModuleDependenciesExtension.getImplDeps(commonProj)
val processorDeps = ModuleDependenciesExtension.getAnnotationProcessorDeps(commonProj)
if (suffix == "fabric") {
    for (dep in (apiDeps + implDeps).distinct()) {
        project.evaluationDependsOn(":modules:$dep:$dep-fabric")
    }
}
for (dep in processorDeps) {
    project.dependencies.add("annotationProcessor",
        project.dependencies.project(mapOf("path" to ":modules:$dep:$dep-common")))
}
if (apiDeps.isNotEmpty() || implDeps.isNotEmpty()) {
    for (dep in apiDeps) {
        val depPath = ":modules:$dep:$dep-$suffix"
        val depProj = project(depPath)
        // Fabric: source set output avoids Loom's mod processing issues
        // NeoForge: use project dependency so MDG handles module path correctly (no JPMS conflict)
        if (suffix == "fabric") {
            val output = project.files(project.provider { depProj.sourceSets.main.get().output })
            project.dependencies.add("api", output)
            project.afterEvaluate {
                if (project.configurations.findByName("modRuntimeClasspath") != null) {
                    project.dependencies.add("modRuntimeClasspath", output)
                }
            }
        } else {
            project.dependencies.add("api", project.dependencies.project(mapOf("path" to depPath)))
        }
    }
    for (dep in implDeps) {
        val depPath = ":modules:$dep:$dep-$suffix"
        val depProj = project(depPath)
        if (suffix == "fabric") {
            val output = project.files(project.provider { depProj.sourceSets.main.get().output })
            project.dependencies.add("implementation", output)
            project.afterEvaluate {
                if (project.configurations.findByName("modRuntimeClasspath") != null) {
                    project.dependencies.add("modRuntimeClasspath", output)
                }
            }
        } else {
            project.dependencies.add("implementation", project.dependencies.project(mapOf("path" to depPath)))
        }
    }
}
// Copy dep names to current project so module-common.gradle.kts POM generation picks them up
if (apiDeps.isNotEmpty() || implDeps.isNotEmpty()) {
    project.extra["_moduleDepsApi"] = apiDeps
    project.extra["_moduleDepsImpl"] = implDeps

    if (suffix == "fabric") {
        project.pluginManager.withPlugin("fabric-loom") {
            for (dep in (apiDeps + implDeps).distinct()) {
                val depProj = project(":modules:$dep:$dep-$suffix")
                val forwardedConfigurations = mapOf(
                    "api" to if (dep in apiDeps) "api" else "implementation",
                    "modApi" to if (dep in apiDeps) "modApi" else "modImplementation",
                    "implementation" to "runtimeOnly",
                    "modImplementation" to "modRuntimeOnly",
                    "runtimeOnly" to "runtimeOnly",
                    "modRuntimeOnly" to "modRuntimeOnly"
                )
                depProj.configurations.matching { it.name in forwardedConfigurations }.all {
                    val destination = forwardedConfigurations.getValue(name)
                    dependencies.withType(org.gradle.api.artifacts.ExternalModuleDependency::class.java).all {
                        project.dependencies.add(destination, copy())
                    }
                }
            }
        }
    }
}

if (suffix == "fabric") {
    val moduleClosure = linkedSetOf<String>()
    val pending = ArrayDeque(apiDeps + implDeps)
    while (pending.isNotEmpty()) {
        val dep = pending.removeFirst()
        if (!moduleClosure.add(dep)) continue
        val depCommon = project.evaluationDependsOn(":modules:$dep:$dep-common")
        pending.addAll(ModuleDependenciesExtension.getApiDeps(depCommon) + ModuleDependenciesExtension.getImplDeps(depCommon))
    }
    project.pluginManager.withPlugin("fabric-loom") {
        val loom = project.extensions.getByName("loom")
        @Suppress("UNCHECKED_CAST")
        val mods = loom.javaClass.getMethod("getMods").invoke(loom) as NamedDomainObjectContainer<Any>
        fun group(owner: Project) {
            val settings = mods.maybeCreate(owner.name)
            settings.javaClass.getMethod("sourceSet", String::class.java, Project::class.java).invoke(settings, "main", owner)
        }
        group(project)
        for (dep in moduleClosure) {
            group(project.evaluationDependsOn(":modules:$dep:$dep-$suffix"))
        }
    }
}

// Make common resources visible at configuration time so Loom's
// TransitiveAccessWidenerMappingsProcessor can find AW files.
if (suffix == "fabric") {
    val sourceSets = project.extensions.getByType(org.gradle.api.plugins.JavaPluginExtension::class.java).sourceSets
    sourceSets.getByName("main").resources.srcDir(project(commonProjectPath).file("src/main/resources"))
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(configurations.getByName("commonJava"))
    source(configurations.getByName("commonJava"))
}

tasks.named<ProcessResources>("processResources") {
    // Fabric already receives the common resource directory through main.resources
    // above so Loom can discover access wideners during configuration. Adding the
    // commonResources artifact again would copy every common resource twice.
    if (suffix != "fabric") {
        dependsOn(configurations.getByName("commonResources"))
        from(configurations.getByName("commonResources"))
    }
}

tasks.named<Javadoc>("javadoc") {
    dependsOn(configurations.getByName("commonJava"))
    source(configurations.getByName("commonJava"))
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(configurations.getByName("commonJava"))
    from(configurations.getByName("commonJava"))
    // Fabric's main source set already contains the common resources. Keep the
    // explicit copy only for loaders that do not register that resource directory.
    if (suffix != "fabric") {
        dependsOn(configurations.getByName("commonResources"))
        from(configurations.getByName("commonResources"))
    }
}
