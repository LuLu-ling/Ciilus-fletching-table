import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    alias(libs.plugins.architectury.plugin)
    alias(libs.plugins.architectury.loom) apply false
    alias(libs.plugins.shadow) apply false
    `maven-publish`
}

architectury {
    minecraft = libs.versions.minecraft.get()
}

subprojects {
    apply(plugin = "dev.architectury.loom")
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")

    val minecraftVersion = rootProject.libs.versions.minecraft.get()
    val modVersion = project.property("mod_version") as String
    val mavenGroup = project.property("maven_group") as String
    val archivesBaseName = project.property("archives_base_name") as String

    version = "$minecraftVersion-$modVersion"
    group = mavenGroup

    extensions.configure<BasePluginExtension> {
        archivesName.set("$archivesBaseName-${project.name}")
    }

    repositories {
        mavenCentral()
        maven {
            name = "TerraformersMC"
            url = uri("https://maven.terraformersmc.com/releases")
        }
        maven {
            name = "Shedaniel"
            url = uri("https://maven.shedaniel.me/")
        }
        maven {
            name = "Architectury"
            url = uri("https://maven.architectury.dev/")
        }
        maven {
            name = "NeoForged"
            url = uri("https://maven.neoforged.net/releases")
        }
    }

    val loom = project.extensions.getByType(LoomGradleExtensionAPI::class.java)

    dependencies {
        "minecraft"(rootProject.libs.minecraft)
        "mappings"(loom.officialMojangMappings())
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    configure<JavaPluginExtension> {
        withSourcesJar()
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
