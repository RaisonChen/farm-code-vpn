import java.util.Properties

plugins {
 id("com.android.application")
 id("kotlin-android")
 // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
 id("dev.flutter.flutter-gradle-plugin")
}

// 读取 local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
 localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
 namespace = "cn.ys1231.appproxy"
 compileSdk = 36 // Flutter 插件要求至少 36
 ndkVersion = flutter.ndkVersion

 compileOptions {
 sourceCompatibility = JavaVersion.VERSION_17
 targetCompatibility = JavaVersion.VERSION_17
 }

 kotlin {
 compilerOptions {
 jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
 }
 }

 defaultConfig {
 applicationId = "cn.ys1231.appproxy"
 minSdk = 28
 targetSdk = 36
 versionCode = flutter.versionCode
 versionName = flutter.versionName
 }

 signingConfigs {
 getByName("debug") {
 // Debug signing config
 }
 // 不再创建 release signingConfig，直接用 debug 签名
 }

 buildTypes {
 release {
 signingConfig = signingConfigs.getByName("debug")
 isMinifyEnabled = true
 isShrinkResources = true
 proguardFiles(
 getDefaultProguardFile("proguard-android-optimize.txt"),
 "proguard-rules.pro"
 )
 }
 debug {
 signingConfig = signingConfigs.getByName("debug")
 }
 }

 packaging {
 dex {
 useLegacyPackaging = true
 }
 jniLibs {
 useLegacyPackaging = true
 }
 resources {
 excludes += "META-INF/INDEX.LIST"
 excludes += "META-INF/io.netty.versions.properties"
 excludes += "META-INF/DEPENDENCIES"
 excludes += "META-INF/LICENSE"
 excludes += "META-INF/LICENSE.txt"
 excludes += "META-INF/NOTICE"
 excludes += "META-INF/NOTICE.txt"
 }
 }
}

flutter {
 source = "../.."
}

dependencies {
 implementation(files("libs/tun2socks.aar"))
 implementation("com.google.code.gson:gson:2.13.2")
 implementation("io.ktor:ktor-server-cors:3.4.2")
 implementation("io.ktor:ktor-server-netty:3.4.2")
 implementation("io.ktor:ktor-server-auth:3.4.2")
 implementation("io.ktor:ktor-server-sse:3.4.2")
 implementation("io.modelcontextprotocol:kotlin-sdk-server:0.11.1")
 implementation("io.ktor:ktor-server-content-negotiation:3.4.2")
 implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.2")
}
