import java.util.Properties

plugins { id("com.android.application") }

// Clave de firma propia: keystore.properties (NO se sube a GitHub). Sin ella se usa la clave debug.
val keyProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "ve.tasasbcv"
    compileSdk = 35
    defaultConfig {
        applicationId = "ve.tasasbcv"
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "2.0"
        // Dirección de GitHub Pages desde donde llegan la interfaz (OTA) y version.json
        buildConfigField("String", "OTA_BASE", "\"${providers.gradleProperty("otaBase").get()}\"")
    }
    buildFeatures { buildConfig = true }
    signingConfigs {
        if (keyProps.isNotEmpty()) create("release") {
            storeFile = file(keyProps.getProperty("storeFile"))
            storePassword = keyProps.getProperty("storePassword")
            keyAlias = keyProps.getProperty("keyAlias")
            keyPassword = keyProps.getProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
}
