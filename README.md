Box Benchmark Framework
==================
The Box Benchmark Framework project allows you to create load tests with the [Box Java SDK](https://github.com/box/box-java-sdk), REST Endpoints, and more leveraging Apache JMeter. 

Apache JMeter
-------------
The [Apache JMeter™](http://jmeter.apache.org/) application is an open source project designed to load test functional behavior and measure performance. 


JMeter Box Java SDK Sampler Clients Currently Implemented
------------------------------------------------
* [GetBoxConnection](https://github.com/kylefernandadams/box-benchmark-framework/blob/master/src/main/java/com/box/platform/jmeter/sampler/GetBoxConnection.java)
* [CreateFolder](https://github.com/kylefernandadams/box-benchmark-framework/blob/master/src/main/java/com/box/platform/jmeter/sampler/CreateFolder.java)
* [UploadFile](https://github.com/kylefernandadams/box-benchmark-framework/blob/master/src/main/java/com/box/platform/jmeter/sampler/UploadFile.java)
* [CreateMetadataOnFile](https://github.com/kylefernandadams/box-benchmark-framework/blob/master/src/main/java/com/box/platform/jmeter/sampler/CreateMetadataOnFile.java)
* [DownloadFile](https://github.com/kylefernandadams/box-benchmark-framework/blob/master/src/main/java/com/box/platform/jmeter/sampler/DownloadFile.java)


JMeter Test Plans
-----------------
* [box_http1_duration.jmx](https://github.com/kylefernandadams/box-benchmark-framework/blob/master/src/test/jmeter/box_http1_duration.jmx)
  *  Leverages the JMeter-provided Apache HttpClient4 library to execute tests for a number of files
* [box_http1_volume.jmx](https://github.com/kylefernandadams/box-benchmark-framework/blob/master/src/test/jmeter/box_http1_volume.jmx)
  *  Leverages the JMeter-provided Apache HttpClient4 library to execute tests for a number of files within a duration
* [box_java_sdk_duration.jmx](https://github.com/kylefernandadams/box-benchmark-framework/blob/master/src/test/jmeter/box_java_sdk_duration.jmx)
  * Leverages the Box Java SDK (HTTP/1.1) to execute tests for a number of files within a duration
* [box_java_sdk_volume.jmx](https://github.com/kylefernandadams/box-benchmark-framework/blob/master/src/test/jmeter/box_java_sdk_volume.jmx)
  * Leverages the Box Java SDK (HTTP/1.1) to execute tests for a number of files

Prerequisites
-------------
* JDK 11 or Higher
* Check your JDK version
```bash
mbp-kadams:apache-jmeter-5.5 kadams$ java --version
openjdk 17.0.1 2021-10-19
OpenJDK Runtime Environment Temurin-17.0.1+12 (build 17.0.1+12)
OpenJDK 64-Bit Server VM Temurin-17.0.1+12 (build 17.0.1+12, mixed mode, sharing)
```
* Maven
* Check your Maven version
```bash
mbp-kadams:apache-jmeter-5.5 kadams$ mvn -version
Apache Maven 3.8.5 (3599d3414f046de2324203b78ddcf9b5e4388aa0)
Maven home: /usr/local/Cellar/maven/3.8.5/libexec
```

Installation
------------
1. Clone box-benchmark-framework repo:
```bash
git clone https://github.com/kylefernandadams/box-benchmark-framework
```
3. Change dir to box-benchmark-framework
```bash
cd box-benchmark-framework
```
4. Build the project using Maven
```bash
mvn clean package
```

Configuration - Properties Files
-------------
1. Edit the [box.properties](https://github.com/kylefernandadams/box-benchmark-framework/blob/master/src/test/jmeter/box.properties) file with your Box application and enterprise specifics.

| Property               | Sample Value                                 | Description                                                                                                                                     |
|------------------------|----------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| thread.count           | 1                                            | Number of threads or users to use. Keep this at 1 for now. <br/> ***Recommendation***: Start with a low number (thread.count=1)                 |
| duration.seconds       | 300                                          | Overall duration to attempt to run the tests in seconds                                                                                         |
| max.file.count         | 10                                           | Total number of files to include in the load test <br/> ***Recommendation***: Start with a low number (max.file.count=1) and gradually increase |
| rampup                 | 4                                            | Amount of time in seconds to ramp up before executing test threads                                                                              |
| user.login             | me@email.com                                 | Email address for the user to run the tests as                                                                                                  |
| box.eid                | 12345                                        | Box Enterprise Id used in Metadata POST requests                                                                                                |
| box.config.path        | /my/path/123_config.json                     | Path to the Box application config JSON file used for instantiating JWT connections                                                             |
| box.folders.endpoint   | https://api.box.com/2.0/folders              | Endpoint for Box folders                                                                                                                        |
| box.upload.endpoint    | https://upload.box.com/api/2.0/files/content | Endpoint for Box file upload                                                                                                                    |
| box.files.endpoint     | https://api.box.com/2.0/files                | Endpoint for Box files                                                                                                                          |
| max.cache.entries      | 100                                          | Access token cache int for Box JWT client                                                                                                       |
| max.folder.count       | 1                                            | Controls how many parent folders to create during the load test                                                                                 |
| sample.file.path       | /path/to/my/Text.txt                         | Path to the sample file used in the load tests                                                                                                  |
| file.mime.type         | text/plain                                   | Mime/type for the sample file                                                                                                                   |
| metadata.template.key  | account                                      | Box metadata template key                                                                                                                       |
| string.metadata.keys   | accountType,accountName                      | Comma-delimited string of metadata attribute keys                                                                                               |
| string.metadata.values | Customer,AcmeCo                              | Comma-delimited string of metadata attribute values that pair up with the above keys                                                            |
| number.metadata.keys   | accountId,accountZip                         | Comma-delimited string of metadata attribute keys                                                                                               |
| number.metadata.values | 123456,12345                                 | Comma-delimited string of metadata attribute values that pair up with the above keys                                                            |

| base.folder.id        | 0                                            | The base folder to create folders and upload files for the performance test                                                                     |
2. Enable or disable test plans to run by commenting/uncomment out each test plan in the [pom.xml](/pom.xml#L114) file. 

Run Options - JMeter GUI
-------------
* This is useful for making changes to the Test Plans or running smoke tests
```bash
mvn jmeter:configure jmeter:gui -DguiTestFile="src/test/jmeter/box_java_sdk_volume.jmx"
```
OR
```bash
mvn jmeter:configure jmeter:gui -DguiTestFile="src/test/jmeter/box_http1_volume.jmx"
```

Run Options - Command Line
-------------
* This will run all of the test plans that were commented out in the pom.xml file
```bash
mvn clean verify
```

Reviewing Results - Command Line
-------------
1. Results will be generated in the /target/jmeter/reports/{test_plan_name} directory
2. Open the index.html file in your browser of choice

Debugging
------------
* System.out command will be written to the console.
* In addition, log files can be found in the /target/jmeter/logs directory


Disclaimer
This project is comprised of open source examples and should not be treated as an officially supported product. Use at your own risk. If you encounter any problems, please log an issue.

License
The MIT License (MIT)

Copyright (c) 2022 Kyle Adams

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
