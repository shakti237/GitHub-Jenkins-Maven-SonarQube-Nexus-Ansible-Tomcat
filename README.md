# 🚀 Sample Java CI/CD Project

This project demonstrates a complete CI/CD pipeline using:

**GitHub → Jenkins → Maven → SonarQube → Nexus → Ansible → Tomcat**

<img width="638" height="522" alt="CICD" src="https://github.com/user-attachments/assets/9ce25304-e84b-4d6a-a4f3-1a7146329d90" />










<img width="426" height="630" alt="CICD_2" src="https://github.com/user-attachments/assets/c3737d56-9448-4c82-b734-4b793654bfbb" />






---

## 🖥️ Infrastructure (3 AWS Linux Servers)

- **Jenkins Server**: Jenkins + Maven + Ansible  
- **Quality Server**: SonarQube + Nexus  
- **Deploy Server**: Apache Tomcat  

---

## 📦 Project Contents

- `src/` – Simple WAR webapp (Servlet + JSP)  
- `pom.xml` – WAR packaging + Sonar config  
- `Jenkinsfile` – Build, test, Sonar scan, deploy to Nexus, Docker build, Ansible deploy  
- `Dockerfile` – Tomcat-based container (optional)  
- `ansible/` – `deploy.yml` + inventory  
- `settings.xml.sample` – Add your Nexus credentials  
- `sonar-project.properties` – Sample Sonar config  
- `.gitignore` – Clean repo setup  

---

## 🔐 Notes

- Nexus, SonarQube, and Jenkins credentials must be configured in Jenkins (not stored in this repo)  
- Default deployment uses **Ansible to Tomcat** (recommended)  
- Dockerfile is included for optional containerized deployment  
- After setup, push to GitHub and configure Jenkins to pull and build  


