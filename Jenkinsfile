pipeline {
    agent any

    environment {
        KUBERNETES_USER_CRED = "echelon133-credentials"
        KUBERNETES_SERVER_URL = "https://192.168.49.2:8443"
        KUBERNETES_APP_NAMESPACE = "sports-live-app"
    }

    stages {

        stage("Build and test project's submodules") {
            steps {
                withMaven {
                    sh "mvn clean verify"
                }
            }
        }

        stage("Login to dockerhub") {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'USER', passwordVariable: 'PASSWORD')]) {
                    sh 'echo $PASSWORD | docker login -u $USER --password-stdin'
                }
            }
        }

        stage("Build docker image of gateway-service and push it to dockerhub") {
            steps {
                script {
                    withMaven {
                        env.GATEWAY_SERVICE_VERSION = sh(returnStdout: true, script: 'mvn -f gateway-service/pom.xml help:evaluate -Dexpression=project.version -q -DforceStdout').trim()
                    }
                }
                sh "docker build --tag=echelon133/gateway-service:$GATEWAY_SERVICE_VERSION ./gateway-service"
                sh "docker push echelon133/gateway-service:$GATEWAY_SERVICE_VERSION"
            }
        }

        stage("Build docker image of match-service and push it to dockerhub") {
            steps {
                script {
                    withMaven {
                        env.MATCH_SERVICE_VERSION = sh(returnStdout: true, script: 'mvn -f match-service/pom.xml help:evaluate -Dexpression=project.version -q -DforceStdout').trim()
                    }
                }
                sh "docker build --tag=echelon133/match-service:$MATCH_SERVICE_VERSION ./match-service"
                sh "docker push echelon133/match-service:$MATCH_SERVICE_VERSION"
            }
        }

        stage("Configure namespaces and permissions of the cluster") {
            steps {
                withKubeConfig([credentialsId: "${KUBERNETES_USER_CRED}", serverUrl: "${KUBERNETES_SERVER_URL}"]) {
                    sh 'kubectl apply -f k8s/namespace.yml'
                    sh 'kubectl apply -f k8s/default-permissions.yml'
                    sh 'kubectl apply -f k8s/admin-permissions.yml'
                }
            }
        }

        stage("Create/Update resources in the cluster") {
            steps {
                withKubeConfig([credentialsId: "${KUBERNETES_USER_CRED}", serverUrl: "${KUBERNETES_SERVER_URL}"]) {
                    sh(returnStatus: true, script: 'kubectl apply -f k8s/gateway-service/')
                    sh(returnStatus: true, script: 'kubectl apply -f k8s/match-service/')
                }
            }
        }
    }
}