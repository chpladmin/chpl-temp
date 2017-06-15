# chpl-temp

The CHPL temp 

# Installation instructions

## Install required software

Java 1.8.0
mvn 3.3.3
Eclipse (latest)

## Clone the repository

```sh
$ git clone https://github.com/chpladmin/chpl-temp.git
```

## Deploy the application
In order to deploy the application and run the executable jar, it must have the following resources in the classpath:
- certStatusApp-jar-with-dependencies.jar (compiled code to execute including packaged references)
- updateCertificationStatusApp.sh
- environment.properties (should follow environment.properties.template)
- log4j2.xml (optional but recommended)
- closeUpdateCertificationStatusApp.sh (if running the app on a schedule, this script will close it)

If desired, update crontab to schedule execution for updateCertificationStatusApp.sh and schedule execution for closeUpdateCertificationStatusApp.sh at the desired times.