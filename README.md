# IntelliJ IDEA plugin connecting to Teamserver REST API. #

### Prerequisites ###

* IntelliJ IDEA version 23.3 or later (either Community Edition) must be installed on your computer.
* Plugin DevKit plugin must be enabled in IDE. Go to IntelliJ settings => Plugins => search for Plugin DevKit and make
sure it is enabled.
* IntelliJ Platform SDK must be configured for your IDEA project. Right click on project folder in IntelliJ => Open
Module Settings => Project => Project SDK.
If necessary add a new "IntelliJ Platform Plugin SDK". It should point to the installation folder of IntelliJ.
* Project language level in project settings should be set to 8. Right click on project folder in IntelliJ => Open
Module Settings => Project => Project language level.
More information on setting up development
environment: http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/setting_up_environment.html
* Gradle must be installed and added to "Path" system environment variable: https://gradle.org/install/
* Java 17

### Build from the command line ###

* Clone the project.
* Run `./gradlew buildPlugin`in project root directory.
* Plugin zip archive should be generated to project root folder/build/distributions.
* To install the generated zip archive of the plugin go to File => Settings => Plugins => Install Plugin from disk.

### Build from IDE ###

* Clone the project.
* Open the project with IntelliJ IDEA.
* In the "Event Log" notification, which should appear in the bottom right corner, click "Import Gradle project".
* Fill in the import dialog.
* Open Gradle tool window and run "buildPlugin" task, which is in the "intellij" task folder.
* Plugin zip archive should be generated to project root folder/build/distributions.
* To install the generated zip archive of the plugin go to File => Settings => Plugins => Install Plugin from disk.

### Run from the command line ###

* Run ``` ./gradlew runIde ``` from the command line within the project root folder.
* A separate instance of IntelliJ IDEA should be launched with the plugin installed.

### Run from the IDE ###

* Build the project
* Open Gradle tool window and run "runIdea" task, which is inside the "intellij" task folder.
* A separate instance of IntelliJ IDEA should be launched with the plugin installed.