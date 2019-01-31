# Nanostream Dataflow

### Diriectory Structure
- aligner - scripts to provision autoscaled HTTP service for alignment (based on bwa)
- simulator - python script that can simulate file uploads to GCS

### Running

To run all Nanostream system you shuld make next steps:
1) Create [Google Cloud Project](https://cloud.google.com/) and enable required APIs (Dataflow, Fire)
2) Create two [Google Cloud Storage](https://cloud.google.com/storage/) **buckets**
- a) **main bucket** for all working files
- b) **simulator dest bucket** for fastq adding simulation
3) Put *source fastq files* and *tsv file with fastq files pathes and timings* into the **main bucket**
4) Start fastq adding **simulator** ([See details](https://github.com/Pseverin/nanostream-dataflow/blob/master/simulator/README.md))
5) Create **PubSub notifications**  ([See details](https://cloud.google.com/storage/docs/pubsub-notifications)) for **simulator dest bucket** that will be creating notifications when new files will have be added to bucket
```
gsutil notification create -t (pub_sub_topic_name) -f json -e OBJECT_FINALIZE (adding_fastq_files_simulation_bucket)
```
6) Create **PubSub subscription** for topic created at Step 5
7) Create **Firestore DB** ([See details](https://firebase.google.com/products/firestore/)) for saving cache and result data
8) Open Terminal and Start **Nanostream Pipeline** from root project directory with following command:
```
java -cp NanostreamDataflowMain/build/NanostreamDataflowMain.jar \
  com.theappsolutions.nanostream.NanostreamApp \
  --runner=org.apache.beam.runners.dataflow.DataflowRunner `# Apache Beam Runner (Dataflow for Google Cloud Dataflow running or Direct for local running)` \
  --project=upwork-nano-stream `# Google Cloud Project name` \
  --streaming=true `# should be true for streaming (infinite) mode` \
  --processingMode=resistant_genes `# pecifies "species" or "resistant_genes" mode of data processing` \
  --inputDataSubscription=projects/upwork-nano-stream/subscriptions/resistant_fastq_paths_emitter_x1_subscription_1 `# PubSub subscription name from step 6` \
  --alignmentWindow=20 `# Size of the window in which FastQ records will be collected for Alignment` \
  --statisticUpdatingDelay=30 `# Delay between updating output statistic data` \
  --servicesUrl=http://130.211.33.64 `# Base URL for http services (Aligner and K-Align)` \
  --bwaEndpoint=/cgi-bin/bwa.cgi `# Aligner endpoint` \
  --bwaDatabase=DB.fasta `# Aligner DB name` \
  --kAlignEndpoint=/cgi-bin/kalign.cgi `# K-Align endpoint` \
  --outputFirestoreDbUrl=https://upwork-nano-stream.firebaseio.com `# Firestore DB url from step 7` \
  --outputFirestoreSequencesStatisticCollection=resistant_sequences_statistic `# Collection name of the Firestore database that will be used for writing output statistic data` \
  --outputFirestoreSequencesBodiesCollection=resistant_sequences_bodies `# Collection name of the Firestore database that will be used for writing output Sequences Body data` \
  --outputFirestoreGeneCacheCollection=resistant_gene_cache `# Collection name of the Firestore database that will be used for saving NCBI genome data cache` \
  --workingBucket=nano-stream-test `# Name of GCS bucket that used for storing project data (step 2.a)` \
  --resistantGenesFastDB=gs://nano-stream-test/gene_info/DB_resistant_formatted.fasta `# OPTOPNAL Only for resistant_genes mode. Path to fasta file with resistant genes database` \
  --resistantGenesList=gs://nano-stream-test/gene_info/resistant_genes_list.txt `# OPTOPNAL Only for resistant_genes mode. Path to fasta file with resistant genes list` 
```


TODO:
- simulation of fastq files (Done)
- change allenday/bwa-http-docker to listen on port 80 without using self-signed certificate (Done)
- load balancer and autoscaling group for aligner (Done)
- wrap [minimap2](https://github.com/lh3/minimap2) into HTTP. (Not clear, how to output SAM file)
