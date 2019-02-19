#!/bin/bash
for i in {1..200}
do
   MESSAGE=i
   gcloud pubsub topics publish manual_test_topic --message="$i"
   sleep 1
done