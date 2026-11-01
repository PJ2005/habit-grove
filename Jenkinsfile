pipeline {
    agent any

    environment {
        IMAGE = "habit-grove:${env.BUILD_NUMBER}"
    }

    options {
        timeout(time: 20, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    stages {
        stage('Build & Test') {
            steps {
                // Maven runs inside the Docker build: tests run here, and a failure stops the pipeline
                sh 'docker build -t $IMAGE .'
            }
        }

        stage('Load image into minikube') {
            steps {
                sh 'minikube image load $IMAGE'
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh '''
                    sed "s|IMAGE_PLACEHOLDER|$IMAGE|" k8s/deployment.yaml | kubectl apply -f -
                    kubectl apply -f k8s/service.yaml
                    kubectl rollout status deployment/habit-grove --timeout=180s
                '''
            }
        }

        stage('Smoke test') {
            steps {
                sh 'curl -fsS "http://$(minikube ip):30080/actuator/health"'
            }
        }
    }

    post {
        always {
            sh 'docker image prune -f || true'
        }
        failure {
            sh 'kubectl get pods -l app=habit-grove || true'
        }
    }
}
