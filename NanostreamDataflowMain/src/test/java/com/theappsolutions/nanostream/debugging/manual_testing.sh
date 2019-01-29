#!/bin/bash
for i in {1..50000}
do
   MESSAGE=$(( ( RANDOM % 1000 )  + 1 ))
   gcloud pubsub topics publish manual_test_topic --message="$MESSAGE"
   sleep 1
done