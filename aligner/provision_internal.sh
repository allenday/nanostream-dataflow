#!/bin/bash
set -e

setup () {
    # create instance template
    gcloud compute instance-templates \
    create-with-container ${NAME}-template \
    --container-image=${DOCKER_IMAGE} \
    --container-env BWA_FILES=${BWA_FILES},REQUESTER_PROJECT=${REQUESTER_PROJECT} \
    --boot-disk-size=250GB \
    --tags ${NAME}-http-hc \
    --preemptible \
    --machine-type=$MACHINE_TYPE

    # create managed instance group
    gcloud compute instance-groups managed \
    create ${NAME}-managed-instance-group \
    --base-instance-name ${NAME} \
    --size $MIN_REPLICAS \
    --template ${NAME}-template \
    --zone $ZONE

    # create HTTP health check
    gcloud compute health-checks \
    create http ${NAME}-health-check \
    --request-path /cgi-bin/bwa.cgi

    # configure named ports
    gcloud compute instance-groups managed \
    set-named-ports ${NAME}-managed-instance-group \
    --named-ports http:80 \
    --zone $ZONE

    # configure managed instance group auto-scaling
    gcloud compute instance-groups managed \
    set-autoscaling ${NAME}-managed-instance-group \
    --max-num-replicas $MAX_REPLICAS \
    --min-num-replicas $MIN_REPLICAS \
    --target-cpu-utilization $TARGET_CPU_UTILIZATION \
    --cool-down-period 120 \
    --zone $ZONE

    # configure managed instance group auto-healing
    gcloud beta compute instance-groups managed \
    set-autohealing ${NAME}-managed-instance-group \
    --health-check ${NAME}-health-check \
    --initial-delay 180 \
    --zone $ZONE

    # create load balancer backend service
    gcloud compute backend-services \
    create ${NAME}-backend-service \
    --load-balancing-scheme internal \
    --region $REGION \
    --health-checks ${NAME}-health-check \
    --protocol tcp

    # configure load balancer backend service
    gcloud compute backend-services \
    add-backend ${NAME}-backend-service \
    --instance-group ${NAME}-managed-instance-group \
    --instance-group-zone $ZONE \
    --region $REGION

    # configure load balancer frontend
    gcloud compute forwarding-rules \
    create ${NAME}-forward \
    --load-balancing-scheme internal \
    --ports 80 \
    --region $REGION \
    --backend-service ${NAME}-backend-service

    # create firewall rule to allow health-check requests
    gcloud compute firewall-rules \
    create ${NAME}-allow-http-from-hc \
    --allow tcp:80 \
    --source-ranges 130.211.0.0/22,35.191.0.0/16 \
    --target-tags ${NAME}-http-hc

    export ALIGNER_CLUSTER_IP_ADDRESS=$(gcloud compute forwarding-rules describe ${NAME}-forward --region=${REGION} --format="value(IPAddress)")
    echo "All done. Cluster will be available on internal address ${ALIGNER_CLUSTER_IP_ADDRESS} in 5-10 minutes"
}

cleanup () {
    # delete forwarding rule
    gcloud compute forwarding-rules delete ${NAME}-forward \
     --region=$REGION --quiet

    # delete backend service
    gcloud compute backend-services delete ${NAME}-backend-service \
    --region=$REGION --quiet

    # delete managed instance group
    gcloud compute instance-groups managed \
    delete ${NAME}-managed-instance-group --zone $ZONE --quiet

    # delete instance template
    gcloud compute instance-templates delete ${NAME}-template --quiet

    # delete HTTP health check
    gcloud compute health-checks delete ${NAME}-health-check --quiet

    # delete firewall rule, ignore failure
    gcloud compute firewall-rules delete ${NAME}-allow-http-from-hc --quiet
}