plugins {
    kotlin("jvm") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.ssg"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.nexomc.com/releases")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("com.nexomc:nexo:1.10.0") //Nexo 1.X -> 1.X.0
    compileOnly("com.github.NEZNAMY", "TAB-API", "5.2.5")
    implementation("net.objecthunter:exp4j:0.4.8")
    implementation(kotlin("stdlib"))
    implementation("org.apache.commons:commons-lang3:3.18.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    // Faz o artefato final ser o sombreado (sem o sufixo -all)
    shadowJar {
        archiveBaseName.set("SSGItemEvolution") // nome do jar
        archiveClassifier.set("")

        // Após gerar o jar, copia pro servidor de testes
        doLast {
            copy {
                from(archiveFile)
                into("C:/Users/Demarky/Documents/ServerLeve/plugins")
            }
            println("✅ Plugin copiado para a pasta do servidor de testes!")
        }
    }

    build {
        dependsOn(shadowJar)
    }
}
