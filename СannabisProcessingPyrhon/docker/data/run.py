import subprocess
import logging
from datetime import datetime
import pysam

logging.getLogger().setLevel(logging.INFO)

reference_path = 'AGQN03/AGQN03.fa'
files = ['SRS1757988/SRR4448249_1.fastq', 'SRS1757988/SRR4448249_2.fastq']
aligned_sam_path = 'aligned.sam'

command_pattern = "/minimap2-2.17_x64-linux/minimap2 -ax sr {REFERENCE_PATH} {SRC_FILES} -R '@RG\tID:RSP11055\tPL:ILLUMINA\tPU:NONE\tSM:RSP11055' > {ALIGNED_SAM_PATH}"

command = command_pattern.format(REFERENCE_PATH=reference_path, SRC_FILES=" ".join(files), ALIGNED_SAM_PATH=aligned_sam_path)

start = datetime.now()
logging.info("Command start: {}".format(command))
subprocess.call(command, shell=True)
delta = datetime.now() - start
logging.info("Command finish in {}: {}".format(delta, command))

aligned_sam = pysam.AlignmentFile(aligned_sam_path, "rb")
aligned_bam_path = 'aligned.bam'
aligned_sorted_bam_path = 'aligned.sorted.bam'
bam_content = pysam.view('-bh', aligned_sam_path)

with open(aligned_bam_path, 'wb') as file:
    file.write(bam_content)

pysam.sort('-o', aligned_sorted_bam_path, aligned_bam_path)




