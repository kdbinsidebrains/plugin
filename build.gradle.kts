import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform
import java.util.*

/**
 * Shortcut for <code>project.findProperty(key).toString()</code>.
 */
fun properties(key: String) = project.findProperty(key).toString()

/**
 * Shortcut for <code>System.getenv().getOrDefault(key, default).toString()</code>.
 */
fun environment(key: String, default: String) = System.getenv().getOrDefault(key, default).toString()

val versionProps = Properties().apply {
    file("version.properties").inputStream().use { load(it) }
}

version = versionProps["pluginVersion"] as String

val javaVersion = JavaVersion.VERSION_17
val platformType = properties("platformType")
val platformVersion = properties("platformVersion")

plugins {
    id("java")
    id("idea")
    id("signing")
    id("maven-publish")
    id("org.jetbrains.changelog") version "2.2.1"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

repositories {
    mavenLocal()
    mavenCentral()

    intellijPlatform {
        snapshots()
        defaultRepositories()
    }
}

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

sourceSets {
    main {
        java {
            srcDirs("src/main/java", "src/main/gen")
        }
        resources {
            srcDirs("src/main/resources")
        }
    }
    test {
        java {
            srcDirs("src/test/java")
        }
        resources {
            srcDirs("src/test/resources")
        }
    }
}


dependencies {
    // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
    intellijPlatform {
        create(platformType, platformVersion)

        zipSigner()
        pluginVerifier()

        jetbrainsRuntime()

        bundledPlugin("com.intellij.java")
        plugins(properties("platformPlugins").split(','))

        testFramework(TestFrameworkType.JUnit5)
    }

    implementation("org.jfree:jfreechart:1.5.5")
    implementation("org.jfree:org.jfree.svg:5.0.6")
    implementation("org.apache.poi:poi:5.4.1")
    implementation("org.apache.poi:poi-ooxml:5.4.1")

    testImplementation("org.mockito:mockito-core:5.17.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.3")
}

// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellijPlatform {
    pluginConfiguration {
        id = properties("pluginId")
        name = properties("pluginName")
        version = project.version as String
        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = provider { null }
        }
        changeNotes.set(provider {
            changelog.renderItem(
                changelog.get(project.version as String)
                    .withHeader(false)
                    .withEmptySections(false),
                Changelog.OutputType.HTML
            )
        })
    }

    publishing {
        token.set(environment("KDBINSIDEBRAINS_RELEASE_PLUGIN_TOKEN", ""))
//        channels.set(listOf(environment("CHANNELS", "Testing")))
    }

    pluginVerification {
        ides {
            properties("pluginVerifierVersions").split(",").forEach { ide(it.trim()) }
        }
    }
}

changelog {
    val projectVersion = project.version as String
    version.set(projectVersion)
    header.set("[$projectVersion] - ${org.jetbrains.changelog.date()}")
    groups.set(listOf("Added", "Changed", "Removed", "Fixed"))
}

tasks {
    java {
        withSourcesJar()
        withJavadocJar()
    }

    named("javadoc") {
        val docs = this as Javadoc
        val options = docs.options as StandardJavadocDocletOptions
        options.addBooleanOption("html5", true)
        options.addBooleanOption("Xdoclint:none", true)
    }

    named("sourcesJar") {
        dependsOn("generateLexer")

        val spec = this as CopySpec
        spec.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    generateParser {
        purgeOldFiles.set(true)

        pathToPsiRoot.set("org/kdb/inside/brains/psi")
        pathToParser.set("org/kdb/inside/brains/parser/QParser.java")
        sourceFile.set(file("src/main/resources/org/kdb/inside/brains/q.bnf"))
        targetRootOutputDir.set(file("src/main/gen"))
    }

    generateLexer {
        dependsOn(generateParser)

        targetFile("QLexer")
        purgeOldFiles.set(false) // if enabled, removes files created by generateParser
        sourceFile.set(file("src/main/resources/org/kdb/inside/brains/q.flex"))
        targetOutputDir.set(file("src/main/gen/org/kdb/inside/brains"))
    }

    compileJava {
        dependsOn(generateLexer)
        options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    processTestResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    publishing {
        repositories {
            maven {
                name = "CentralPortal"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = environment("MAVEN_USERNAME", "")
                    password = environment("MAVEN_PASSWORD", "")
                }
            }
        }

        publications {
            create<MavenPublication>("mavenJava") {
                groupId = properties("mavenGroupId")
                artifactId = properties("mavenArtifactId")
                version = project.version.toString()

                artifact(project.tasks.named("jar")) {
                    classifier = null
                }

                artifact(project.tasks.named("sourcesJar"))

                artifact(project.tasks.named("javadocJar"))

                pom {
                    name.set("KdbInsideBrains")
                    description =
                        "Open-Source IntelliJ-based IDEA plugin for <a href=\"https://kx.com/\">kdb+</a> time-series realtime database."
                    url = "https://www.kdbinsidebrains.dev"
                    licenses {
                        license {
                            name = "The Apache License, Version 2.0"
                            url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                        }
                    }
                    developers {
                        developer {
                            id = "kdbinsidebrains"
                            name = "Support KdbInsideBrains"
                            email = "support@kdbinsidebrains.dev"
                        }
                    }
                    scm {
                        url = "https://github.com/kdbinsidebrains/plugin"
                        connection = "scm:git:git://github.com/kdbinsidebrains/plugin.git"
                        developerConnection = "scm:git:ssh://github.com:kdbinsidebrains/plugin.git"
                    }
                }
            }
        }
    }

    signing {
        sign(publishing.publications["mavenJava"])
    }
}