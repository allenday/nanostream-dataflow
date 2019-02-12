## Aligner Cluster Provisioning

Scripts in this directory provisions auto-scaled cluster of VMs available through global HTTP load balancer.
Docker container `allenday/bwa-http-docker:http` is installed on each machine.
This provides HTTP interface to `bwa mem` and `kalign` tools.

To provision cluster with example species reference files run:
```
./provision_species.sh
```

To provision cluster with example antibiotic resistance genes reference files run:
```
./provision_resistance_genes.sh
```
