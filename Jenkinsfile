import groovy.json.JsonSlurper
import java.security.*

def parseJson(jsonString) {
    def lazyMap = new JsonSlurper().parseText(jsonString)
    def m = [:]
    m.putAll(lazyMap)
    return m
}

def parseJsonArray(jsonString){
    def datas = readJSON text: jsonString
    return datas
}

def parseJsonString(jsonString, key){
    def datas = readJSON text: jsonString
    String Values = writeJSON returnText: true, json: datas[key]
    return Values
}

def parseYaml(jsonString) {
    def datas = readYaml text: jsonString
    String yml = writeYaml returnText: true, data: datas['kubernetes']
    return yml

}

def createYamlFile(data,filename) {
    writeFile file: filename, text: data
}

def returnSecret(path,secretValues){
    def secretValueFinal= []
    for(secret in secretValues) {
        def secretValue = [:]
        //secretValue['envVar'] = secret.envVariable
        //secretValue['vaultKey'] = secret.vaultKey
        secretValue.put('envVar',secret.envVariable)
        secretValue.put('vaultKey',secret.vaultKey)
        secretValueFinal.add(secretValue)
    }
    def secrets = [:]
    secrets["path"] = path
    secrets['engineVersion']=  2
    secrets['secretValues'] = secretValueFinal

    return secrets
}

// String str = ''
// loop to create a string as -e $STR -e $PTDF

def dockerVaultArguments(secretValues){
    def data = []
    for(secret in secretValues) {
        data.add('$'+secret.envVariable+' > .'+secret.envVariable)
    }
    return data
}

def dockerVaultArgumentsFile(secretValues){
    def data = []
    for(secret in secretValues) {
        data.add(secret.envVariable)
    }
    return data
}

def pushToCollector(){
  print("Inside pushToCollector...........")
    def job_name = "$env.JOB_NAME"
    def job_base_name = "$env.JOB_BASE_NAME"
    String metaDataProperties = parseJsonString(env.JENKINS_METADATA,'general')
    metadataVars = parseJsonArray(metaDataProperties)
    if(metadataVars.tenant != '' &&
    metadataVars.lazsaDomainUri != ''){
      echo "Job folder - $job_name"
      echo "Pipeline Name - $job_base_name"
      echo "Build Number - $currentBuild.number"
      sh """curl -k -X POST '${metadataVars.lazsaDomainUri}/collector/orchestrator/devops/details' -H 'X-TenantID: ${metadataVars.tenant}' -H 'Content-Type: application/json' -d '{\"jobName\" : \"${job_base_name}\", \"projectPath\" : \"${job_name}\", \"agentId\" : \"${metadataVars.agentId}\", \"devopsConfigId\" : \"${metadataVars.devopsSettingId}\", \"agentApiKey\" : \"${metadataVars.agentApiKey}\", \"buildNumber\" : \"${currentBuild.number}\" }' """
    }
}

def returnVaultConfig(vaultURL,vaultCredID){
    echo vaultURL
    echo vaultCredID
    def configurationVault = [:]
    //configurationVault["vaultUrl"] = vaultURL
    configurationVault["vaultCredentialId"] = vaultCredID
    configurationVault["engineVersion"] = 2
    return configurationVault
}

def waitforsometime() {
    sh 'sleep 5'
}

def checkoutRepository() {
    sh '''sudo chown -R `id -u`:`id -g` "$WORKSPACE" '''
    checkout([
            $class: 'GitSCM',
            branches: [[name: env.TESTCASEREPOSITORYBRANCH]],
            doGenerateSubmoduleConfigurations: false,
            extensions: [[$class: 'CleanCheckout']],
            submoduleCfg: [],
            userRemoteConfigs: [[credentialsId: env.SOURCECODECREDENTIALID,url: env.TESTCASEREPOSITORYURL]]
    ])
}

def getApplicationUrl(){
    def host
    if (env.DEPLOYMENT_TYPE == 'KUBERNETES'){
        kubernetesJsonString = parseJsonString(env.JENKINS_METADATA,'kubernetes')
        ingressJsonString = parseJsonString(kubernetesJsonString,'ingress')
        ingress = parseJsonArray(ingressJsonString)
        if(ingress['hosts']){
            hostname = ingress.hosts[0]
            print("hostname:"+hostname)
            host = hostname
        }
        else{
            host = metadataVars.ingressAddress
        }
        print("host in kubernetes:"+host)
    }
    else if(env.DEPLOYMENT_TYPE == 'EC2') {
        dockerProperties = parseJsonString(env.JENKINS_METADATA, 'docker')
        dockerData = parseJsonArray(dockerProperties)
        host = DOCKERHOST+':'+dockerData.hostPort
        print("host in ec2:"+host)
    }

    print("SITE_URL under test: http://${host}$CONTEXT/")
    return host;
}

