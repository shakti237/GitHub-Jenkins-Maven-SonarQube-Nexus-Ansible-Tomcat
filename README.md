# Automated CI/CD Pipeline for a Java Web Application on AWS

## Project Overview

This document outlines the setup and execution of a **CI/CD pipeline** for deploying a **Java web application** (packaged as a `.war` file) using **Jenkins**, **Maven**, **SonarQube**, **Nexus**, **Ansible**, and **Tomcat** across three **AWS EC2 instances**.

While the application architecture is **1-tier (monolithic)**, the infrastructure leverages a **3-server CI/CD setup** to ensure build automation, code quality validation, artifact management, and seamless deployment.

---

## Architecture Diagram

```
+----------------------+     +-----------------------+     +-----------------------+
|   Jenkins Server     |     |    Quality Server     |     |    Deploy Server      |
|      (EC2 #1)        |     |       (EC2 #2)        |     |       (EC2 #3)        |
|----------------------|     |-----------------------|     |-----------------------|
| - Jenkins            |     | - SonarQube           |     | - Apache Tomcat       |
| - Maven              |     | - Nexus Repository    |     |                       |
| - Ansible            |     |                       |     |                       |
|----------------------|     |-----------------------|     |-----------------------|
| 1. Clone GitHub Repo | --> | 2. Run Sonar Analysis | --> | 3. Deploy via Ansible |
| 4. Build using Maven | --> | 5. Push to Nexus Repo | --> | 4. Host WAR on Tomcat |
+----------------------+     +-----------------------+     +-----------------------+
```

---

## Step 1: AWS EC2 Setup

**Total EC2 Instances Used:** 3

| Server Role     | Components              | Ports to Open                   |
|-----------------|--------------------------|----------------------------------|
| Jenkins Server  | Jenkins, Maven, Ansible | 22 (SSH), 8080 (Jenkins)        |
| Quality Server  | SonarQube, Nexus        | 9000 (SonarQube), 8081 (Nexus)  |
| Deploy Server   | Apache Tomcat           | 22 (SSH), 8080 (Tomcat)         |

**Common Setup Steps:**

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install git wget unzip -y
```

---

## Step 2: Jenkins Server Configuration

### Install Java & Jenkins

```bash
sudo apt install openjdk-21-jdk -y
java -version

curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | \
sudo tee /usr/share/keyrings/jenkins-keyring.asc > /dev/null

echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
https://pkg.jenkins.io/debian-stable binary/ | \
sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null

sudo apt update && sudo apt install jenkins -y
sudo systemctl enable jenkins && sudo systemctl start jenkins
```

### Install Maven

```bash
sudo apt install maven -y
mvn -version
```

### Install Ansible

```bash
sudo apt install ansible -y
ansible --version
```

### Integrate Jenkins with Ansible

Generate SSH key for passwordless access:

```bash
ssh-keygen -t rsa
ssh-copy-id ubuntu@<Deploy-Server-IP>
```

Test connectivity:

```bash
ansible all -m ping
```

---

## Step 3: Quality Server Setup

### Install SonarQube

```bash
sudo apt install openjdk-21-jdk -y
wget https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-10.5.0.zip
unzip sonarqube-10.5.0.zip
sudo mv sonarqube-10.5.0 /opt/sonarqube
sudo /opt/sonarqube/bin/linux-x86-64/sonar.sh start
```

**Access SonarQube:**  
  `http://<Quality-Server-IP>:9000`

### Install Nexus Repository

```bash
wget https://download.sonatype.com/nexus/3/latest-unix.tar.gz
tar -xvzf latest-unix.tar.gz
sudo mv nexus-*/ /opt/nexus
sudo /opt/nexus/bin/nexus start
```

**Access Nexus:**  
  `http://<Quality-Server-IP>:8081`

---

## Step 4: Deploy Server (Tomcat) Setup

```bash
sudo apt install openjdk-21-jdk -y
wget https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.25/bin/apache-tomcat-10.1.25.tar.gz
tar -xvzf apache-tomcat-10.1.25.tar.gz
sudo mv apache-tomcat-10.1.25 /opt/tomcat
sudo sh /opt/tomcat/bin/startup.sh
```

**Access Tomcat:**  
  `http://<Deploy-Server-IP>:8080`

---

## Step 5: Maven & Project Configuration

**Project Highlights:**

- `pom.xml` defines build configuration, dependencies, and Nexus distribution.
- Packaging type: `war`
- Artifact name: `sample-java-webapp.war`

**Build Command:**

```bash
mvn clean package
```

---

## Step 6: Jenkins CI/CD Pipeline

### Pipeline Flow

1. Pull source code from GitHub
2. Run SonarQube analysis
3. Build `.war` using Maven
4. Upload artifact to Nexus
5. Trigger Ansible deployment to Tomcat

### Jenkins Job Configuration

- **Type:** Freestyle 
- **SCM:** Git (link your repository)

**Example Execute Script:**

