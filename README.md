# Automated CI/CD Pipeline for a Java Web Application on AWS

### **Project Overview**
This document outlines the setup and execution of a **CI/CD pipeline** for deploying a **Java web application** (packaged as a `.war` file) using **Jenkins**, **Maven**, **SonarQube**, **Nexus**, **Ansible**, and **Tomcat** across **three AWS EC2 instances**.  

While the **application architecture** is **1-tier (monolithic)**, the **infrastructure** leverages a **3-server CI/CD setup** to ensure build automation, code quality validation, artifact management, and seamless deployment.

---

### **Architecture Diagram**

```
+----------------------+        +-----------------------+        +-----------------------+
|   Jenkins Server     |        |   Quality Server      |        |   Deploy Server       |
| (EC2 #1)             |        | (EC2 #2)              |        | (EC2 #3)              |
|----------------------|        |-----------------------|        |-----------------------|
| - Jenkins            |        | - SonarQube           |        | - Apache Tomcat       |
| - Maven              |        | - Nexus Repository    |        |                       |
| - Ansible            |        |                       |        |                       |
|----------------------|        |-----------------------|        |-----------------------|
| 1. Clone GitHub Repo | -----> | 2. Run Sonar Analysis | -----> | 3. Deploy via Ansible |
| 4. Build using Maven | -----> | 5. Push to Nexus Repo | -----> | 4. Host WAR on Tomcat |
+----------------------+        +-----------------------+        +-----------------------+
```

---

### **Step 1: AWS EC2 Setup**

**Total EC2 Instances Used:** 3  

| Server Role | Components | Ports to Open |
|--------------|-------------|---------------|
| **Jenkins Server** | Jenkins, Maven, Ansible | 22 (SSH), 8080 (Jenkins) |
| **Quality Server** | SonarQube, Nexus | 9000 (SonarQube), 8081 (Nexus) |
| **Deploy Server** | Apache Tomcat | 22 (SSH), 8080 (Tomcat) |

#### **Common Setup Steps:**
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install git wget unzip -y
```

---

### **Step 2: Jenkins Server Configuration**

1. **Install Java & Jenkins**
   ```bash
   sudo apt install openjdk-21-jdk -y
   java -version
   curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee /usr/share/keyrings/jenkins-keyring.asc > /dev/null
   echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/ | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null
   sudo apt update && sudo apt install jenkins -y
   sudo systemctl enable jenkins && sudo systemctl start jenkins
   ```

2. **Install Maven**
   ```bash
   sudo apt install maven -y
   mvn -version
   ```

3. **Install Ansible**
   ```bash
   sudo apt install ansible -y
   ansible --version
   ```

4. **Integrate Jenkins with Ansible**
   - Add SSH key in `/var/lib/jenkins/.ssh/id_rsa` for passwordless access to the deploy server.
   - Test connectivity:
     ```bash
     ansible all -m ping
     ```

---

### **Step 3: Quality Server Setup**

#### **1. Install SonarQube**
```bash
sudo apt install openjdk-21-jdk -y
wget https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-10.5.0.zip
unzip sonarqube-10.5.0.zip
sudo mv sonarqube-10.5.0 /opt/sonarqube
sudo /opt/sonarqube/bin/linux-x86-64/sonar.sh start
```
- Access at: `http://<Quality-Server-IP>:9000`

#### **2. Install Nexus Repository**
```bash
wget https://download.sonatype.com/nexus/3/latest-unix.tar.gz
tar -xvzf latest-unix.tar.gz
sudo mv nexus-*/ /opt/nexus
sudo /opt/nexus/bin/nexus start
```
- Access at: `http://<Quality-Server-IP>:8081`

---

### **Step 4: Deploy Server (Tomcat) Setup**

```bash
sudo apt install openjdk-21-jdk -y
wget https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.25/bin/apache-tomcat-10.1.25.tar.gz
tar -xvzf apache-tomcat-10.1.25.tar.gz
sudo mv apache-tomcat-10.1.25 /opt/tomcat
sudo sh /opt/tomcat/bin/startup.sh
```

- Access at: `http://<Deploy-Server-IP>:8080`

---

### **Step 5: Maven & Project Configuration**

**Project Structure Highlights:**
- **`pom.xml`** defines the build configuration, dependencies, and Nexus distribution.
- Packaging type: `war`
- Artifact name: `sample-java-webapp.war`

**Example Build Command:**
```bash
mvn clean package
```

---

### **Step 6: Jenkins CI/CD Pipeline**

#### **1. Pipeline Flow**
1. Pull source code from **GitHub**.  
2. Run **SonarQube** code quality analysis.  
3. Build `.war` file using **Maven**.  
4. Upload artifact to **Nexus repository**.  
5. Trigger **Ansible playbook** for deployment to Tomcat.

#### **2. Jenkins Job Configuration**
- **Type:** Freestyle or Pipeline  
- **SCM:** Git (link your repository)
- **Build Steps:**
  - Execute Shell or Jenkinsfile with stages:
    ```bash
    mvn clean package
    ansible-playbook deploy.yml
    ```
- **Post-build actions:**
  - Publish results to SonarQube and Nexus.

---

### **Step 7: Ansible Deployment Playbook**

**deploy.yml (Example):**
```yaml
---
- name: Deploy WAR file to Tomcat
  hosts: deploy_server
  become: yes
  tasks:
    - name: Stop Tomcat
      shell: sh /opt/tomcat/bin/shutdown.sh || true

    - name: Copy WAR file from Nexus
      get_url:
        url: "http://<Quality-Server-IP>:8081/repository/maven-releases/com/demo/sample-java-webapp/1.0.0/sample-java-webapp-1.0.0.war"
        dest: "/opt/tomcat/webapps/sample-java-webapp.war"

    - name: Start Tomcat
      shell: sh /opt/tomcat/bin/startup.sh
```

---

### **Step 8: Verify Deployment**

- Access application:  
  `http://<Deploy-Server-IP>:8080/sample-java-webapp`

- Check Tomcat logs:
  ```bash
  tail -f /opt/tomcat/logs/catalina.out
  ```

---

### **Workflow Diagram**

```
        +-----------+       +--------------+       +---------------+
        | Developer | --->  |   Jenkins    | --->  |  Ansible/Tomcat|
        |  (GitHub) |       |  + Maven     |       |  (Deployment)  |
        +-----------+       |  + Sonar/Nexus|      +---------------+
                             \______________________/
                                   Automated CI/CD
```

---

### **Conclusion**

You now have a **fully automated CI/CD pipeline** for a **1-tier Java web application**.  
This setup ensures:
- Continuous Integration via Jenkins & Maven  
- Code Quality Enforcement via SonarQube  
- Artifact Versioning via Nexus  
- Automated Deployment via Ansible to Tomcat  

**CI/CD Infrastructure:** 3 EC2 servers  
**Application Architecture:** 1-tier monolithic web app  

---


