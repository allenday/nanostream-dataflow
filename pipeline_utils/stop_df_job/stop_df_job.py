import logging
import googleapiclient.discovery

from oauth2client.client import GoogleCredentials

REQUESTED_STATE_TUPLE = "requestedState", "JOB_STATE_DRAINING"


def main(event, context):
    """Triggered from a message on a Cloud Pub/Sub topic.
    Args:
         event (dict): Event payload.
         context (google.cloud.functions.Context): Metadata for the event.
    """
    logging.getLogger().setLevel(logging.INFO)

    logging.info(event)
    data = event['attributes']

    project_id = data['project_id']
    job_name = data['job_name']

    credentials = GoogleCredentials.get_application_default()
    dataflow = googleapiclient.discovery.build('dataflow', 'v1b3', credentials=credentials, cache_discovery=False)

    response = dataflow.projects().jobs().list(projectId=project_id, filter="ACTIVE").execute()

    if "jobs" in response:
        results = [v for k, v in enumerate(response["jobs"]) if job_name in v["name"]]

        for job_data in results:
            if REQUESTED_STATE_TUPLE[0] in job_data and job_data[REQUESTED_STATE_TUPLE[0]] == REQUESTED_STATE_TUPLE[1]:
                continue
            logging.info("Found job: {}".format(job_data))
            job_id = job_data["id"]
            drain_body = dict([REQUESTED_STATE_TUPLE])
            response = dataflow.projects().jobs().update(projectId=project_id, jobId=job_id, body=drain_body).execute()
            logging.info("Draining for dataflow job with id={}".format(job_id))
