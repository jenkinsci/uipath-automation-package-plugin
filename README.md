UiPath Jenkins Plugin
====================

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/uipath-automation-package.svg)](https://plugins.jenkins.io/uipath-automation-package)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/uipath-automation-package-plugin.svg?label=changelog)](https://github.com/jenkinsci/uipath-automation-package-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/uipath-automation-package.svg?color=blue)](https://plugins.jenkins.io/uipath-automation-package)

## About this plugin

This plugin allows you to build and deploy UiPath automation processes, as well as run UiPath automated test cases.

To pack projects created with UiPath Studio starting from 20.10, you need to use a version starting from 2.1 of this extension. Also in order to pack solutions created with UiPath Studio starting from 25.10, you need to use plugin version starting from 6.0.

For up-to-date documentation, including setup for new Solution tasks, please check the [official UiPath Documentation](https://docs.uipath.com/cicd-integrations/standalone/2025.10/user-guide/about-jenkins-plugin).

## Prerequisites

* Jenkins server running on Windows or Linux
* Orchestrator instance (basic, federated or cloud authentication are supported):
  * Solutions features are available only on Orchestrator Cloud or AutomationSuite 25.10+.
  * Testing features require Orchestrator version 20.4 or newer.
  * When using an on-premise Orchestrator under HTTPS, make sure to import the SSL Certificate so that the HTTPS calls to Orchestrator can be trusted.

## Installing and enabling the plugin

The Jenkins plugin can be installed from any Jenkins installation connected to the Internet using the **Plugin Manager** screen.

## Configure service connection for external apps
Step 1: [Configure your external application and scopes in Automation Cloud](https://docs.uipath.com/orchestrator/docs/managing-external-applications#adding-an-external-application). After adding the application, keep the App ID, Secret and Application Scopes at hand, to be used for the next step. 
<br/><t><b>Note:</b> If you generate and use a new Secret, the old one is going to be invalidated.  
Step 2: [Configure application credentials as secret text in Jenkins](https://www.jenkins.io/doc/book/using/using-credentials/). For this step you need the Secret generated in Automation Cloud. <br/> Step 3: Configure the Authentication for each task under Post-Build Actions, by adding the Account Name, followed by the App ID, Secret and Application Scopes generated through Automation Cloud. <br/><br/><b>Note:</b> Consider using the external app in individual pipelines to avoid invalidation errors.

## Additional information

All paths specified should be local to the current workspace. You can use environment variables in paths, though you should ensure that they result in paths that are local to the workspace. All paths

In order to deploy packages or run tests, ensure that the authenticated user has the Folders View (or OrganizationUnits View) and (20.4+ only) Background Tasks View permissions.

In order to package libraries when connected to an Orchestrator instance, ensure that the authenticated user has the Libraries View permission.

Authentication on Cloud Orchestrator using External Apps. 

For further details on managing external apps on Orchestrator, pls refer to the [official documentation.](https://docs.uipath.com/automation-cloud/docs/managing-external-applications) 

## Questions

Do you have any questions regarding the plugin? Ask them [here](https://connect.uipath.com/marketplace/components/jenkins-plugin-for-uipath-public-preview/questions).

## Troubleshooting
#### [Unauthorized error](#unauthorized-error)
#### [Forbidden error](#forbidden-error)
#### [Folder/environment not found](#folder/environment-not-found)
#### [Package already exists (Conflict)](#package-already-exists-conflict)
#### [Failed to run the command (Generic error)](#failed-to-run-the-command-generic-error))
#### [Jenkins fails to process paths containing non-Latin characters](#jenkins-fails-to-process-paths-containing-non-latin-characters)

### Unauthorized error
If using basic authentication:
* ensure the correctness of the username-password combination on the web login
* if federated authentication is enabled, make sure your write the username in the task as “DOMAIN\user”

If using token authentication:
* Revoke the token from the API access panel and generate a new one
* Ensure that the user that generated the key has can access the Orchestrator and has a user on the Orchestrator instance

If authenticating against on an on-premise Orchestrator you might receive this error as a result of the certificate used for the Orchestrator not being valid. This might mean that it has the wrong CN or other validation issues. Ensure that the Orchestrator certificate is valid and that the machine running the job trusts the Orchestrator certificate in case you are using a self-signed certificate.

### Forbidden error
Likely, the user does not have the permission to perform the action.

Ensure that the user has permissions to read folders, upload packages, create and update processes, read test sets and test cases, create and run test sets, read background tasks.

### Folder/environment not found
Ensure that the authenticated user used by CI/CD plugins has the Folders.View and (20.4 only) BackgroundTask.View permissions.

### Package already exists (Conflict)
Ensure that the package that you are trying to deploy does not exist with the same version already. If it does, consider using automatic package versioning, so that the new version is bumped up every time we deploy.

### Failed to run the command (Generic error)
If the Jenkins workspace is inside a location on disk (like C:\Windows or C:\Program Files) to which the user does not have permissions, ensure that the workspace is placed on a path that can be accessed smoothly by the user

### Jenkins fails to process paths containing non-Latin characters
Jenkins is not able to pass correctly non-standard encoded characters when invoking the UiPath Plugin. The unknown characters will be replaced by ???.

The solution depends on how Jenkins is deployed on both the server and the agent host machines, but involves setting "file.encoding" to UTF-8 in Java options:
* Windows
	* Running Jenkins in Windows as a Service
	In the service configuration file add the arguments into the <arguments> tag. It should look like this:
	```
	<arguments>-Xrs -Xmx512m -Dhudson.lifecycle=hudson.lifecycle.WindowsServiceLifecycle -Dfile.encoding=UTF-8 -jar "%BASE%\jenkins.war" --httpPort=8080 --webroot="%BASE%\war"</arguments>
	```
	
	* Running Jenkins inside Docker
	The JAVA_OPTS should be passed to the container via --env JAVA_OPTS="..." like the following:
	```
	docker run --name myjenkins -p 8080:8080 -p 50000:50000 --env JAVA_OPTS=-Dhudson.lifecycle=hudson.lifecycle.WindowsServiceLifecycle -Dfile.encoding=UTF-8 jenkins/jenkins:lts
	```
	
	* Running Jenkins inside Tomcat
	Use environment variable CATALINA_OPTS:
	```
	export CATALINA_OPTS="-DJENKINS_HOME=/path/to/jenkins_home/ -Dhudson.lifecycle=hudson.lifecycle.WindowsServiceLifecycle -Dfile.encoding=UTF-8 -Xmx512m"
	```
* Linux
	* Debian / Ubuntu based Linux distributions
	In the configuration file search for the argument JAVA_ARGS and add the file enconding. It might look like this:
	```
	JAVA_ARGS="-Dfile.encoding=UTF-8 -Xmx512m"
	```
	
	* RedHat Linux based distributions
	In the configuration file search for the argument JENKINS_JAVA_OPTIONS and add the file enconding. It might look like this:
	```
	JENKINS_JAVA_OPTIONS="-Dfile.encoding=UTF-8 -Xmx512m"
	```

## License

[UiPath Open Platform License Agreement – V.20190913](./LICENSE.md)
