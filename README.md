UiPath Jenkins Plugin
====================

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/uipath-automation-package.svg)](https://plugins.jenkins.io/uipath-automation-package)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/uipath-automation-package-plugin.svg?label=changelog)](https://github.com/jenkinsci/uipath-automation-package-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/uipath-automation-package.svg?color=blue)](https://plugins.jenkins.io/uipath-automation-package)

## About this plugin

This plugin allows you to build and deploy UiPath automation processes, as well as run UiPath automated test cases.

## Prerequisites

* Jenkins server running on Windows or Linux
* At least one Jenkins slave running on Windows (required for step execution)
* Orchestrator instance (basic, federated or cloud authentication are supported)
  * Testing features require Orchestrator version 20.4 or newer
  * When using an on-premise Orchestrator under HTTPS, make sure to import the SSL Certificate so that the HTTPS calls to Orchestrator can be trusted

## Installing and enabling the plugin

The Jenkins plugin can be installed from any Jenkins installation connected to the Internet using the **Plugin Manager** screen.

## Steps

### 📦 UiPath Pack

**Application:** RPA

**Type:** Build task

UiPath Pack is available in standard jobs and pipelines, and lets you package an existing UiPath project into a NuGet package.

[![UiPath Pack](.github/pack.png)](.github/pack.png)

**⚙️ Configuration**

| Job parameter                 | Description           |
| ----------------------------- | -------------         |
| Choose versioning method      | UiPath packages are versioned. With UiPath pack you can choose between 3 different options: (1) Auto generate (2) Define custom version (3) Use the current version set in the project. |
| Project(s) path               | The location of the project(s) to be packaged. It can be a direct path to a project.json file or a directory with one or multiple projects. In the latter case, each level one project is packaged individually. |
| Output folder                 | Path to a folder, where the created package should be placed. |

**📖 Pipeline Example:**

```Groovy
pipeline {
  agent any
  environment {
      MAJOR = '1'
      MINOR = '0'
  }
  stages {
    stage ('Build') {
      steps {
        UiPathPack (
          outputPath: "Output\\${env.BUILD_NUMBER}",
          projectJsonPath: "UiBank\\project.json",
          version: [$class: 'ManualVersionEntry', version: "${MAJOR}.${MINOR}.${env.BUILD_NUMBER}"]
        )
      }
    }
  }
}
```

### 🚀 UiPath Deploy

**Application:** RPA

**Type:** Post-Build task

UiPath Deploy is available in standard jobs and pipelines, and lets you deploy a UiPath NuGet package onto UiPath Orchestrator.

[![UiPath Deploy](.github/deploy.png)](.github/deploy.png)

**⚙️ Configuration**

| Job parameter                 | Description           |
| ----------------------------- | -------------         |
| Package(s) path               | The folder that holds your UiPath nuget package(s). |
| Orchestrator address          | The address of the Orchestrator instance onto which the package(s) will be deployed. |
| Orchestrator tenant           | The Orchestrator tenant onto which the package(s) will be deployed. |
| Orchestrator folder           | The folder to deploy to. If the folder is a classic folder, you will also need to set the environments field. For modern folders, setting the environments is not required. |
| Environments                  | The environment onto which the package will be deployed as a process. For the project and environment with existing processes, the processes will be updated to use the latest project version. Specify the environment onto which the package will be deployed as a process. For the project and environment with existing processes, the processes will be updated to use the latest project version. Required when using a classic folder, otherwise not applicable. |
| Authentication                | For authentication towards Orchestrator, credentials have to be created in Jenkins upfront. There are 2 options to authenticate: *(1)* Authenticate to an On-Premise Orchestrator using username and password *(2)* Authenticate to a Cloud Orchestrator using a refresh token (API key). The account name and API key are accessible via Services->API Access (see below for a detailed explanation on how to retrieve this). |

**📖 Pipeline Example:**

```Groovy
pipeline {
  agent any
  environment {
      MAJOR = '1'
      MINOR = '0'
  }
  stages {
    stage ('PostBuild') {
      steps {
        UiPathDeploy (
          packagePath: "path\\to\NuGetpackage",
          orchestratorAddress: "OrchestratorUrl",
          orchestratorTenant: "tenant name",
          folderName: "folder name",
          environments: "environment",
          credentials: [$class: 'UserPassAuthenticationEntry', credentialsId: “credentialsId”]
        )
      }
    }
  }
}
```

### ▶ UiPath Run tests

**Application:** Testing

**Type:** Post-Build task

UiPath Run tests is available in standard jobs and pipelines, and lets you *(1)* run an existing Test Set on Orchestrator, or *(2)* package, deploy and run test cases, by specifying the path to a UiPath test project.

After the test run has finished, the Test Result tab will be published to the Jenkins build, showing the detailed results. Additionally, a JUnit test results file will be output to the test result output path, when specified, or to the workspace root if not specified.

Depending on the result, the build will be either marked as successful (all test cases are passed), or unstable (at least one test case failed).

[![UiPath Run tests](.github/run-tests.png)](.github/run-tests.png)

**⚙️ Configuration**

| Job parameter                 | Description           |
| ----------------------------- | -------------         |
| Target                        | *(1)* Execute test set (specify an existing test set on UiPath Orchestrator) or *(2)* Execute test project (provide the project.json path of a UiPath Testing project) |
| Test result output path       | The executed test set returns the test result as junit.xml. Specify the path where the result should be stored, relative to the Jenkins workspace directory (e.g. result.xml). *Optional* |
| Orchestrator address          | The address of the Orchestrator instance onto which the package(s) will be deployed. |
| Orchestrator tenant           | The Orchestrator tenant onto which the package(s) will be deployed. |
| Orchestrator folder           | The folder to deploy to. If the folder is a classic folder, you will also need to set the environments field. For modern folders, setting the environments is not required. |
| Environments                  | The environment onto which the package will be deployed as a process. For the project and environment with existing processes, the processes will be updated to use the latest project version. Specify the environment onto which the package will be deployed as a process. For the project and environment with existing processes, the processes will be updated to use the latest project version. Required when using a classic folder, otherwise not applicable. |
| Authentication                | For authentication towards Orchestrator, credentials have to be created in Jenkins upfront. There are 2 options to authenticate: *(1)* Authenticate to an On-Premise Orchestrator using username and password *(2)* Authenticate to a Cloud Orchestrator using a refresh token (API key). The account name and API key are accessible via Services->API Access (see below for a detailed explanation on how to retrieve this). |
| Timeout (seconds)             | The execution timeout for the test run. The default value is 7200 seconds. If the timeout exceeds before the execution on Orchestrator is finished and returned the final result, the built will cancel and be marked as failed. |

**📖 Pipeline Example:**

```Groovy
pipeline {
  agent any
  environment {
    MAJOR = '1'
    MINOR = '0'
  }
  stages {
    stage ('PostBuild') {
      steps {
        UiPathTest (
          testTarget: [$class: 'TestSetEntry', testSet: "My Test Set"],
          orchestratorAddress: "OrchestratorUrl",
          orchestratorTenant: "tenant name",
          folderName: "folder name",
          timeout: "10000",
          testResultsOutputPath: "result.xml",
          credentials: [$class: 'UserPassAuthenticationEntry', credentialsId: "credentialsId"]
        )
      }
    }
  }
}
```

## Obtaining the Cloud Orchestrator API Key

[![How to obtain the Cloud Orchestrator API Key](.github/cloud-orchestrator-howto.png)](.github/cloud-orchestrator-howto.png)

## Additional information

All paths specified should be local to the current workspace. You can use environment variables in paths, though you should ensure that they result in paths that are local to the workspace. All paths

## Questions

Do you have any questions regarding the plugin? Ask them [here](https://connect.uipath.com/marketplace/components/jenkins-plugin-for-uipath-public-preview/questions).

## License

[UiPath Open Platform License Agreement – V.20190913](./LICENSE.md)