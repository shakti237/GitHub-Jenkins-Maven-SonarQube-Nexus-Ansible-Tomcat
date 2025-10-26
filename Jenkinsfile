pipeline {
    agent any
    environment {
        SONAR_URL = 'http://172.31.47.6:9000'
        NEXUS_URL = 'http://172.31.47.6:8081/repository/maven-releases/'
        TOMCAT_HOST = '172.31.33.205'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/<your-username>/sample-java-webapp.git',
                    credentialsId: 'github-creds'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.war', fingerprint: true
                }
            }
        }

        stage('SonarQube') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh "mvn sonar:sonar -Dsonar.host.url=${env.SONAR_URL} -Dsonar.login=${SONAR_TOKEN}"
                }
            }
        }

        stage('Deploy to Nexus') {
            steps {
                sh 'mvn deploy -DskipTests=true'
            }
        }

        stage('Deploy via Ansible') {
            steps {
                sh 'ansible-playbook -i ansible/inventory ansible/deploy.yml --extra-vars "build_number=${BUILD_NUMBER}"'
            }
        }
    }

    post {
        always {
            junit 'target/surefire-reports/*.xml'
        }
    }
}
