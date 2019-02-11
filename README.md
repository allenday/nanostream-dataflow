# Nanostream Dataflow

[Visualiser](https://nano-stream1.appspot.com/)

[PubSub Diagnostics](https://upload-watcher-dot-nano-stream1.appspot.com/)

### Project Structure
- NanostreamDataflowMain - Apache Beam app that provides all data transformations
- aligner - scripts to provision auto-scaled HTTP service for alignment (based on `bwa`)
- simulator - python script that can simulate file uploads to GCS
- fasta_formatter - python script for formatting fasta files into project readable format
- visualization - module for the visualization of results
- doc - additional files for documentation

### Setup
To run all Nanostream system you should make next steps:
1) Create [Google Cloud Project](https://cloud.google.com/)
2) Create [Google Cloud Storage](https://cloud.google.com/storage/) **destination bucket** for adding fastq files.
You can use ([this python module](https://github.com/allenday/nanostream-dataflow/blob/master/simulator)) to provide a simulation of an adding fastq files
3) Create **PubSub notifications**  ([See details](https://cloud.google.com/storage/docs/pubsub-notifications)) for **simulator dest bucket** that will be creating notifications when new files will have be added to bucket
```
gsutil notification create \
-t <subscription name> -f json \
-e OBJECT_FINALIZE \
-p <subfolder path> gs://<bucket name>
```

<details><summary>CGI</summary><p>
```
gsutil notification create \
-t file_upload -f json \
-e OBJECT_FINALIZE \
-p Uploads/ gs://nano-stream1
```
</p></details>


5) Create **Firestore DB** ([See details](https://firebase.google.com/products/firestore/)) for saving cache and result data

Run provisioning scripts:

6) On Google Cloud Shell, `git clone https://github.com/Firedrops/nanostream-dataflow.git`.
7) `cd /nanostream-dataflow/aligner/`
8) `bash provision_species.sh` or `bash provision_gene_resistance.sh`

9) install java8 on Gcloud
[instructions here](https://tecadmin.net/install-java-8-on-debian/)

Optional:

9) If you running the pipeline in *resistant_genes* mode you should provide *fasta db* and *gene list* files stored at the GCS bucket

### Build
You can skip this step and run project with [pre-built jar file](https://github.com/allenday/nanostream-dataflow/blob/master/NanostreamDataflowMain/build/)
To build jar from source, follow next steps:
1) Install [Maven](https://maven.apache.org/install.html)
2) Add [Japsa](https://github.com/mdcao/japsa) package to local Maven repository. To do this you should run following command from project root:
```
mvn install:install-file -Dfile=NanostreamDataflowMain/libs/japsa.jar -DgroupId=coin -DartifactId=japsa -Dversion=1.7-10a -Dpackaging=jar
```
3) Build uber-jar file
```
cd NanostreamDataflowMain
mvn clean package
```
after running this command successfully, there should be a file:
"NanostreamDataflowMain/target/NanostreamDataflowMain-1.0-SNAPSHOT.jar"

### Running
To start **Nanostream Pipeline** run following command:
```
java -cp (path_to_nanostream_app_jar) \
  com.theappsolutions.nanostream.NanostreamApp \
  --region=asia-northeast1 \
  --zone=c \
  --runner=org.apache.beam.runners.dataflow.DataflowRunner `# Apache Beam Runner (Dataflow for Google Cloud Dataflow running or Direct for local running)` \
  --project=nano-stream1 `# Google Cloud Project name` \
  --streaming=true `# should be true for streaming (infinite) mode` \
  --processingMode=species `# specifies "species" or "resistant_genes" mode of data processing` \
  --inputDataSubscription=projects/nano-stream1/subscriptions/dataflow `# PubSub subscription name from step 4` \
  --alignmentWindow=20 `# Size of the window in which FastQ records will be collected for Alignment` \
  --statisticUpdatingDelay=30 `# Delay between updating output statistic data` \
  --servicesUrl=http://34.85.27.91 `# Base URL for http services (Aligner and K-Align)` TODOTODO \
  --bwaEndpoint=/cgi-bin/bwa.cgi `# Aligner endpoint` \
  --bwaDatabase=DB.fasta `# Aligner DB name` \
  --kAlignEndpoint=/cgi-bin/kalign.cgi `# K-Align endpoint` \
  --outputFirestoreDbUrl=https://nano-stream1.firebaseio.com `# Firestore DB url from step 5` \
  --outputFirestoreSequencesStatisticCollection=resistant_sequences_statistic `# Collection name of the Firestore database that will be used for writing output statistic data` \
  --outputFirestoreSequencesBodiesCollection=resistant_sequences_bodies `# Collection name of the Firestore database that will be used for writing output Sequences Body data` \
  --outputFirestoreGeneCacheCollection=resistant_gene_cache `# Collection name of the Firestore database that will be used for saving NCBI genome data cache` \
  --resistantGenesFastDB=gs://nano-stream-test/gene_info/DB_resistant_formatted.fasta `# OPTIONAL Only for resistant_genes mode. Path to fasta file with resistant genes database (step 6)` \
  --resistantGenesList=gs://nano-stream-test/gene_info/resistant_genes_list.txt `# OPTIPNAL Only for resistant_genes mode. Path to fasta file with resistant genes list(step 6)`
```

<details><summary>species</summary><p>

```
java -cp /home/coingroupimb/git_larry_2019-02-08/NanostreamDataflowMain/build/NanostreamDataflowMain.jar \
  com.theappsolutions.nanostream.NanostreamApp \
  --region=asia-northeast1 \
  --runner=org.apache.beam.runners.dataflow.DataflowRunner \
  --project=nano-stream1 \
  --streaming=true \
  --processingMode=species \
  --inputDataSubscription=projects/nano-stream1/subscriptions/dataflow \
  --alignmentWindow=20 \
  --statisticUpdatingDelay=30 \
  --servicesUrl=http://34.85.27.91 \
  --bwaEndpoint=/cgi-bin/bwa.cgi \
  --bwaDatabase=DB.fasta \
  --kAlignEndpoint=/cgi-bin/kalign.cgi \
  --outputFirestoreDbUrl=https://nano-stream1.firebaseio.com \
  --outputFirestoreSequencesStatisticCollection=resistant_sequences_statistic \
  --outputFirestoreSequencesBodiesCollection=resistant_sequences_bodies \
  --outputFirestoreGeneCacheCollection=resistant_gene_cache \
```

</p></details>

<details><summary>resistance</summary><p>

```
java -cp /home/coingroupimb/git_larry_2019-02-08/NanostreamDataflowMain/build/NanostreamDataflowMain.jar \
  com.theappsolutions.nanostream.NanostreamApp \
  --region=asia-northeast1 \
  --runner=org.apache.beam.runners.dataflow.DataflowRunner \
  --project=nano-stream1 \
  --streaming=true \
  --processingMode=species \
  --inputDataSubscription=projects/nano-stream1/subscriptions/dataflow \
  --alignmentWindow=20 \
  --statisticUpdatingDelay=30 \
  --servicesUrl=http://34.85.27.91 \
  --bwaEndpoint=/cgi-bin/bwa.cgi \
  --bwaDatabase=DB.fasta \
  --kAlignEndpoint=/cgi-bin/kalign.cgi \
  --outputFirestoreDbUrl=https://nano-stream1.firebaseio.com \
  --outputFirestoreSequencesStatisticCollection=resistant_sequences_statistic \
  --outputFirestoreSequencesBodiesCollection=resistant_sequences_bodies \
  --outputFirestoreGeneCacheCollection=resistant_gene_cache \
  --resistantGenesFastDB=gs://nano-stream-1/NewDatabases/DB_resistant_formatted.fasta \
  --resistantGenesList=gs://nano-stream1/NewDatabases/resistant_genes_list.txt
```

</p></details>


### Available databases
For this project the bucket **nano-stream-data** were created
with reference databases of species and antibiotic resistant genes.

There is following structure of files:
```
gs://nano-stream-data/
|- databases
|-- antibiotic_resistant
|--- DB.fasta
|--- DB.fasta.[amb,ann,bwt,pac,sa]
|-- species
|--- DB.fasta
|--- DB.fasta.[amb,ann,bwt,pac,sa]
```
where:
- DB.fasta - FASTA file with reference sequences
- DB.fasta.amb, DB.fasta.ann, DB.fasta.bwt, DB.fasta.pac, DB.fasta.sa - files generated and used by `bwa` in order to improve performance, see details in [this SEQanswers answer](http://seqanswers.com/forums/showpost.php?s=06f0dadc73bdf687f265a94c8217d0bd&p=90992&postcount=2)

**nano-stream-data** - is a public bucket with [requester pays](https://cloud.google.com/storage/docs/requester-pays) option enabled.

### Cleanup

To clear previously-generated services and remnants, 
See steps 6 - 8 above, except `bash provision_species.sh -c` or `bash provision_gene_resistance.sh -c`

### TODO:
1) docker build with recipe
1) mnt gs bucket references instead of cp
2) minimap2 pipeline
3) sessioning?
