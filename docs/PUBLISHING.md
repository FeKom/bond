# Como publicar uma lib Java no Maven Central

Este documento explica o processo completo para publicar o Bond (ou qualquer lib Java) no Maven Central.

---

## Visão geral do fluxo

```
Código → Build local → Assinar com GPG → Upload para Sonatype (staging)
→ Validação automática → Release para Maven Central
```

O Maven Central é operado pela Sonatype. Você publica primeiro num repositório "staging", a Sonatype valida (assinatura, POM, javadoc, sources), e depois promove para o Central.

---

## Passo 1: Criar conta no Sonatype / Maven Central

### Opção A: Central Portal (novo, recomendado)
1. Acesse https://central.sonatype.com
2. Crie uma conta (pode usar GitHub login)
3. Verifique o namespace (groupId). Para `io.github.fekom`, você precisa provar que é dono do GitHub `fekom`
4. A verificação é feita criando um repositório temporário com o nome que eles pedem

### Opção B: OSSRH (legacy, ainda funciona)
1. Crie conta em https://issues.sonatype.org
2. Abra um ticket pedindo permissão para publicar no groupId desejado
3. Tempo de resposta: ~2 dias úteis

### Sobre o groupId
- `github.fekom` NÃO é aceito pelo Maven Central
- Precisa ser um domínio reverso que você controle: `io.github.fekom`, `com.fekom`, etc.
- Se não tem domínio próprio, use `io.github.<seu-usuario>`

---

## Passo 2: Gerar chave GPG

O Maven Central exige que todos os artefatos sejam assinados com GPG.

```bash
# Gerar chave
gpg --gen-key

# Listar chaves (copie o ID longo)
gpg --list-keys --keyid-format long

# Publicar chave num servidor público (obrigatório)
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>

# Exportar chave privada para usar no CI (base64)
gpg --export-secret-keys <KEY_ID> | base64
```

Guarde:
- `KEY_ID` — identificador da chave
- `GPG_PASSPHRASE` — senha da chave
- A chave privada em base64 (para CI)

---

## Passo 3: Configurar o build.gradle.kts

O `build.gradle.kts` precisa de:

### Plugins necessários
```kotlin
plugins {
    `java-library`
    `maven-publish`
    signing
}
```

### Gerar javadoc e sources JARs
```kotlin
java {
    withJavadocJar()
    withSourcesJar()
}
```

### Configurar publicação
```kotlin
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = "io.github.fekom"
            artifactId = "bond-spring-boot-starter"
            version = "0.1.0"

            pom {
                name.set("Bond")
                description.set("Byte-based rate limiter with token bucket for Spring Boot")
                url.set("https://github.com/FeKom/bond")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("fekom")
                        name.set("FeKom")
                        url.set("https://github.com/FeKom")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/FeKom/bond.git")
                    developerConnection.set("scm:git:ssh://github.com/FeKom/bond.git")
                    url.set("https://github.com/FeKom/bond")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("ossrhUsername") as String? ?: ""
                password = project.findProperty("ossrhPassword") as String? ?: ""
            }
        }
    }
}
```

### Configurar assinatura GPG
```kotlin
signing {
    sign(publishing.publications["mavenJava"])
}
```

### Credenciais (NÃO comitar)

No `~/.gradle/gradle.properties` (local, fora do projeto):
```properties
ossrhUsername=seu-usuario-sonatype
ossrhPassword=sua-senha-sonatype
signing.keyId=ABCD1234
signing.password=sua-senha-gpg
signing.secretKeyRingFile=/caminho/para/secring.gpg
```

---

## Passo 4: Publicar

### Publicar manualmente
```bash
# Build + assinar + upload para staging
./gradlew publish

# Depois, acesse https://s01.oss.sonatype.org
# Faça login → Staging Repositories → selecione → Close → Release
```

### Automatizar com CI (GitHub Actions)

Crie `.github/workflows/publish.yml`:
```yaml
name: Publish to Maven Central
on:
  release:
    types: [published]

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21

      - name: Import GPG key
        run: echo "${{ secrets.GPG_PRIVATE_KEY }}" | base64 -d | gpg --import

      - name: Publish
        run: ./gradlew publish
        env:
          ORG_GRADLE_PROJECT_ossrhUsername: ${{ secrets.OSSRH_USERNAME }}
          ORG_GRADLE_PROJECT_ossrhPassword: ${{ secrets.OSSRH_PASSWORD }}
          ORG_GRADLE_PROJECT_signingKeyId: ${{ secrets.GPG_KEY_ID }}
          ORG_GRADLE_PROJECT_signingPassword: ${{ secrets.GPG_PASSPHRASE }}
```

Secrets necessários no GitHub:
- `OSSRH_USERNAME`
- `OSSRH_PASSWORD`
- `GPG_PRIVATE_KEY` (base64)
- `GPG_KEY_ID`
- `GPG_PASSPHRASE`

---

## Passo 5: Validação do Maven Central

O staging repository passa por validação automática:

| Requisito | Status no Bond |
|-----------|---------------|
| POM com name, description, url | Falta configurar |
| POM com license | OK (MIT) |
| POM com developer info | Falta configurar |
| POM com SCM info | Falta configurar |
| Sources JAR (-sources.jar) | Falta `withSourcesJar()` |
| Javadoc JAR (-javadoc.jar) | Falta `withJavadocJar()` |
| Assinatura GPG (.asc) | Falta plugin `signing` |
| groupId verificado | Trocar de `github.fekom` |

---

## O que falta no Bond para publicar

### Obrigatório
- [ ] Trocar groupId para `io.github.fekom` (ou domínio próprio)
- [ ] Adicionar plugin `signing` no build.gradle.kts
- [ ] Adicionar `java.withSourcesJar()` e `java.withJavadocJar()`
- [ ] Completar POM metadata (url, scm, developers, license)
- [ ] Criar conta Sonatype e verificar namespace
- [ ] Gerar e publicar chave GPG
- [ ] Configurar credenciais no gradle.properties local

### Recomendado
- [ ] GitHub Actions para publicação automática em releases
- [ ] Versionamento semântico (0.1.0 para primeira release)
- [ ] README atualizado com exemplos de uso como starter
- [ ] JavaDoc nas classes públicas (BucketStore, RateLimiterService, BondProperties, Capacity)

### Alternativa: GitHub Packages
Se não quiser passar pelo processo do Maven Central, pode publicar no GitHub Packages:
- Mais simples (usa token do GitHub)
- Mas consumidores precisam adicionar o repositório manualmente no build
- Bom para uso interno ou beta testing

```kotlin
repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/FeKom/bond")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}
```

### Alternativa: JitPack
- Zero configuração: qualquer repositório GitHub pode ser usado como dependência
- Consumidor adiciona `maven { url 'https://jitpack.io' }` e usa `com.github.FeKom:bond:TAG`
- Sem necessidade de GPG, Sonatype, ou configuração extra
- Limitação: menos controle sobre o build, não aparece no Maven Central search

---

## Resumo do fluxo completo

```
1. Conta Sonatype + verificar namespace
2. Chave GPG + publicar em keyserver
3. Configurar build.gradle.kts (signing, POM, sources, javadoc)
4. Credenciais locais (gradle.properties)
5. ./gradlew publish
6. Close + Release no Sonatype staging
7. ~30min depois aparece no Maven Central
8. (Opcional) Automatizar com GitHub Actions
```

Após a primeira publicação bem-sucedida, as próximas são automáticas se configurar CI.
