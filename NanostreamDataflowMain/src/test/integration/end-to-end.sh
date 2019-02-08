#!/usr/bin/env bash
# fail on error
set -e

mvn clean install exec:java \
  -f ../../../pom.xml \
  -Dexec.mainClass="com.theappsolutions.nanostream.NanostreamApp" \
  -Dmaven.test.skip=true \
  -Dexec.args=" \
  --runner=org.apache.beam.runners.dataflow.DataflowRunner `#Apache Beam Runner (Dataflow for Google Cloud Dataflow running or Direct for local running)` \
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
  --resistantGenesFastDB=gs://nano-stream-test/gene_info/DB_resistant_formatted.fasta `# OPTOPNAL Only for resistant_genes mode. Path to fasta file with resistant genes database` \
  --resistantGenesList=gs://nano-stream-test/gene_info/resistant_genes_list.txt" `# OPTOPNAL Only for resistant_genes mode. Path to fasta file with resistant genes list`
