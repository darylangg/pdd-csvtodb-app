# ControllerBase Documentation

## Introduction 
This repo acts as a boilerplate code for the development of ODP controllers. All controllers should have the following features:
* Fanout of last known data
  * As Data comes in, the latest reading of each device should be captured. This data will be periodically fanned out.
* Fanout of data changes
  * Data is fanned out to a different exchange whenever there is meaningful change detected above a certain threshold (vertical specific)
* Fanout of device connectivity
  * Device connectivity is passed from converter/router to a fanout exchange. A timer is also in place to check if the connectivity of each device has expired.
* Writing of new data to CSV
  * New data is written into a CSV file, to be ingested by csvtodb. 
* RPC Data Initialization
  * The list of last known data will be returned to the user upon an RPC init request.
* RPC Control (depending on vertical)
  * As control requests come in, log the request into DB via hapi and send it to the converter/router for processing.

## Getting Started
1. Fork this repository to a new repository 
2. Run the `init.sh` script to initialize the `application.properties` file with the appropriate configs
   * Script needs to be run in a Linux console (eg, git bash, ubuntu)
   * Depending on the env where the script runs from, the first line of `init.sh` will need to be using `bash` or `sh`
   * This script will edit `pom.xml` and `application.properties` to configure your project
```
> ./init.sh
> Enter project: <project-name>
> Enter vertical: <vertical-name>
``` 
4. Configure jaspyt VM options to decode `encrypted.properties`. From IntelliJ, add the following configurations:
   1. Run > Edit Configuration > edit configuration templates > Application > modify options > Add VM Options
   2. Add `-Djasypt.encryptor.password=<jaspyt-password>`
   3. Do the same for Run > Edit Configuration > edit configuration templates > Junit > modify options > Add VM Options
5. Verify that `controllerbase` is working as expected by running all test cases (WIP)

## Code Structure
1. CSVRoute
2. CSVWriterRoute
   ```sequence
   Alice->Bob: Hello Bob, how are you?
   Note right of Bob: Bob thinks
   Bob-->Alice: I am good thanks!
   ```
3. DataFanOutRoute
4. DeviceConnectivityPollRoute
5. DeviceConnectivityRoute
6. InitRoute
7. RPCControlRequestRoute
8. RPCInitRoute

## Configuring your project
1. Add vertical `<vertical>.proto` files to `/src/main/protobuf` and run `gen_proto.bat` from `/protobuf`. This will compile the ```.java``` files required.
2. Update the sections which contain the TODO tag
   1. `DataBean.updateCache()`
      * This function takes in data with the `CommonProto.Data` format
      * Add new devices to `latestData`, update `newUpdates` if the data change is above a certain threshold
      * Incoming vendorId needs to be remapped with the mappings from db
      * This threshold is to be identified based on vertical specifications
   2. `RPCBean.prepareDBPayload()`
      * This function parses a HashMap from the `CommonProto.RPC` format
      * The payload format should be aligned with the endpoints on the Hapi server
   3. `RPCBean.getReplyBuilder()`
      * MessageId to be set based
   4. `RPCBean.prepareRequestForIng()`
      * Requests come in to controller using device Id (eg. ACMV-1)
      * This device Id will have to be remapped to vendorDeviceId (eg. ACMV-1 -> WNC-BTU-01)
      * Repack the request body with the vendorDeviceIds
   5. `getInitReply`
      * Set the vertical and message type (eg. for door data, set vertical="door", messageType="doorData")
   6. `WriteBuffer.protobufToCSVString()`
      * This function parses a HashMap from the `CommonProto.Data` format
      * The payload format should be aligned with the endpoints on the Hapi server
   7. Add test cases where appropriate
3. Update `application.properties`
   1. Producer/consumer certs
   2. `defaultIngSrc` - Data source
   3. `defaultAppSrc` - Data destination
   4. CSV Configs - directory of CSV files, csv headers
   5. `default.device-category`
4. Update `encrpyted.properties`
   1. Use the following guide to encrypt passwords using jaspyt
   2. https://docs.google.com/document/d/1oXHtB-8IEhEnqVbzyZQOzKpRTziv9MsyMGhkeQlzQUk/edit?usp=sharing

## Deploying the application
1. To deploy applications, we package the image in the `tar.gz` format and deploy it on our servers
2. Use the `deploy` lifecycle in maven to generate the `tar.gz` image in the target folder
3. Use the following example `run.sh` to run the image in the server
```
#!/bin/sh
echo Enter password
read -s password

vertical=<vertical>
container_name="$vertical-controller-app"
version=v1.0.0
echo "container name - $container_name"
if [ "$(docker ps -qa -f name=$container_name)" ]; then
    echo ":: Found container - container_name"
    if [ "$(docker ps -q -f name=$container_name)" ]; then
        echo ":: Stopping running container - $container_name"
        docker stop $container_name;
    fi
    echo ":: Removing stopped container - $container_name"
    docker rm $container_name;
fi

docker load --input "$container_name-$version.tar.gz"

docker run -d \
--name $container_name \
--log-opt max-size=50m --log-opt max-file=5 \
--log-opt tag="{{.ImageName}}|{{.Name}}|{{.ImageFullID}}|{{.FullID}}" \
-e jasypt.encryptor.password=$password \
-v <certDir>:/certs \
-v <csvDir>:/out \
-v <logDir>:/logs \
--mount type=bind,source="${PWD}/application.properties",target=/application.properties \
--mount type=bind,source="${PWD}/encrypted.properties",target=/encrypted.properties \
"$container_name:$version"
```