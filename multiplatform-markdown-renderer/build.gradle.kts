import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish") version "0.25.3"
}

android {
    namespace = "com.mikepenz.markdown"
    compileSdk = Versions.androidCompileSdk

    defaultConfig {
        minSdk = Versions.androidMinSdk
        targetSdk = Versions.androidTargetSdk
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.composeCompiler
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"

        kotlinOptions {
            if (project.findProperty("composeCompilerReports") == "true") {
                freeCompilerArgs += listOf(
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${project.buildDir.absolutePath}/compose_compiler"
                )
            }
            if (project.findProperty("composeCompilerMetrics") == "true") {
                freeCompilerArgs += listOf(
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${project.buildDir.absolutePath}/compose_compiler"
                )
            }
        }
    }
}

kotlin {
    applyDefaultHierarchyTemplate()

    targets.all {
        compilations.all {
            compilerOptions.configure {
                languageVersion.set(KotlinVersion.KOTLIN_1_9)
                apiVersion.set(KotlinVersion.KOTLIN_1_9)
            }
        }
    }
    androidTarget {
        publishLibraryVariants("release")
    }
    jvm {
        compilations {
            all {
                kotlinOptions.jvmTarget = "11"
            }
        }

        testRuns["test"].executionTask.configure {
            useJUnit {
                excludeCategories("org.intellij.markdown.ParserPerformanceTest")
            }
        }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    js(IR) {
        nodejs()
    }
    macosX64()
    macosArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val fileBasedTest by creating {
            dependsOn(commonTest)
        }
        val jvmTest by getting {
            dependsOn(fileBasedTest)
        }
        val jsTest by getting {
            dependsOn(fileBasedTest)
        }
        val nativeMain by getting {
            dependsOn(commonMain)
        }
        val nativeTest by getting {
            dependsOn(fileBasedTest)
        }
        val nativeSourceSets = listOf(
            "macosX64",
            "macosArm64",
            "ios",
            "iosSimulatorArm64"
        ).map { "${it}Main" }
        for (set in nativeSourceSets) {
            getByName(set).dependsOn(nativeMain)
        }
        val nativeTestSourceSets = listOf(
            "macosX64",
            "macosArm64"
        ).map { "${it}Test" }
        for (set in nativeTestSourceSets) {
            getByName(set).dependsOn(nativeTest)
            getByName(set).dependsOn(fileBasedTest)
        }
    }
}

dependencies {
    commonMainApi(Deps.Markdown.core)

    commonMainCompileOnly(compose.runtime)
    commonMainCompileOnly(compose.ui)
    commonMainCompileOnly(compose.foundation)

    "androidMainImplementation"(Deps.Compose.coilCompose) {
        exclude("androidx.compose.foundation")
        exclude("androidx.compose.ui")
    }
}

tasks.dokkaHtml.configure {
    dokkaSourceSets {
        configureEach {
            noAndroidSdkLink.set(false)
        }
    }
}

tasks.create<Jar>("javadocJar") {
    dependsOn("dokkaJavadoc")
    archiveClassifier.set("javadoc")
    from("${layout.buildDirectory}/javadoc")
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.S01)
    signAllPublications()
}

publishing {
    repositories {
        maven {
            name = "installLocally"
            setUrl("${rootProject.layout.buildDirectory}/localMaven")
        }
    }
}
