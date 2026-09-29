plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val keystoreFile = System.getenv("KEYSTORE_FILE")?.takeIf { it.isNotBlank() }
val keystorePassword = System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
val keyAliasEnv = System.getenv("KEY_ALIAS")?.takeIf { it.isNotBlank() }
val keyPasswordEnv = System.getenv("KEY_PASSWORD")?.takeIf { it.isNotBlank() }
val hasReleaseSigning = listOf(keystoreFile, keystorePassword, keyAliasEnv, keyPasswordEnv).all { it != null }

android {
    namespace = "com.jumpadventure.game"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.synfusion.jump"
        minSdk = 24
        targetSdk = 34
        versionCode = providers.gradleProperty("versionCode").map { it.toInt() }.orElse(1).get()
        versionName = "1.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(requireNotNull(keystoreFile))
                storePassword = requireNotNull(keystorePassword)
                keyAlias = requireNotNull(keyAliasEnv)
                keyPassword = requireNotNull(keyPasswordEnv)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation("junit:junit:4.13.2")
}
