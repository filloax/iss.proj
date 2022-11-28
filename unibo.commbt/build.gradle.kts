plugins {
	kotlin("jvm") version "1.6.21"
	distribution
}

version = "1.0"

java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
	mavenCentral()
	flatDir {   dirs("../unibolibs")	 }
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.10")

	implementation("org.json:json:20220924")
	implementation(":2p301")

	// https://mvnrepository.com/artifact/io.ultreia/bluecove
	// Fork aggiornato
	// Non utilizzato a causa di complicazioni con l'installazione su alcune piattaforme
//	implementation("io.ultreia:bluecove:2.1.1")

	/* UNIBO *************************************************************************************************************** */
	implementation(":it.unibo.qakactor-2.7")
    implementation(":unibo.comm22-1.4")
	implementation(":uniboInterfaces")
	testImplementation("junit:junit:4.13.2")
}

task<JavaExec>("runClient") {
	mainClass.set("it.unibo.comm22.bluetooth.main.SimpleClientKt")
	classpath = java.sourceSets["main"].runtimeClasspath
	standardInput = System.`in`
}