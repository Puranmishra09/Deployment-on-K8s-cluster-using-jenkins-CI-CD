node {
    
    stage('Git Checkout') {
        git 'https://github.com/Puranmishra09/Deployment-on-K8s-cluster-using-jenkins-CI-CD.git'
    }

   stage('Send Files to Ansible VM') {
    sshagent(['89be48e1-a2bc-4d8d-bd9a-2e66fb315fc6']) {
        sh '''
            echo "Current directory: $(pwd)"
            echo "Listing contents:"
            ls -R

            scp -o StrictHostKeyChecking=no \
                "Kubernetes/Deployment.yaml" \
                "Kubernetes/Service.yaml" \
                "Transfer & Execute files on remote server using SshAgent/Dockerfile/Dockerfile" \
                puranmishra2024@34.72.208.46:/home/puranmishra2024/
        '''
    }
}

    stage('Build Docker Image on Ansible VM') {
        sshagent(['89be48e1-a2bc-4d8d-bd9a-2e66fb315fc6']) {
            sh '''
                ssh -o StrictHostKeyChecking=no puranmishra2024@34.72.208.46 '
                    cd /home/puranmishra2024 &&
                    docker build -t $JOB_NAME:v1.$BUILD_ID .
                '
            '''
        }
    }

    stage('Tag Docker Image') {
        sshagent(['89be48e1-a2bc-4d8d-bd9a-2e66fb315fc6']) {
            sh '''
                ssh -o StrictHostKeyChecking=no puranmishra@2024@34.72.208.46 '
                    docker tag $JOB_NAME:v1.$BUILD_ID puranmishra/$JOB_NAME:v1.$BUILD_ID &&
                    docker tag $JOB_NAME:v1.$BUILD_ID puranmishra/$JOB_NAME:latest
                '
            '''
        }
    }

    stage('Push Image to Docker Hub') {
        sshagent(['89be48e1-a2bc-4d8d-bd9a-2e66fb315fc6']) {
            withCredentials([string(credentialsId: 'dockerhub-pass', variable: 'DOCKER_PASS')]) {
                sh '''
                    ssh -o StrictHostKeyChecking=no puranmishra2024@34.72.208.46 '
                        echo "$DOCKER_PASS" | docker login -u puranmishra --password-stdin &&
                        docker push puranmishra/$JOB_NAME:v1.$BUILD_ID &&
                        docker push puranmishra/$JOB_NAME:latest
                    '
                '''
            }
        }
    }

    stage('Copy K8s Files to GKE Node (optional)') {
        // Only needed if you manually apply k8s manifests via ssh
        sshagent(['89be48e1-a2bc-4d8d-bd9a-2e66fb315fc6']) {
            sh 'scp -o StrictHostKeyChecking=no /var/lib/jenkins/workspace/$JOB_NAME/*.yaml puranmishra2024@10.128.0.5:/home/puranmishra2024/'
        }
    }

    stage('Deploy to GKE using Ansible') {
        sshagent(['89be48e1-a2bc-4d8d-bd9a-2e66fb315fc6']) {
            sh '''
                ssh -o StrictHostKeyChecking=no puranmishra2024@34.72.208.46 '
                    gcloud container clusters get-credentials devops-gke-cluster --zone us-central1-a --project tribal-isotope-457208-q4 &&
                    kubectl set image deployment/myapp-deployment myapp-container=puranmishra/$JOB_NAME:latest &&
                    kubectl apply -f /home/puranmishra2024/k8s/deployment.yaml &&
                    kubectl apply -f /home/puranmishra2024/k8s/service.yaml
                '
            '''
        }
    }
}
