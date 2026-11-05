import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Endereço da API: local.properties (fora do git) pode sobrescrever, ex.:
//   rotacerta.api.debug=http://192.168.0.10:8085/transportadora_dev/api/v1/     (celular na mesma rede do PC)
//   rotacerta.api.debug=https://rotacerta.137-131-162-187.sslip.io/transportadora/api/v1/   (servidor de demonstração)
val locais = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
// 10.0.2.2 = o "localhost" do computador visto de dentro do emulador
val apiDebug = locais.getProperty("rotacerta.api.debug") ?: "http://10.0.2.2:8085/transportadora_dev/api/v1/"
// release: servidor de demonstração (VPS com Dokploy, HTTPS do Let's Encrypt)
val apiRelease = locais.getProperty("rotacerta.api.release") ?: "https://rotacerta.137-131-162-187.sslip.io/transportadora/api/v1/"

android {
    namespace = "br.com.mba.fametro.kotlintransportadora"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.mba.fametro.kotlintransportadora"
        minSdk = 26 // Android 8: java.time nativo e canais de notificação
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_URL", "\"$apiDebug\"")
            // simulador de GPS (anda sozinho pelo trajeto) só para demonstração
            buildConfigField("boolean", "SIMULADOR_GPS", "true")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "API_URL", "\"$apiRelease\"")
            buildConfigField("boolean", "SIMULADOR_GPS", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.osmdroid)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
