package cc.sighs.gradle

import groovy.util.Node
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.plugins.BasePluginExtension

private data class MavenDependency(
    val group: String,
    val artifact: String,
    val version: String,
    val scope: String,
    val exclusions: Set<Pair<String, String>> = emptySet()
)

private fun Project.isJarJarPublication(): Boolean =
    name == "forge" || name == "neoforge" || name.endsWith("-forge") || name.endsWith("-neoforge")

fun Project.configureMavenDependencies(xml: Node) {
    fun Node.hasName(expected: String): Boolean =
        name().toString() == expected || name().toString().endsWith("}$expected")

    xml.children().filterIsInstance<Node>().filter { it.hasName("dependencies") }
        .forEach { xml.remove(it) }

    val whitelist = (extensions.extraProperties.properties["mavenDependencyWhitelist"] as? Iterable<*>)
        ?.map { it.toString() }?.toSet().orEmpty()
    val embeddedConfiguration = when {
        isJarJarPublication() -> "jarJar"
        name == "fabric" || name.endsWith("-fabric") -> "include"
        else -> null
    }
    val embedded = if (embeddedConfiguration != null) {
        configurations.findByName(embeddedConfiguration)?.dependencies
            ?.withType(ExternalModuleDependency::class.java)
            ?.map { "${it.group}:${it.name}" }?.toSet().orEmpty()
    } else emptySet()
    val dependencies = linkedMapOf<String, MavenDependency>()

    fun add(dependency: MavenDependency) {
        val key = "${dependency.group}:${dependency.artifact}"
        val previous = dependencies[key]
        dependencies[key] = if (previous == null) dependency else {
            val preferred = if (previous.scope == "compile") previous else dependency
            preferred.copy(exclusions = previous.exclusions + dependency.exclusions)
        }
    }

    for ((configurationName, scope) in listOf(
        "api" to "compile", "compileOnlyApi" to "compile",
        "modApi" to "compile", "compileOnly" to "compile", "modCompileOnly" to "compile",
        "implementation" to "runtime", "modImplementation" to "runtime",
        "runtimeOnly" to "runtime", "modRuntimeOnly" to "runtime", "jarJar" to "runtime",
        "include" to "runtime"
    )) {
        val configuration = configurations.findByName(configurationName) ?: continue
        configuration.dependencies.withType(ExternalModuleDependency::class.java).forEach { dependency ->
            val group = dependency.group ?: return@forEach
            val version = dependency.version ?: return@forEach
            val key = "$group:${dependency.name}"
            if (key !in embedded && group !in whitelist &&
                dependency.name !in whitelist && key !in whitelist) return@forEach
            val exclusions = dependency.excludeRules.map {
                (it.group ?: "*") to (it.module ?: "*")
            }.toSet() + if (dependency.isTransitive) emptySet() else setOf("*" to "*")
            add(MavenDependency(group, dependency.name, version, scope, exclusions))
        }
        if (!path.startsWith(":modules:")) {
            configuration.dependencies.withType(ProjectDependency::class.java).forEach { dependency ->
                if (!dependency.path.startsWith(":modules:")) return@forEach
                if (dependency.path.startsWith(":modules:compat:")) return@forEach
                val target = project(dependency.path)
                add(MavenDependency(
                    target.group.toString(),
                    target.extensions.getByType(BasePluginExtension::class.java).archivesName.get(),
                    target.version.toString(), scope
                ))
            }
        }
    }

    for ((property, scope) in listOf("_moduleDepsApi" to "compile", "_moduleDepsImpl" to "runtime")) {
        val modules = extensions.extraProperties.properties[property] as? Iterable<*> ?: continue
        val suffix = ModuleDependenciesExtension.getSuffix(name)
        for (module in modules) {
            val target = project(":modules:$module:$module-$suffix")
            add(MavenDependency(
                target.group.toString(),
                target.extensions.getByType(BasePluginExtension::class.java).archivesName.get(),
                target.version.toString(), scope
            ))
        }
    }

    if (dependencies.isEmpty()) return
    val container = xml.appendNode("dependencies")
    dependencies.values.forEach { dependency ->
        container.appendNode("dependency").apply {
            appendNode("groupId", dependency.group)
            appendNode("artifactId", dependency.artifact)
            appendNode("version", dependency.version)
            appendNode("scope", dependency.scope)
            if (dependency.exclusions.isNotEmpty()) {
                val exclusions = appendNode("exclusions")
                dependency.exclusions.forEach { (group, artifact) ->
                    exclusions.appendNode("exclusion").apply {
                        appendNode("groupId", group)
                        appendNode("artifactId", artifact)
                    }
                }
            }
        }
    }
}