def runCypressTest() {
    def host = getApplicationUrl()

    sh """ 
  cat <<EOL > test.sh
  #!/bin/bash
  
  rm -rf node_modules/ mochawesome-report/ cypress/videos/ /cypress/screenshots/ 
  apt-get update
  apt-get install -y libgbm-dev
  npm install --save-dev mochawesome
  npm install mochawesome-merge --save-dev
  npm install
  case "$env.TESTCASECOMMAND" in 
    *env*)
        # Do stuff
        echo 'case env'
        $env.TESTCASECOMMAND applicationUrl=http://${host}$CONTEXT/ --browser $env.BROWSERTYPE --reporter mochawesome --reporter-options overwrite=false,html=false,json=true,charts=true
        ;;
    *)  
        echo 'case else'
        $env.TESTCASECOMMAND -- --env applicationUrl=http://${host}$CONTEXT/ --browser $env.BROWSERTYPE --reporter mochawesome --reporter-options overwrite=false,html=false,json=true,charts=true  
        ;;
  esac
  npx mochawesome-merge mochawesome-report/*.json > mochawesome-report/output.json
  npx marge mochawesome-report/output.json mochawesome-report  ./ --inline

  EOL
  """
    sh 'docker run -v "$WORKSPACE"/testcaseRepo:/app -w /app cypress/browsers:node14.19.0-chrome100-ff99-edge /bin/sh test.sh > test.out || true'
    sh 'cat test.out'
    sh """ 
 awk 'BEGIN { for(i=1;i<=5;i++) printf "*+"; } /(^[ ]*✖.+(failed|pended|pending|skipped|skipping)|^[ ]*✔[ ]+All specs passed).+/ {for(i=4;i>=0;i--) switch (i) {case 4: if( \$(NF-i) ~ /^[0-9]/ ){printf aggr "Tests run: "\$(NF-i) aggr ", ";} else{printf "Tests run: 0, " ;}  break; case 3: if( \$(NF-i) ~ /^[0-9]/ ){printf aggr "Passed: "\$(NF-i) aggr ", "} else{printf "Passed: 0, "} break; case 2: if( \$(NF-i) ~ /^[0-9]/ ){printf aggr "Failures: " \$(NF-i) aggr ", "} else{printf "Failures: 0, "}  break; case 1: if( \$(NF-i) ~ /^[0-9]/ ){printf aggr "Pending: " \$(NF-i) aggr ", "} else{printf "Pending: 0, "} break; case 0: if( \$(NF-i) ~ /^[0-9]/ ){printf aggr "Skipped: " \$(NF-i)} else{printf "Skipped: 0"} break; }} END { for(i=1;i<=5;i++) printf "*+"; }' test.out
 """
    sh 'pwd'
}

def publishResults(reportDir, reportFiles, reportName, reportTitles) {
    publishHTML([allowMissing: true,
                 alwaysLinkToLastBuild: true,
                 keepAll: true,
                 reportDir: reportDir,
                 reportFiles: reportFiles,
                 reportName: reportName,
                 reportTitles: reportTitles
    ])

    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'

}

def agentLabel = "${env.JENKINS_AGENT == null ? "":env.JENKINS_AGENT}"

