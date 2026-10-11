// Modified for Kotlin Reforged by SvyatON, 2026-10-11.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import groovy.json.JsonSlurper
plugins {
    `java-library`
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "2.4.0"
}
version = "7.0.0+1.21.6-26.3"
group = "thedarkcolour"
java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach { compilerOptions.jvmTarget.set(JvmTarget.JVM_21) }
repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net/")
    maven("https://libraries.minecraft.net")
}
val additionalKotlinLibraries = ((JsonSlurper().parse(file("gradle/additional-kotlin-libraries.json")) as Map<*, *>)["libraries"] as List<*>).map {
    val library = it as Map<*, *>
    Triple(library["group"] as String, library["artifact"] as String, library["version"] as String)
}
sourceSets {
    create("fakecraft").java.srcDir("src/fakecraft/java")
    create("langForge") {
        java.srcDir("src/kfflang/forge/java")
        kotlin.srcDir("src/kfflang/forge/kotlin")
        resources.srcDir("src/kfflang/forge/resources")
    }
    create("libForge") { kotlin.srcDirs("src/kfflib/forge/kotlin", "src/kfflib/common/kotlin") }
    create("modForge") { kotlin.srcDir("src/kffmod/forge/kotlin"); resources.srcDir("src/kffmod/common/resources") }
}
listOf("langForge", "libForge", "modForge").forEach { source ->
    tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>(sourceSets[source].getCompileTaskName("kotlin")) {
        compilerOptions.moduleName.set("thedarkcolour:Kotlin for Forge Reforged_$source")
    }
}
dependencies {
    sourceSets.forEach { s ->
        val c=s.compileOnlyConfigurationName
        add(c,"org.jetbrains.kotlin:kotlin-stdlib:2.4.0")
        add(c,"org.jetbrains.kotlin:kotlin-reflect:2.4.0")
        add(c,"org.joml:joml:1.10.5")
        add(c,"com.electronwill.night-config:core:3.7.1")
        add(c,"org.apache.logging.log4j:log4j-core:2.22.1")
        add(c,"org.ow2.asm:asm:9.5")
        add(c,"net.minecraftforge:eventbus:7.0-beta.10")
        add(c,"net.minecraftforge:unsafe:0.9.2")
        add(c,"net.minecraftforge:forgespi:7.1.5")
        add(c,"net.minecraftforge:mergetool-api:1.0")
        listOf("fmlcore","fmlloader","javafmllanguage").forEach { add(c,"net.minecraftforge:$it:1.21.8-58.1.0") { isTransitive=false } }
    }
    add("libForgeCompileOnly",sourceSets["fakecraft"].output)
    add("libForgeCompileOnly",sourceSets["langForge"].output)
    add("modForgeCompileOnly",sourceSets["langForge"].output)
    add("modForgeCompileOnly","net.minecraftforge:forge:1.21.8-58.1.0:universal") { isTransitive=false }
}

tasks.named<ProcessResources>("processModForgeResources") {
    from("src/kffmod/templates/META-INF/mods.toml") {
        into("META-INF")
        expand("kff_version" to project.version, "min_mc_version" to "1.21.6", "unsupported_mc_version" to "26.4")
    }
}
listOf("langForge","libForge","modForge").forEach { s ->
    tasks.register<Jar>(s+"Jar") {
        from(sourceSets[s].output)
        from("LICENSE")
        archiveBaseName.set(s)
        manifest.attributes("FMLModType" to (if(s=="langForge") "LANGPROVIDER" else if(s=="libForge") "GAMELIBRARY" else "MOD"), "Implementation-Version" to project.version, "Automatic-Module-Name" to "thedarkcolour.kotlinforforge."+s.removeSuffix("Forge"))
    }
    tasks.register<Jar>(s + "SourcesJar") {
        from(sourceSets[s].allSource)
        from("LICENSE")
        archiveBaseName.set(s)
        archiveClassifier.set("sources")
    }
}

tasks.register<Jar>("sourcesJar") { archiveBaseName.set("kotlinforforge"); archiveClassifier.set("sources"); from("src"); from("LICENSE") }
val kotlinRuntime = listOf(
    Triple("org.jetbrains.kotlin", "kotlin-stdlib", "2.4.0"),
    Triple("org.jetbrains.kotlin", "kotlin-stdlib-jdk7", "2.4.0"),
    Triple("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", "2.4.0"),
    Triple("org.jetbrains.kotlin", "kotlin-reflect", "2.4.0"),
    Triple("org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm", "1.11.0"),
    Triple("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", "1.11.0"),
    Triple("org.jetbrains.kotlinx", "kotlinx-serialization-core-jvm", "1.11.0"),
    Triple("org.jetbrains.kotlinx", "kotlinx-serialization-json-jvm", "1.11.0"),
) + additionalKotlinLibraries
publishing {
    repositories { maven { name = "Distribution"; url = uri(layout.buildDirectory.dir("maven")) } }
    publications {
        for ((id, source) in mapOf("kfflang" to "langForge", "kfflib" to "libForge", "kffmod" to "modForge", "kotlinforforge" to "")) {
            register<MavenPublication>(id) {
                artifactId = id
                if (source.isNotEmpty()) {
                    artifact(tasks.named(source + "Jar"))
                    artifact(tasks.named(source + "SourcesJar"))
                } else {
                    artifact(layout.buildDirectory.file("kotlinforforge-${project.version}-maven.jar")) { classifier = null }
                    artifact(tasks.named("sourcesJar"))
                }
                pom {
                    name.set("Kotlin Reforged")
                    description.set("Unofficial Kotlin for Forge adapter for Forge EventBus 7")
                    url.set("https://github.com/thedarkcolour/KotlinForForge")
                    licenses { license { name.set("LGPL-2.1-only"); url.set("https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html") } }
                    withXml {
                        val dependencies = asNode().appendNode("dependencies")
                        val modules = when (id) {
                            "kotlinforforge" -> listOf("kfflang", "kfflib", "kffmod")
                            "kfflib", "kffmod" -> listOf("kfflang")
                            else -> emptyList()
                        }.map { Triple("thedarkcolour", it, project.version.toString()) }
                        for ((group, artifact, version) in modules + kotlinRuntime) {
                            val dependency = dependencies.appendNode("dependency")
                            dependency.appendNode("groupId", group)
                            dependency.appendNode("artifactId", artifact)
                            dependency.appendNode("version", version)
                            dependency.appendNode("scope", "compile")
                        }
                    }
                }
            }
        }
    }
}
tasks.register("buildForge") { dependsOn("langForgeJar","libForgeJar","modForgeJar","sourcesJar") }
tasks.named("assemble") { dependsOn("buildForge") }


tasks.withType<Jar>().configureEach { isPreserveFileTimestamps = false; isReproducibleFileOrder = true }
