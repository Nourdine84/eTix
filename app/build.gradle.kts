plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize") // ✅ OBLIGATOIRE
    id("org.jetbrains.kotlin.kapt")

    // 🔥 SAFE ARGS
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.etix"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.etix"
        minSdk = 21
        targetSdk = 34
        // Build « B » de test de mise à jour (CI émulateur uniquement) : même code, versionCode + offset.
        val versionOffset = (project.findProperty("etixVersionCodeOffset") as String?)?.toInt() ?: 0
        versionCode = 8 + versionOffset
        versionName = "1.7.0-lot7" + ((project.findProperty("etixVersionNameSuffix") as String?) ?: "")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Clé QA dédiée et durable : fournie UNIQUEMENT par variables d'environnement
    // (secrets GitHub en CI). Jamais de chemin, mot de passe ou keystore dans le dépôt.
    val qaKeystoreFile = System.getenv("ETIX_QA_KEYSTORE_FILE")?.takeIf { it.isNotBlank() }
    val qaSigningRequired = System.getenv("ETIX_QA_SIGNING_REQUIRED") == "true"
    if (qaSigningRequired && qaKeystoreFile == null) {
        throw GradleException("ETIX_QA_SIGNING_REQUIRED=true mais ETIX_QA_KEYSTORE_FILE absent : APK QA non signable avec la clé QA durable.")
    }

    signingConfigs {
        if (qaKeystoreFile != null) {
            create("qa") {
                storeFile = file(qaKeystoreFile)
                storePassword = System.getenv("ETIX_QA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ETIX_QA_KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: "etix-qa"
                // PKCS12 : mot de passe de clé = mot de passe du keystore
                keyPassword = System.getenv("ETIX_QA_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
                    ?: System.getenv("ETIX_QA_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        // Build de test installable À CÔTÉ de l'app existante :
        // package com.etix.qa → données, signature et désinstallation totalement séparées de com.etix.
        // Signature : clé QA durable si fournie (CI), sinon clé debug locale (build local non
        // destiné à être installé par-dessus un APK QA de la CI).
        create("qa") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            matchingFallbacks += listOf("debug")
            if (qaKeystoreFile != null) {
                signingConfig = signingConfigs.getByName("qa")
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Captures Robolectric (rendu natif) : mémoire saturée au-delà de ~120 tests dans une seule JVM (OOM constaté
        // au lot 7). Tas élargi et nouvelle JVM toutes les 8 classes. N'affecte que les tests, pas l'application.
        unitTests.all {
            it.maxHeapSize = "2g"
            it.setForkEvery(8)
        }
    }

    // ✅ LINT BASELINE
    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = true
    }
}

/**
 * 🔴 CORRECTION CRITIQUE
 * 👉 Empêche KAPT (Room) de traiter les tests unitaires JVM
 * 👉 Évite le crash: processingEnv must not be null
 */
tasks.withType<org.jetbrains.kotlin.gradle.internal.KaptWithoutKotlincTask>()
    .configureEach {
        onlyIf {
            !name.contains("Test", ignoreCase = true)
        }
    }

kapt {
    correctErrorTypes = true

    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
    }
}

dependencies {

    // --- Core UI ---
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // --- Navigation ---
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // --- Room ---
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // --- Lifecycle ---
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")

    // --- Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // --- Tests (JVM purs) ---
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")

    // 🔍 ML Kit – Text Recognition (OCR)
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // 📷 CameraX
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
}