pipeline {
  agent { label agentLabel }
  environment {
    DEFAULT_STAGE_SEQ = "'Initialization','Build','UnitTests','SonarQubeScan','BuildContainerImage','containerImageScanc','PublishContainerImage','Deploy','FunctionalTests','Destroy'"
    CUSTOM_STAGE_SEQ = "${DYNAMIC_JENKINS_STAGE_SEQUENCE}"
    PROJECT_TEMPLATE_ACTIVE = "${DYNAMIC_JENKINS_STAGE_NEEDED}"
    LIST = "${env.PROJECT_TEMPLATE_ACTIVE == 'true' ? env.CUSTOM_STAGE_SEQ : env.DEFAULT_STAGE_SEQ}"
    BRANCHES = "${env.GIT_BRANCH}"
    COMMIT = "${env.GIT_COMMIT}"
    RELEASE_NAME = "corejavawithgradle"
    SERVICE_PORT = "${APP_PORT}"
    DOCKERHOST = "${DOCKERHOST_IP}"
    REGISTRY_URL = "${DOCKER_REPO_URL}"
    ACTION = "${ACTION}"
    DEPLOYMENT_TYPE = "${DEPLOYMENT_TYPE == ""? "EC2":DEPLOYMENT_TYPE}"
    KUBE_SECRET = "${KUBE_SECRET}"
    CHROME_BIN = "/usr/bin/google-chrome"
    ARTIFACTORY = "${ARTIFACTORY == ""? "ECR":ARTIFACTORY}"
    ARTIFACTORY_CREDENTIALS = "${ARTIFACTORY_CREDENTIAL_ID}"
    SONAR_CREDENTIAL_ID = "${env.SONAR_CREDENTIAL_ID}"
    STAGE_FLAG = "${STAGE_FLAG}"
    JENKINS_METADATA = "${JENKINS_METADATA}"
    JAVA_MVN_IMAGE_VERSION = "maven:3.8.6-jdk-8-slim" //https://hub.docker.com/_/maven/tags
    KUBECTL_IMAGE_VERSION = "bitnami/kubectl:1.24.9" //https://hub.docker.com/r/bitnami/kubectl/tags
    HELM_IMAGE_VERSION = "alpine/helm:3.8.1" //https://hub.docker.com/r/alpine/helm/tags   
    OC_IMAGE_VERSION = "quay.io/openshift/origin-cli:4.9.0" //https://quay.io/repository/openshift/origin-cli?tab=tags
    JFROGCLI_IMAGE_VERSION = "public.ecr.aws/lazsa/gradle-jf:jdk8"

  }

  stages {
    stage('Initialization') {
      agent { label agentLabel }
      steps {
        script {
          def listValue = "$env.LIST"
          def list = listValue.split(',')
          print(list)
            // For Context Path read
            String metaDataProperties = parseJsonString(env.JENKINS_METADATA,'general')
            metadataVars = parseJsonArray(metaDataProperties)
            if(metadataVars.repoName == ''){
                metadataVars.repoName = env.RELEASE_NAME
            }
            env.CONTEXT = metadataVars.contextPath
            env.TESTCASEREPOSITORYURL = metadataVars.testcaseRepositoryUrl
            env.TESTCASEREPOSITORYBRANCH = metadataVars.testcaseRepositoryBranch
            env.SOURCECODECREDENTIALID = metadataVars.sourceCodeCredentialId
            env.TESTCASECOMMAND = metadataVars.testcaseCommand
            env.TESTINGTOOLTYPE = metadataVars.testingToolType
            env.BROWSERTYPE = metadataVars.browserType
            env.CONTAINERSCANTYPE = metadataVars.containerScanType

            env.PUBLISH_ARTIFACT = metadataVars.artifactPublish
            env.REPO_NAME = metadataVars.repoName
            env.ARTIFACTORY_URL = metadataVars.artifactoryUrl
            repoProperties = parseJsonString(env.JENKINS_METADATA,'general')
            if( metadataVars.artifactRepository){
                artifactPathMetadata = parseJsonString(repoProperties,'artifactRepository')
                artifactPathVars = parseJsonArray(artifactPathMetadata)
                env.LIBRARY_REPO = artifactPathVars.release
            }

            if (env.DEPLOYMENT_TYPE == 'KUBERNETES' || env.DEPLOYMENT_TYPE == 'OPENSHIFT') {
                String kubeProperties = parseJsonString(env.JENKINS_METADATA,'kubernetes')
                kubeVars = parseJsonArray(kubeProperties)
                if(kubeVars['vault']){
                    String kubeData = parseJsonString(kubeProperties,'vault')
                    def kubeValues = parseJsonArray(kubeData)
                    if(kubeValues.type == 'vault'){
                        String helm_file = parseYaml(env.JENKINS_METADATA)
                        echo helm_file
                        createYamlFile(helm_file,"Helm.yaml")
                    }
                }else {
                    String helm_file = parseYaml(env.JENKINS_METADATA)
                    echo helm_file
                    createYamlFile(helm_file,"Helm.yaml")
                }
            }
            def job_name = "$env.JOB_NAME"
            env.BUILD_TAG = "${BUILD_NUMBER}"
            print(job_name)
            def namespace = ''
            if (env.DEPLOYMENT_TYPE == 'KUBERNETES' || env.DEPLOYMENT_TYPE == 'OPENSHIFT'){
                if (kubeVars.namespace != null && kubeVars.namespace != '') {
                    namespace = kubeVars.namespace
                }else{
                    echo "namespace not received"
                }
            }
            print("kube namespace: $namespace")
            env.namespace_name = namespace
            if (env.STAGE_FLAG != 'null' && env.STAGE_FLAG != null) {
                stage_flag = parseJson("$env.STAGE_FLAG")
            } else {
               stage_flag = parseJson('{"qualysScan": false, "sonarScan": true, "zapScan": false, "rapid7Scan": false, "sysdig": false, "FunctionalTesting": false}')
            }
            if (!stage_flag) {
               stage_flag = parseJson('{"qualysScan": false, "sonarScan": true, "zapScan": false, "rapid7Scan": false, "sysdig": false, "FunctionalTesting": false}')
            }

            if (env.ARTIFACTORY == "ECR") {
                def url_string = "$REGISTRY_URL"
                url = url_string.split('\\.')
                env.AWS_ACCOUNT_NUMBER = url[0]
                env.ECR_REGION = url[3]
                echo "ecr region: $ECR_REGION"
                echo "ecr acc no: $AWS_ACCOUNT_NUMBER"
            } else if (env.ARTIFACTORY == "ACR") {
                def url_string = "$REGISTRY_URL"
                url = url_string.split('/')
                env.ACR_LOGIN_URL = url[0]
                echo "Reg Login url: $ACR_LOGIN_URL"
            }  else if (env.ARTIFACTORY == "JFROG") {
                def url_string = "$REGISTRY_URL"
                url = url_string.split('/')
                env.JFROG_LOGIN_URL = url[0]
                echo "Reg Login url: $JFROG_LOGIN_URL"
            }

          echo "projectTemplateActive - $env.PROJECT_TEMPLATE_ACTIVE"
          if (env.CUSTOM_STAGE_SEQ != null) {
            echo "customStagesSequence - $env.CUSTOM_STAGE_SEQ"
          }
          echo "defaultStagesSequence - $env.DEFAULT_STAGE_SEQ"
          for (int i = 0; i < list.size(); i++) {
            print(list[i])
            if ("${list[i]}" == "'UnitTests'" && env.ACTION == 'DEPLOY') {
              stage('Unit Tests') {
                print(list[i])
                // stage details here
                sh """
                docker run --rm -v "$WORKSPACE":/usr/src/mymaven -w /usr/src/mymaven $JAVA_MVN_IMAGE_VERSION ./gradlew test
                """
              }
            } else if ("${list[i]}" == "'SonarQubeScan'" && env.ACTION == 'DEPLOY' && stage_flag['sonarScan']) {
              stage('SonarQube') {
                // stage details here
                def sonar_org = "${metadataVars.sonarOrg}";
                def sonar_project_key = "${metadataVars.sonarProjectKey}";


                  if (env.SONAR_CREDENTIAL_ID != null && env.SONAR_CREDENTIAL_ID != '') {
                         withCredentials([usernamePassword(credentialsId: "$SONAR_CREDENTIAL_ID", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                            //sh """JAVA_HOME=/usr/lib/jvm/jdk-1.8 ./gradlew sonarqube -Dsonar.verbose=true -Dsonar.projectKey="${sonar_project_key}" -Dsonar.projectName="${sonar_project_key}"  -Dsonar.java.binaries=build/classes  -Dsonar.host.url="${metadataVars.sonarHost}" -Dsonar.organization="${metadataVars.sonarOrg}" -Dsonar.login=$PASSWORD --stacktrace"""
                            sh """docker run -v "$WORKSPACE":/app -w /app sonarsource/sonar-scanner-cli:4.7.0 -Dsonar.verbose=true -Dsonar.projectKey="${sonar_project_key}" -Dsonar.projectName="${sonar_project_key}" -Dsonar.organization="${metadataVars.sonarOrg}" -Dsonar.java.binaries=build/classes -Dsonar.junit.reportPaths=./build/test-results/test -Dsonar.coverage.jacoco.xmlReportPaths=./build/reports/jacoco/test/html -Dsonar.exclusions=build/reports/**.*,build/test-results/**.* -Dsonar.host.url="${metadataVars.sonarHost}" -Dsonar.login=$PASSWORD"""
                          }
                    }
                    else{
                        withSonarQubeEnv('pg-sonar') {
                            //sh """JAVA_HOME=/usr/lib/jvm/jdk-1.8 ./gradlew sonarqube -Dsonar.verbose=true -Dsonar.projectKey="${sonar_project_key}" -Dsonar.projectName="${sonar_project_key}" -Dsonar.organization="${metadataVars.sonarOrg}" -Dsonar.junit.reportPaths=./build/test-results/test -Dsonar.java.binaries=build/classes -Dsonar.coverage.jacoco.xmlReportPaths=./build/reports/tests/test/index.html --stacktrace"""
                            sh """docker run -v "$WORKSPACE":/app -w /app sonarsource/sonar-scanner-cli:4.7.0 -Dsonar.verbose=true -Dsonar.projectKey="${sonar_project_key}" -Dsonar.projectName="${sonar_project_key}" -Dsonar.organization="${metadataVars.sonarOrg}" -Dsonar.junit.reportPaths=./build/test-results/test -Dsonar.java.binaries=build/classes -Dsonar.coverage.jacoco.xmlReportPaths=./build/reports/tests/test/index.html -Dsonar.host.url="$SONAR_HOST_URL" -Dsonar.login=$SONAR_AUTH_TOKEN """
                            }
                    }
              }

            } else if ("${list[i]}" == "'containerImageScan'" && stage_flag['containerScan'] && "$PUBLISH_ARTIFACT" == "false") {
                 stage("Container Image Scan") {
                     if (env.CONTAINERSCANTYPE == 'XRAY') {
                         jf 'docker scan $REGISTRY_URL:$BUILD_TAG'
                     }
                     if(env.CONTAINERSCANTYPE == 'QUALYS'){
                          getImageVulnsFromQualys credentialsId: "${metadataVars.qualysCredentialId}", imageIds: env.REGISTRY_URL+":"+env.BUILD_TAG, pollingInterval: '30', useLocalConfig: true, apiServer: "${metadataVars.qualysServerURL}", platform: 'PCP', vulnsTimeout: '600'
                     }
                     if(env.CONTAINERSCANTYPE == 'RAPID7'){
                         assessContainerImage failOnPluginError: true,
                                     imageId: env.REGISTRY_URL+":"+env.BUILD_TAG,
                                     thresholdRules: [
                                             exploitableVulnerabilities(action: 'Mark Unstable', threshold: '1'),
                                             criticalVulnerabilities(action: 'Fail', threshold: '1')
                                     ],
                                     nameRules: [
                                             vulnerablePackageName(action: 'Fail', contains: 'nginx')
                                     ]
                     }
                     if(env.CONTAINERSCANTYPE == 'SYSDIG'){
                        sh 'echo  $REGISTRY_URL:$BUILD_TAG > sysdig_secure_images'
                             sysdig inlineScanning: true, bailOnFail: true, bailOnPluginFail: true, name: 'sysdig_secure_images'
                     }

                 }

            }
            else if ("${list[i]}" == "'Build'" && env.ACTION == 'DEPLOY') {
              stage('Build') {
                // stage details here
                echo "echoed BUILD_TAG--- $BUILD_TAG"
                sh """
                docker run --rm -v "$WORKSPACE":/usr/src/mymaven -w /usr/src/mymaven $JAVA_MVN_IMAGE_VERSION ./gradlew clean build jacocoTestReport --refresh-dependencies
                sudo chown -R `id -u`:`id -g` "$WORKSPACE" 
                """
              }
            }  else if ("${list[i]}" == "'publishArtifact'" && "$PUBLISH_ARTIFACT" == "true") {
               stage('Publish Artifact') {
                      withCredentials([usernamePassword(credentialsId: "$ARTIFACTORY_CREDENTIALS", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                     sh """
                     docker run --rm  -v "$WORKSPACE":/opt/"$REPO_NAME" -w /opt/"$REPO_NAME" "$JFROGCLI_IMAGE_VERSION" /bin/bash -c "jf c add jfrog --password "$PASSWORD" --user "$USERNAME" --url="$ARTIFACTORY_URL" --artifactory-url="$ARTIFACTORY_URL"/artifactory --interactive=false --overwrite=true ; jf gradle-config --repo-deploy "$LIBRARY_REPO" --use-wrapper; jf gradle artifactoryPublish"  """
                   }
               }
            } else if ("${list[i]}" == "'BuildContainerImage'" && env.ACTION == 'DEPLOY' && "$PUBLISH_ARTIFACT" == "false") {
               stage('Build Container Image') {   // no changes
                   // stage details here
                   echo "echoed BUILD_TAG--- $BUILD_TAG"
                   //Uncomment this block and modify as per your need if you want to use custom base image in Dockerfile from private repository
                   /*
                   withCredentials([usernamePassword(credentialsId: "$ARTIFACTORY_CREDENTIALS", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                      if (env.ARTIFACTORY == 'ECR') {
                          sh 'set +x; AWS_ACCESS_KEY_ID=$USERNAME AWS_SECRET_ACCESS_KEY=$PASSWORD aws ecr get-login-password --region "$ECR_REGION" | docker login --username AWS --password-stdin $REGISTRY_URL ;set -x'
                      }
                      if (env.ARTIFACTORY == 'JFROG') {
                          sh 'docker login -u "\"$USERNAME\"" -p "\"$PASSWORD\"" "$REGISTRY_URL"'
                      }
                      if (env.ARTIFACTORY == 'ACR') {
                          sh 'docker login -u "\"$USERNAME\"" -p "\"$PASSWORD\"" "$ACR_LOGIN_URL"'
                      }
                   } */
                   sh 'docker build -t "$REGISTRY_URL:$BUILD_TAG" -t "$REGISTRY_URL:latest" .'
               }
            }else if ("${list[i]}" == "'PublishContainerImage'" && (env.ACTION == 'DEPLOY' || env.ACTION == 'PROMOTE') && "$PUBLISH_ARTIFACT" == "false") {
                        stage('Publish Container Image') {   // no changes
                            // stage details here
                            echo "Publish Container Image"
                            withCredentials([usernamePassword(credentialsId: "$ARTIFACTORY_CREDENTIALS", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                               if (env.ARTIFACTORY == 'ECR') {
                                   sh 'set +x; AWS_ACCESS_KEY_ID=$USERNAME AWS_SECRET_ACCESS_KEY=$PASSWORD aws ecr get-login-password --region "$ECR_REGION" | docker login --username AWS --password-stdin $REGISTRY_URL ;set -x'
                               }
                               if (env.ARTIFACTORY == 'JFROG') {
                                   sh 'docker login -u "\"$USERNAME\"" -p "\"$PASSWORD\"" "$REGISTRY_URL"'
                               }
                               if (env.ARTIFACTORY == 'ACR') {
                                   sh 'docker login -u "\"$USERNAME\"" -p "\"$PASSWORD\"" "$ACR_LOGIN_URL"'
                               }
                           }
                          if (env.ACTION == 'DEPLOY') {
                              sh 'docker push "$REGISTRY_URL:$BUILD_TAG"'
                              sh 'docker push "$REGISTRY_URL:latest"'
                          }
                          if (env.ACTION == 'PROMOTE') {
                              echo "------------------------------ inside promote condition -------------------------------"
                              def registry_url_string = "${metadataVars.promoteSource}"
                              withCredentials([usernamePassword(credentialsId: "${metadataVars.promoteSourceArtifactoryCredId}", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                                  if ("${metadataVars.promoteSourceArtifactoryType}" == 'ECR') {
                                     temp_url = registry_url_string.split(':')
                                     temp_url2 = temp_url[0]
                                     url = temp_url2.split('\\.')
                                     env.PROMOTE_SOURCE_ECR_LOGIN_URL = temp_url[0]
                                     env.PROMOTE_SOURCE_ECR_REGION = url[3]
                                     echo "ecr region: $PROMOTE_SOURCE_ECR_REGION"
                                     sh 'set +x; AWS_ACCESS_KEY_ID=$USERNAME AWS_SECRET_ACCESS_KEY=$PASSWORD aws ecr get-login-password --region "$PROMOTE_SOURCE_ECR_REGION" | docker login --username AWS --password-stdin $PROMOTE_SOURCE_ECR_LOGIN_URL ;set -x'
                                  }
                                  if ("${metadataVars.promoteSourceArtifactoryType}" == 'JFROG') {
                                     temp_url = registry_url_string.split(':')
                                     env.PROMOTE_SOURCE_ACR_LOGIN_URL = temp_url[0]
                                     sh 'docker login -u "\"$USERNAME\"" -p "\"$PASSWORD\"" "$PROMOTE_SOURCE_ACR_LOGIN_URL"'
                                  }
                                  if ("${metadataVars.promoteSourceArtifactoryType}" == 'ACR') {
                                     temp_url = registry_url_string.split(':')
                                     env.PROMOTE_SOURCE_JFROG_LOGIN_URL = temp_url[0]
                                     sh 'docker login -u "\"$USERNAME\"" -p "\"$PASSWORD\"" "$PROMOTE_SOURCE_JFROG_LOGIN_URL"'
                                  }
                              }
                              sh """ docker pull "${metadataVars.promoteSource}" """
                              sh """ docker image tag "${metadataVars.promoteSource}" "$REGISTRY_URL:${metadataVars.promoteTag}" """
                              sh """ docker push "$REGISTRY_URL:${metadataVars.promoteTag}" """
                              env.BUILD_TAG = "${metadataVars.promoteTag}"
                          }
                             
                        }
              }
            else if ("${list[i]}" == "'Deploy'" && "$PUBLISH_ARTIFACT" == "false") {
              stage('Deploy') {
                // stage details here
                if (env.ACTION == 'DEPLOY' || env.ACTION == 'PROMOTE' || env.ACTION == 'ROLLBACK') {

                  if (env.ACTION == 'ROLLBACK') {
                      echo "-------------------------------------- inside rollback condition -------------------------------"
                      env.BUILD_TAG = "${metadataVars.rollbackTag}"

                  }
                  if (env.DEPLOYMENT_TYPE == 'EC2') {
                      withCredentials([usernamePassword(credentialsId: "$ARTIFACTORY_CREDENTIALS", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                          if (env.ARTIFACTORY == 'ECR') {
                              sh 'set +x; ssh -o "StrictHostKeyChecking=no" ciuser@$DOCKERHOST "AWS_ACCESS_KEY_ID=$USERNAME AWS_SECRET_ACCESS_KEY=$PASSWORD aws ecr get-login-password --region "$ECR_REGION" | docker login --username AWS --password-stdin $REGISTRY_URL " ;set -x'
                          }
                          if (env.ARTIFACTORY == 'JFROG') {
                              sh 'ssh -o "StrictHostKeyChecking=no" ciuser@$DOCKERHOST "docker login -u "\"$USERNAME\"" -p "\"$PASSWORD\"" "$REGISTRY_URL""'
                          }
                          if (env.ARTIFACTORY == 'ACR') {
                              sh 'ssh -o "StrictHostKeyChecking=no" ciuser@$DOCKERHOST "docker login -u "\"$USERNAME\"" -p "\"$PASSWORD\"" "$ACR_LOGIN_URL""'
                          }
                      }
                      sh 'ssh -o "StrictHostKeyChecking=no" ciuser@$DOCKERHOST "sleep 5s"'
                      sh 'ssh -o "StrictHostKeyChecking=no" ciuser@$DOCKERHOST "docker pull "$REGISTRY_URL:$BUILD_TAG""'
                      sh """ssh -o "StrictHostKeyChecking=no" ciuser@$DOCKERHOST "docker stop "${metadataVars.repoName}" || true && docker rm "${metadataVars.repoName}" || true" """
                  // Read Docker Vault properties
                  String dockerProperties = parseJsonString(env.JENKINS_METADATA,'docker')
                  dockerData = parseJsonArray(dockerProperties)
                  if(dockerData['vault']){

                      String vaultProperties = parseJsonString(dockerProperties,'vault')
                      def vaultType = parseJsonArray(vaultProperties)
                      if (vaultType.type == 'vault') {
                          echo "type is vault"
                          env.VAULT_TYPE = 'vault'

                          String vaultConfiguration = parseJsonString(vaultProperties,'configuration')
                          def vaultData = parseJsonArray(vaultConfiguration)
                          def vaultConfigurations = returnVaultConfig(vaultData.vaultUrl, vaultData.vaultCredentialID)
                          env.VAULT_CONFIG = vaultConfigurations
                          // Getting the secret Values
                          String vaultSecretValues = parseJsonString(vaultProperties,'secrets')
                          def vaultSecretData = parseJsonArray(vaultSecretValues)
                          def vaultSecretConfigData = returnSecret(vaultSecretData.path, vaultSecretData.secretValues)
                          env.VAULT_SECRET_CONFIG = vaultSecretConfigData
                          def dockerEnv = dockerVaultArguments(vaultSecretData.secretValues)
                          def secretkeys = dockerVaultArgumentsFile(vaultSecretData.secretValues)

                          withVault([configuration: vaultConfigurations, vaultSecrets: [vaultSecretConfigData]]) {
                              def data = []
                              for(secret in dockerEnv){
                                  sh "echo $secret"
                              }
                              for(keys in secretkeys){
                                  sh "echo $keys=\$(cat .$keys) >> .secrets"
                              }
                              sh 'cat .secrets;'
                          }
                          def result = sh(script: 'cat .secrets', returnStdout: true)


                          sh 'scp -o "StrictHostKeyChecking=no" .secrets ciuser@$DOCKERHOST:/home/ciuser/docker-env'
                          //sh 'ssh -o "StrictHostKeyChecking=no" ciuser@$DOCKERHOST "echo $SECRETS > secrets"'
                          sh 'rm -rf .secrets'
                          if (env.DEPLOYMENT_TYPE == 'EC2' && env.CONTEXT == 'null') {
                              sh """ ssh -o "StrictHostKeyChecking=no" ciuser@$DOCKERHOST "docker run -d --restart always --name "${metadataVars.repoName}"  --env-file docker-env -p ${dockerData.hostPort}:$SERVICE_PORT -e port=$SERVICE_PORT $REGISTRY_URL:$BUILD_TAG" """
                          }
                          else if (env.DEPLOYMENT_TYPE == 'EC2' && env.CONTEXT != 'null') {
                              sh """ ssh -o "StrictHostKeyChecking=no" ciuser@$DOCKERHOST "docker run -d --restart always --name "${metadataVars.repoName}"  --env-file docker-env   -p ${dockerData.hostPort}:$SERVICE_PORT -e context=$CONTEXT -e port=$SERVICE_PORT $REGISTRY_URL:$BUILD_TAG" """
                          }
                      }
                  }
                  else {
                      if (env.DEPLOYMENT_TYPE == 'EC2' && env.CONTEXT == 'null') {
                          sh """ ssh -o "StrictHostKeyChecking=no" ciuser@$DOCKERHOST "docker run -d --restart always --name "${metadataVars.repoName}" -p ${dockerData.hostPort}:$SERVICE_PORT -e port=$SERVICE_PORT $REGISTRY_URL:$BUILD_TAG" """
                      }
                      else if (env.DEPLOYMENT_TYPE == 'EC2' && env.CONTEXT != 'null') {
                          sh """ ssh -o "StrictHostKeyChecking=no" ciuser@$DOCKERHOST "docker run -d --restart always --name "${metadataVars.repoName}" -p ${dockerData.hostPort}:$SERVICE_PORT -e context=$CONTEXT -e port=$SERVICE_PORT $REGISTRY_URL:$BUILD_TAG" """
                  }
              }
            }
            if (env.DEPLOYMENT_TYPE == 'KUBERNETES' || env.DEPLOYMENT_TYPE == 'OPENSHIFT') {
                withCredentials([file(credentialsId: "$KUBE_SECRET", variable: 'KUBECONFIG'), usernamePassword(credentialsId: "$ARTIFACTORY_CREDENTIALS", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                    sh """
                        sed -i s+#SERVICE_NAME#+"${metadataVars.helmReleaseName}"+g ./helm_chart/values.yaml ./helm_chart/Chart.yaml
                        docker run --rm  --user root -v "$KUBECONFIG":"$KUBECONFIG" -e KUBECONFIG="$KUBECONFIG" $KUBECTL_IMAGE_VERSION create ns "$namespace_name" || true
                    """

                        if (env.DEPLOYMENT_TYPE == 'OPENSHIFT') {
                            sh '''
                                  COUNT=$(grep 'serviceAccount' Helm.yaml | wc -l)
                                  if [[ $COUNT -gt 0 ]]
                                  then
                                      ACCOUNT=$(grep 'serviceAccount:' Helm.yaml | tail -n1 | awk '{ print $2}')
                                      echo $ACCOUNT
                                  else
                                      ACCOUNT='default'
                                  fi
                                  docker run --rm  --user root -v "$KUBECONFIG":"$KUBECONFIG" -e KUBECONFIG="$KUBECONFIG" -v "$WORKSPACE":/apps -w /apps $OC_IMAGE_VERSION oc adm policy add-scc-to-user anyuid -z $ACCOUNT -n "$namespace_name"

                            '''
                        }
                        
                           env.kube_secret_name_for_registry = "$ARTIFACTORY_CREDENTIALS".toLowerCase()
                           if (env.ARTIFACTORY == 'JFROG') {
                               sh '''
                               docker run --rm  --user root -v "$KUBECONFIG":"$KUBECONFIG" -e KUBECONFIG="$KUBECONFIG" $KUBECTL_IMAGE_VERSION -n "$namespace_name" delete secret $kube_secret_name_for_registry --ignore-not-found || true
                               docker run --rm  --user root -v "$KUBECONFIG":"$KUBECONFIG" -e KUBECONFIG="$KUBECONFIG" $KUBECTL_IMAGE_VERSION -n "$namespace_name" create secret docker-registry $kube_secret_name_for_registry --docker-server="$JFROG_LOGIN_URL" --docker-username="\"$USERNAME\"" --docker-password="\"$PASSWORD\"" || true
                               '''
                           }
                           if (env.ARTIFACTORY == 'ACR') {
                               sh '''
                                 docker run --rm  --user root -v "$KUBECONFIG":"$KUBECONFIG" -e KUBECONFIG="$KUBECONFIG" $KUBECTL_IMAGE_VERSION -n "$namespace_name" delete secret $kube_secret_name_for_registry --ignore-not-found || true
                                 docker run --rm  --user root -v "$KUBECONFIG":"$KUBECONFIG" -e KUBECONFIG="$KUBECONFIG" $KUBECTL_IMAGE_VERSION -n "$namespace_name" create secret docker-registry $kube_secret_name_for_registry --docker-server="$ACR_LOGIN_URL" --docker-username="\"$USERNAME\"" --docker-password="\"$PASSWORD\"" || true
                               '''
                           }
                           sh """
                           ls -lart
                           echo "context: $CONTEXT" >> Helm.yaml
                           cat Helm.yaml
                           sed -i s+#SERVICE_NAME#+"${metadataVars.helmReleaseName}"+g ./helm_chart/values.yaml ./helm_chart/Chart.yaml
                           docker run --rm  --user root -v "$KUBECONFIG":"$KUBECONFIG" -e KUBECONFIG="$KUBECONFIG" -v "$WORKSPACE":/apps -w /apps $HELM_IMAGE_VERSION upgrade --install "${metadataVars.helmReleaseName}" -n "$namespace_name" helm_chart --atomic --timeout 300s --set image.repository="$REGISTRY_URL" --set image.tag="$BUILD_TAG" --set image.registrySecret="$kube_secret_name_for_registry"  --set service.internalport="$SERVICE_PORT" -f Helm.yaml
                           """
                        }
                    }
                }
              }
            } else if ("${list[i]}" == "'FunctionalTests'" && env.ACTION == 'DEPLOY' && stage_flag['FunctionalTesting'] && "$PUBLISH_ARTIFACT" == "false") {
                stage('Functional Tests') {
                    waitforsometime()
                    dir('testcaseRepo') {
                        checkoutRepository()
                        if (env.TESTINGTOOLTYPE == 'cypress') {
                            runCypressTest()
                            publishResults('mochawesome-report', 'output.html', 'Cypress Test Report', 'Cypress Test Report')
                        }
                    }
                }
            } else if ("${list[i]}" == "'Destroy'" && env.ACTION == 'DESTROY' && "$PUBLISH_ARTIFACT" == "false") {
              stage('Destroy') {
                // stage details here
                    if (env.DEPLOYMENT_TYPE == 'EC2') {
                         sh """ ssh -o "StrictHostKeyChecking=no" ciuser@$DOCKERHOST "docker stop ${metadataVars.repoName} || true && docker rm ${metadataVars.repoName} || true" """
                    }
                    if (env.DEPLOYMENT_TYPE == 'KUBERNETES' || env.DEPLOYMENT_TYPE == 'OPENSHIFT') {
                      withCredentials([file(credentialsId: "$KUBE_SECRET", variable: 'KUBECONFIG')]) {
                            sh """
                            docker run --rm  --user root -v "$KUBECONFIG":"$KUBECONFIG" -e KUBECONFIG="$KUBECONFIG" -v "$WORKSPACE":/apps -w /apps $HELM_IMAGE_VERSION uninstall "${metadataVars.helmReleaseName}" -n "$namespace_name"
                            """
                      }
                    }
              }
            }
          }
        }
      }
    }
  }
  post { 
        always {
            sh """ sudo chown -R `id -u`:`id -g` "$WORKSPACE" """
            pushToCollector()
        }
        cleanup {
            sh 'docker  rmi  $REGISTRY_URL:$BUILD_TAG $REGISTRY_URL:latest || true'
        }
  }
}
