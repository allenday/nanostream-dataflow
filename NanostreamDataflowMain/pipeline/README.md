# Pipeline

Pipeline uses [Apache Beam](https://beam.apache.org/get-started/beam-overview/) [Dataflow Runner](https://beam.apache.org/documentation/runners/dataflow/). 


### Build from Source

#### Requirements:
- Java 1.8. Dataflow does not yet support 1.9 or greater.
- [Maven](https://maven.apache.org/install.html)
- Add required Maven dependencies to local Maven repository, such as [**Japsa 1.9-3c**](https://github.com/mdcao/japsa) package. To do this you should run following command from project root:
```
mvn install:install-file -Dfile=NanostreamDataflowMain/libs/japsa.jar -DgroupId=coin -DartifactId=japsa -Dversion=1.9-3c -Dpackaging=jar
mvn install:install-file -Dfile=NanostreamDataflowMain/libs/pal1.5.1.1.jar -DgroupId=nz.ac.auckland -DartifactId=pal -Dversion=1.5.1.1 -Dpackaging=jar
```

#### Deploy Dataflow template

To deploy the pipeline use command:

```
mvn compile exec:java  \
    -f NanostreamDataflowMain/pipeline/pom.xml  \
    -Dexec.mainClass=com.google.allenday.nanostream.NanostreamApp \
    -Dexec.args=" \
        --project=<Your project id>  \
        --runner=DataflowRunner  \
        --jobName=<Dataflow job name>  \
        --streaming=true  \
        --processingMode=<species|resistance_genes>  \
        --alignmentWindow=20  \
        --statisticUpdatingDelay=30  \
        --inputDataSubscription=<Pub/Sub subscription for new data listening> \
        --outputGcsUri=<Your output processing bucket folder>  \
        --autoStopTopic=<A topic used for autostop>  \
        --autoStopDelay=<Time in sec to stop pipilene> \
        --jobNameLabel=<Dataflow job name> \
        --gcpTempLocation=<A GCS location for temporary files>  \
        --stagingLocation=<A GCS location for staging files>  \
        --templateLocation=<A GCS location for template upload>  \
        --memoryOutputLimit=<Memory output limit> `# (Optional)` \
        --alignmentBatchSize=<Aligmnent batch size> `# (Optional)` \
        --enableStreamingEngine  \
        --workerMachineType=n1-highmem-8  \
        --diskSizeGb=100 \
        --initAutoStopOnlyIfDataPassed=false" \
    -Dexec.cleanupDaemonThreads=false
```

Parameters:
- `project`: Google Cloud project id. You can get it by running command: `gcloud config get-value project`  
- `runner`: Apache beam dataflow runner. Value `DataflowRunner`.  
- `jobName`: Dataflow job name
- `streaming`: Run pipeline in streaming mode (infinite input data). Value `true`.
- `processingMode`: Values: `species` or `resistance_genes`.  
- `alignmentWindow`: Size of the window (in wallclock seconds) in which FastQ records will be collected for alignment. Value `20`. 
- `statisticUpdatingDelay`: How frequently (in wallclock seconds) are statistics updated for dashboard visualization. Value `30`.
- `inputDataSubscription`: Pub/Sub subscription for new data listening
- `outputGcsUri`: A folder in your results bucket where pipeline might store some data.
- `autoStopTopic`: A GCP Pub-sub topic used for pipeline autostop. (A message submitted when no messages during a period of time came to the pipeline).  
- `autoStopDelay`: Time period in seconds after which pipeline will be automatically stopped
- `jobNameLabel`: JobName value provider to access from PTransforms
- `gcpTempLocation`: A GCS location for temporary files.
- `stagingLocation`: A GCS location for staging files.
- `templateLocation`: A GCS location for template location.
- `memoryOutputLimit`: (OPTIONAL) Threshold to decide how to pass data after alignment. Default Value `0` - no limit. 
- `alignmentBatchSize`: (OPTIONAL) Max size of batch that will be generated before alignment. Default value - `2000`.
- `enableStreamingEngine`: Streaming Engine allows you to run the steps of your streaming pipeline in the Dataflow service backend, thus conserving CPU, memory, and Persistent Disk storage resources. (https://cloud.google.com/dataflow/docs/guides/specifying-exec-params).
- `workerMachineType`: The Compute Engine machine type that Dataflow uses when starting worker VMs. You can use any of the available Compute Engine machine type families as well as custom machine types. (https://cloud.google.com/dataflow/docs/guides/specifying-exec-params)
- `diskSizeGb`: The disk size, in gigabytes, to use on each remote Compute Engine worker instance. (https://cloud.google.com/dataflow/docs/guides/specifying-exec-params) 
- `initAutoStopOnlyIfDataPassed`: (OPTIONAL)  Specifies if need to wait for data to init AutoStop timer