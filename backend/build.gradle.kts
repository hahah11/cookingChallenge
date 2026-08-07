plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.openapi.generator") version "7.24.0"
}

group = "at.fraihs"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

extra["springModulithVersion"] = "2.1.0"
extra["jmoleculesVersion"] = "2023.3.2"
extra["jmoleculesArchunitVersion"] = "0.28.0"
extra["archunitVersion"] = "1.4.1"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-liquibase")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.modulith:spring-modulith-starter-core")
	implementation("com.github.f4b6a3:tsid-creator:5.2.6")
	implementation("org.mapstruct:mapstruct:1.6.3")
	implementation("org.jmolecules:jmolecules-ddd")
	implementation("org.jmolecules:jmolecules-events")
	implementation("io.swagger.core.v3:swagger-annotations-jakarta:2.2.31")
	implementation("org.openapitools:jackson-databind-nullable:0.2.6")
	runtimeOnly("org.postgresql:postgresql")
	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	annotationProcessor("org.projectlombok:lombok")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.modulith:spring-modulith-starter-test")
	testImplementation("com.tngtech.archunit:archunit-junit5:${property("archunitVersion")}")
	testImplementation("org.jmolecules.integrations:jmolecules-archunit:${property("jmoleculesArchunitVersion")}")
	testRuntimeOnly("com.h2database:h2")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.modulith:spring-modulith-bom:${property("springModulithVersion")}")
		mavenBom("org.jmolecules:jmolecules-bom:${property("jmoleculesVersion")}")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

val openApiSpec = rootDir.resolve("../openapi/cookingchallenge-api.yaml").canonicalPath
val openApiOutputDir = layout.buildDirectory.dir("generated/openapi").get().asFile

openApiGenerate {
	generatorName.set("spring")
	inputSpec.set(openApiSpec)
	outputDir.set(openApiOutputDir.path)
	apiPackage.set("at.fraihs.cookoff.shared.web.openapi.api")
	modelPackage.set("at.fraihs.cookoff.shared.web.openapi.model")
	modelNameSuffix.set("RestDto")
	configOptions.set(
		mapOf(
			"interfaceOnly" to "true",
			"skipDefaultInterface" to "true",
			"useTags" to "true",
			"useSpringBoot4" to "true",
		)
	)
}

sourceSets {
	main {
		java {
			srcDir(openApiOutputDir.resolve("src/main/java"))
		}
	}
}

tasks.named("compileJava") {
	dependsOn("openApiGenerate")
}
