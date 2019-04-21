#!/bin/bash
set -e

setup () {
    # create instance template
    gcloud compute instance-templates \
    create-with-container ${NAME}-template \
    --container-image=${DOCKER_IMAGE} \
    --container-env BWA_FILES=${BWA_FILES},REQUESTER_PROJECT=${REQUESTER_PROJECT} \
    --boot-disk-size=100GB \
<<<<<<< HEAD:aligner/provision.sh
    --tags http-server,http,allow-http \
=======
    --tags ${NAME}-http \
>>>>>>> cbfe24c5a164374bbb2014272ef10cc60acf6b75:aligner/provision_global.sh
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
    gcloud compute http-health-checks \
    create ${NAME}-health-check \
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
    --http-health-check ${NAME}-health-check \
    --initial-delay 180 \
    --zone $ZONE

    # create load balancer backend service
    gcloud compute backend-services \
    create ${NAME}-backend-service \
    --http-health-checks ${NAME}-health-check \
    --global \
    --timeout=600

    # configure load balancer backend service
    gcloud compute backend-services \
    add-backend ${NAME}-backend-service \
    --instance-group ${NAME}-managed-instance-group \
    --balancing-mode UTILIZATION \
    --max-utilization $TARGET_CPU_UTILIZATION \
    --instance-group-zone $ZONE \
    --global

    # configure load balancer URL-maps
    gcloud compute url-maps \
    create ${NAME}-url-map \
    --default-service ${NAME}-backend-service

    # create load balancer frontend
    gcloud compute target-http-proxies \
    create ${NAME}-target-proxy \
    --url-map ${NAME}-url-map

    # configure load balancer frontend
    gcloud compute forwarding-rules \
    create ${NAME}-forward \
    --global \
    --ports 80 \
    --target-http-proxy ${NAME}-target-proxy

    # create firewall rule
    gcloud compute firewall-rules \
    create ${NAME}-allow-http \
    --allow tcp:80 \
    --target-tags ${NAME}-http

    export ALIGNER_CLUSTER_IP_ADDRESS=$(gcloud compute forwarding-rules describe ${NAME}-forward --global --format="value(IPAddress)")
    echo "All done. Cluster will be available on http://${ALIGNER_CLUSTER_IP_ADDRESS}/ in 5-10 minutes"
}

cleanup () {
    # delete forwarding rule
    yes | gcloud compute forwarding-rules delete ${NAME}-forward --global

    # delete target proxy
    yes | gcloud compute target-http-proxies delete ${NAME}-target-proxy

    # delete url-map
    yes | gcloud compute url-maps delete ${NAME}-url-map

    # delete backend service
    yes | gcloud compute backend-services delete ${NAME}-backend-service --global

    # delete managed instance group
    yes | gcloud compute instance-groups managed \
    delete ${NAME}-managed-instance-group --zone $ZONE

    # delete instance template
    yes | gcloud compute instance-templates delete ${NAME}-template

    # delete HTTP health check
    yes | gcloud compute http-health-checks delete ${NAME}-health-check

    # delete firewall rule
<<<<<<< HEAD:aligner/provision.sh
    yes | gcloud compute firewall-rules delete allow-http
}
=======
    gcloud compute firewall-rules delete ${NAME}-allow-http
}
>>>>>>> cbfe24c5a164374bbb2014272ef10cc60acf6b75:aligner/provision_global.sh
