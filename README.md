# Nanostream Dataflow

### Diriectory Structure
- aligner - scripts to provision autoscaled HTTP service for alignment (based on bwa)
- simulator - python script that can simulate file uploads to GCS


TODO:
- simulation of fastq files (Done)
- change allenday/bwa-http-docker to listen on port 80 without using self-signed certificate (Done)
- load balancer and autoscaling group for aligner (Done)
- wrap [minimap2](https://github.com/lh3/minimap2) into HTTP. (Not clear, how to output SAM file)