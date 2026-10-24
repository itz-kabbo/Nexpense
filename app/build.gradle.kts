plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.roborazzi)
}

val releaseStoreFile = providers.gradleProperty("releaseStoreFile")
    .orElse(providers.environmentVariable("RELEASE_STORE_FILE")).orNull
val releaseStorePassword = providers.gradleProperty("releaseStorePassword")
    .orElse(providers.environmentVariable("RELEASE_STORE_PASSWORD")).orNull
val releaseKeyAlias = providers.gradleProperty("releaseKeyAlias")
    .orElse(providers.environmentVariable("RELEASE_KEY_ALIAS")).orNull
val releaseKeyPassword = providers.gradleProperty("releaseKeyPassword")
    .orElse(providers.environmentVariable("RELEASE_KEY_PASSWORD")).orNull
val releaseSigningConfigured = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.nexusbench.nexpense"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nexusbench.nexpense"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (releaseSigningConfigured) {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword!!
                keyAlias = releaseKeyAlias!!
                keyPassword = releaseKeyPassword!!
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                it.systemProperty("roborazzi.test.record", "true")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":parser"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.matching { it.name == "testDebugUnitTest" }.configureEach {
    dependsOn(":parser:test")
}

tasks.register("checkNoInternetPermission") {
    description = "Fails if release manifest contains android.permission.INTERNET"
    dependsOn("processReleaseMainManifest")
    doLast {
        val intermediates = layout.buildDirectory.asFile.get()
        val manifestFiles = intermediates.walkTopDown().filter {
            it.name == "AndroidManifest.xml" && (it.path.contains("merged_manifest") || it.path.contains("processRelease"))
        }.toList()

        var checkedAny = false
        for (file in manifestFiles) {
            val content = file.readText()
            if (content.contains("android.permission.INTERNET")) {
                throw GradleException("Security violation: ${file.path} contains android.permission.INTERNET!")
            }
            checkedAny = true
        }

        if (checkedAny) {
            println("SUCCESS: Verified release manifest contains NO android.permission.INTERNET.")
        } else {
            // Check raw manifest file as fallback
            val srcManifest = file("src/main/AndroidManifest.xml")
            if (srcManifest.readText().contains("android.permission.INTERNET")) {
                throw GradleException("Security violation: ${srcManifest.path} contains android.permission.INTERNET!")
            }
            println("SUCCESS: Verified source AndroidManifest contains NO android.permission.INTERNET.")
        }
    }
}

tasks.matching { it.name in setOf("assembleRelease", "bundleRelease", "packageRelease") }.configureEach {
    dependsOn("checkNoInternetPermission")
    doFirst {
        if (!releaseSigningConfigured) {
            throw GradleException(
                "Release signing is not configured. Set releaseStoreFile, releaseStorePassword, " +
                    "releaseKeyAlias, and releaseKeyPassword in local Gradle properties or environment variables."
            )
        }
        if (android.defaultConfig.applicationId.orEmpty().split('.').any { it == "example" || it == "yourco" }) {
            throw GradleException("Replace the placeholder applicationId before creating a release build.")
        }
    }
}