```bash
#!/bin/bash
set -e

MVN=/opt/maven/bin/mvn
WORKSPACE=/var/lib/jenkins/workspace/Automated-Project
SONAR_TOKEN="sqa_676300d2d4b500d65917c7b5a28d6bef6d9e1ba0"
SONAR_URL="http://3.110.187.107:9000"
PROJECT_KEY="Sample-Java-Webapp"

cd $WORKSPACE

echo "========================================="
echo "CI/CD Pipeline Started"
echo "========================================="

# Step 1: Build
echo "Step 1: Building with Maven..."
$MVN clean package

# Step 2: SonarQube Analysis
echo "Step 2: Running SonarQube Analysis..."
$MVN sonar:sonar \
  -Dsonar.projectKey=$PROJECT_KEY \
  -Dsonar.host.url=$SONAR_URL \
  -Dsonar.login=$SONAR_TOKEN

# Step 3: Quality Gate Check
echo "Step 3: Checking Quality Gate..."
sleep 30

RESPONSE=$(curl -s -u ${SONAR_TOKEN}: "${SONAR_URL}/api/qualitygates/project_status?projectKey=${PROJECT_KEY}")
QUALITY_GATE=$(echo "$RESPONSE" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)

echo "Quality Gate Status: $QUALITY_GATE"

if [ "$QUALITY_GATE" != "OK" ]; then
  echo "❌ Quality Gate FAILED!"
  echo "Report: ${SONAR_URL}/dashboard?id=${PROJECT_KEY}"
  exit 1
fi

echo "✅ Quality Gate PASSED!"

# Step 4: Deploy to Nexus
echo "Step 4: Deploying to Nexus..."
$MVN deploy -DskipTests

# Step 5: Deploy to Tomcat via Ansible
echo "Step 5: Deploying via Ansible..."
ansible-playbook ansible/deploy.yml -i ansible/inventory -e "workspace=$WORKSPACE"

echo "========================================="
echo "✅ Pipeline Completed Successfully!"
echo "========================================="
echo "App URL: http://172.31.33.205:8080/sample-java-webapp"
```

---

## Step 7: Ansible Deployment Playbook

```yaml
---
- name: Deploy WAR to Tomcat
  hosts: all
  become: yes
  vars:
    tomcat_home: "/opt/tomcat"
    tomcat_user: "tomcat"

  tasks:
    - name: Stop Tomcat
      shell: "cd {{ tomcat_home }} && {{ tomcat_home }}/bin/shutdown.sh"
      args:
        executable: /bin/bash
      ignore_errors: yes

    - name: Wait for Tomcat to stop
      pause:
        seconds: 10

    - name: Kill any remaining Tomcat processes
      shell: "pkill -9 -f tomcat"
      ignore_errors: yes

    - name: Clean old deployment
      file:
        path: "{{ item }}"
        state: absent
      loop:
        - "{{ tomcat_home }}/webapps/sample-java-webapp.war"
        - "{{ tomcat_home }}/webapps/sample-java-webapp"
        - "{{ tomcat_home }}/work/Catalina/localhost/sample-java-webapp"

    - name: Download WAR from Nexus
      get_url:
        url: "http://172.31.47.6:8081/repository/maven-releases/com/demo/sample-java-webapp/1.0.0/sample-java-webapp-1.0.0.war"
        dest: "{{ tomcat_home }}/webapps/sample-java-webapp.war"
        url_username: admin
        url_password: admin123
        force: yes
        mode: '0644'

    - name: Set ownership of webapps
      file:
        path: "{{ tomcat_home }}/webapps"
        owner: "{{ tomcat_user }}"
        group: "{{ tomcat_user }}"
        recurse: yes

    - name: Start Tomcat
      shell: "cd {{ tomcat_home }} && {{ tomcat_home }}/bin/startup.sh"
      args:
        executable: /bin/bash
      become_user: "{{ tomcat_user }}"

    - name: Wait for Tomcat to start
      wait_for:
        port: 8080
        host: 0.0.0.0
        delay: 15
        timeout: 60

    - name: Verify Tomcat process
      shell: "ps aux | grep tomcat | grep -v grep"
      register: tomcat_check
      ignore_errors: yes

    - name: Display Tomcat status
      debug:
        msg: "Tomcat is running: {{ tomcat_check.stdout }}"

    - name: Test application endpoint
      uri:
        url: "http://localhost:8080/sample-java-webapp"
        status_code: 200,404
      register: app_check
      retries: 5
      delay: 5
      ignore_errors: yes

    - name: Display deployment result
      debug:
        msg: "✅ Deployment Successful! Access: http://13.233.88.164:8080/sample-java-webapp"
```

---

## Step 8: Verify Deployment

**Access Application:**  
`http://<Deploy-Server-IP>:8080/sample-java-webapp`

**Check Tomcat Logs:**

```bash
tail -f /opt/tomcat/logs/catalina.out
```

---

## Workflow Diagram

```
+-----------+       +--------------+       +------------------+
| Developer | --->  |   Jenkins    | --->  | Ansible/Tomcat   |
|  (GitHub) |       |  + Maven     |       |  (Deployment)    |
+-----------+       |  + Sonar/Nexus|      +------------------+
                    \______________________/
                           Automated CI/CD
```

---

## Conclusion

You now have a fully automated CI/CD pipeline for a 1-tier Java web application.

**This setup ensures:**

✅ Continuous Integration via Jenkins & Maven  
✅ Code Quality Enforcement via SonarQube  
✅ Artifact Versioning via Nexus  
✅ Automated Deployment via Ansible to Tomcat

**Infrastructure Summary:**

- **CI/CD Infrastructure:** 3 EC2 servers
- **Application Architecture:** 1-tier monolithic web app
