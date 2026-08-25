# SonarQube + Jenkins teaching setup

This project uses the Maven scanner in its CI pipeline. The scanner compiles and tests the Java application, uploads the analysis to SonarQube, and Jenkins then waits for the project's Quality Gate. Follow this guide once when setting up a new Jenkins server; afterward, a normal pipeline run performs the scan automatically.

## Prerequisites

- SonarQube is running. In this environment it is available at `http://localhost:9000`.
- Jenkins can reach SonarQube and has the **Docker Pipeline** plugin already used by this repository's `Jenkinsfile`.
- The Jenkins agent can pull the `maven:3.9.11-eclipse-temurin-25` image and reach Maven Central.

## 1. Install Jenkins plugins

1. Open Jenkins.
2. Go to **Manage Jenkins → Plugins** (or **Manage Plugins** on older Jenkins installations).
3. In the **Available plugins** tab, search for and install:
   - **SonarQube Scanner for Jenkins** — connects Jenkins to SonarQube and provides `withSonarQubeEnv` and `waitForQualityGate`.
   - **Docker Pipeline** — required because this repository runs pipeline stages in Docker containers.
4. Restart Jenkins if prompted.

## 2. Generate a SonarQube token

1. Open `http://localhost:9000` and sign in to SonarQube.
2. Click the user icon in the upper-right corner, then open **My Account → Security**.
3. Under **Generate a new token**, enter a name such as `jenkins-devopsclass` and select **Generate**.
4. Copy the token immediately and keep it private. SonarQube displays the token value only once.

For automatic creation of the project on the first Jenkins run, the SonarQube user who generated the token needs the **Create Projects** permission. If that permission is not appropriate, create the project manually with key `techaxis:devopsclass` instead.

## 3. Store the token securely in Jenkins

Never add the token to `Jenkinsfile`, `pom.xml`, Git, or a shell script.

1. In Jenkins, go to **Manage Jenkins → Credentials**.
2. Open **System → Global credentials (unrestricted) → Add Credentials**.
3. Enter these values:
   - **Kind:** `Secret text`
   - **Secret:** paste the SonarQube token generated above
   - **ID:** `sonarqube-token`
   - **Description:** `SonarQube token for DevOps Class Java CI`
4. Select **Create**.

## 4. Configure the SonarQube server in Jenkins

1. Go to **Manage Jenkins → System**.
2. Find the **SonarQube servers** section and select **Add SonarQube**.
3. Configure it as follows:
   - **Name:** `SonarQube` (this must exactly match `withSonarQubeEnv('SonarQube')` in the Jenkinsfile)
   - **Server URL:**
     - Jenkins installed directly on this computer: `http://localhost:9000`
     - Jenkins running inside Docker on macOS/OrbStack: `http://host.docker.internal:9000`
     - Jenkins attached to Docker network `sonarqube-production_sonarnet`: `http://sonarqube:9000`
   - **Server authentication token:** select the `sonarqube-token` credential.
4. Select **Save**.

Jenkins injects this token only during the SonarQube stage as `SONAR_AUTH_TOKEN`; the repository does not contain the secret.

## 5. Configure the Quality Gate webhook

The webhook tells Jenkins that SonarQube has finished calculating the Quality Gate. Without it, the pipeline pauses at the Quality Gate stage and eventually times out.

1. In SonarQube, go to **Administration → Configuration → Webhooks → Create**.
2. Name it `Jenkins Quality Gate`.
3. Set the URL to:

   ```text
   http://<jenkins-host>:8080/sonarqube-webhook/
   ```

4. Select **Create**.

The Jenkins address must be reachable *from the SonarQube container*. Do not use `localhost` here unless Jenkins itself runs in that same container.

## 6. Select the Java Quality Profile and Quality Gate

1. In **Quality Profiles**, select or create the Java profile to teach. Set it as the default profile, or associate it with `techaxis:devopsclass` after the first scan.
2. In **Quality Gates**, select the desired gate. Set it as the default gate, or associate it with that same project after the first scan.

For a classroom demo, start with the built-in gate and adjust a copy of it rather than changing the global built-in configuration.

## 7. First pipeline run

Create or update the Jenkins pipeline job to use this repository's `Jenkinsfile`, then select **Build Now**. The `SonarQube Analysis` stage runs:

```text
mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar
```

The `techaxis:devopsclass` project appears automatically if the token can create projects. Otherwise, create it in SonarQube with that exact key and re-run the build. The next `Quality Gate` stage passes or fails the Jenkins build based on the gate result.

## Troubleshooting

- **`Not authorized` / `401`:** The Jenkins secret is missing, expired, or assigned to the wrong SonarQube server configuration. Generate a new token, replace the Jenkins secret, and run again.
- **`Unknown host` / connection refused:** Jenkins cannot reach the configured SonarQube URL. Choose the URL based on where Jenkins runs, as listed in step 4.
- **Quality Gate waits for five minutes then fails:** the SonarQube webhook cannot reach Jenkins. Check the webhook's delivery details in SonarQube and correct `<jenkins-host>`.
- **Project does not appear:** create `techaxis:devopsclass` manually, or grant the token user **Create Projects** permission.
- **Pipeline fails before SonarQube analysis:** the Jenkins agent needs Java 25/Maven and access to Maven Central. The supplied Jenkinsfile uses the JDK 25 Maven image for this reason.

## Important classroom notes

- JaCoCo is configured in `pom.xml`. Running `mvn clean verify` generates test coverage reports at `target/site/jacoco/jacoco.xml`, which the Maven Sonar plugin automatically imports into SonarQube.
- The project targets Java 25. The SonarQube stage deliberately uses a JDK 25 Maven image; a `docker:cli` image alone cannot build or scan this project.
- Keep SonarQube analysis in CI. Docker image build and deployment should happen only after the Quality Gate passes, as this pipeline now does.
