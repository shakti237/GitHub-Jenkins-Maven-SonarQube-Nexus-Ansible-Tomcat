# Sample Java CI/CD Project

This sample project demonstrates a complete CI/CD pipeline integrating:
GitHub → Jenkins → Maven → SonarQube → Nexus → Docker → Ansible → Tomcat.

**Environment (provided by user):**
- Java: 11
- Maven: 3.9.6
- Jenkins IP: 172.31.34.19
- SonarQube & Nexus IP: 172.31.47.6
- Tomcat/Ansible IP: 172.31.33.205

This ZIP contains:
- A simple Maven WAR webapp (`src/`) with a servlet and JSP.
- `pom.xml` configured for war packaging and Sonar.
- `Jenkinsfile` (Declarative Pipeline) to run build, Sonar, deploy to Nexus, build Docker, and run Ansible.
- `Dockerfile` (Tomcat-based) to containerize the WAR.
- `ansible/` folder with `deploy.yml` and `inventory`.
- `settings.xml.sample` for Maven (user must add Nexus credentials).
- `sonar-project.properties` sample.
- `.gitignore`

**Notes**
- Nexus/Sonarqube credentials and Jenkins credentials must be configured in your Jenkins instance (no secrets stored here).
- By default this project deploys the WAR to Tomcat via Ansible (recommended, simpler). The Dockerfile is included if you prefer containerized Tomcat.
- After extracting, push to GitHub and configure Jenkins to pull from your repo.

