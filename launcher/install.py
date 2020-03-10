# -*- coding: utf-8 -*-

import subprocess
import os
import re
from string import Template
import json
from datetime import datetime

TEMPLATES = [
    {'inp': 'config.js.template', 'out': '../NanostreamDataflowMain/webapp/src/main/app-vue-js/src/config.js'},
    {'inp': 'start_function.config.js.template', 'out': './start_function/config.js'},
]


class Install:

    def __init__(self):
        self.google_cloud_project = self.get_google_cloud_env_var()
        self.upload_bucket_name = self.google_cloud_project + '-upload-bucket'
        self.dataflow_bucket_name = self.google_cloud_project + '-dataflow'
        self.reference_db_bucket_name = self.google_cloud_project + '-reference-db'
        self.results_bucket_name = self.google_cloud_project + '-results'
        self.upload_pub_sub_topic = self.google_cloud_project + '-pubsub-topic'
        self.autostop_pub_sub_topic = self.google_cloud_project + '-autostop'
        self.app_engine_region = 'us-central'  # TODO: parametrize

        log("Used names: \n  project: %s\n  upload pubsub topic: %s\n  autostop pubsub topic: %s\n  app engine region: %s\n" % (
                self.google_cloud_project,
                self.upload_pub_sub_topic,
                self.autostop_pub_sub_topic,
                self.app_engine_region
            ))

        # bucket urls
        self.upload_bucket_url = 'gs://%s/' % self.upload_bucket_name
        self.reference_db_bucket_url = 'gs://%s/' % self.reference_db_bucket_name
        self.dataflow_bucket_url = 'gs://%s/' % self.dataflow_bucket_name
        self.results_bucket_url = 'gs://%s/' % self.results_bucket_name

        log("Buckets: \n  uploads bucket: %s\n  reference db bucket: %s\n  dataflow bucket: %s\n  results bucket: %s\n" % (
                self.upload_bucket_url,
                self.reference_db_bucket_url,
                self.dataflow_bucket_url,
                self.results_bucket_url,
            ))

        # reference database config:
        self.reference_name_list = 'DB'  # how reference files are named: DB.fasta -> DB
        self.ref_species_dir = self.reference_db_bucket_url + 'reference-sequences/species/'
        self.ref_genes_dir = self.reference_db_bucket_url + 'reference-sequences/antibiotic-resistance-genes/'
        self.resistance_genes_list = self.reference_db_bucket_url + 'gene_info/resistance_genes_list.txt'

        log("Reference db: \n  reference_name_list: %s\n  ref_species_dir: %s\n  ref_genes_dir: %s\n  resistance_genes_list: %s\n" % (
            self.reference_name_list,
            self.ref_species_dir,
            self.ref_genes_dir,
            self.resistance_genes_list,
        ))

        self.dir_file = os.path.dirname(os.path.realpath(__file__))  # a folder where this script is located
        self._config_data = []  # a placeholder for config data
        self._recreate_bucket_notifications = True

    def main(self):
        self.authenticate_gcloud()
        self.set_default_project_for_gcloud()
        self.enable_apis()
        self.create_storage_buckets()
        self.create_pub_sub_topics()
        self.create_bucket_notifications()
        self.install_required_libs()
        self.deploy_dataflow_templates()
        self.initialize_app_engine_in_project()
        self.initialize_firebase_project()
        self.write_config_files()
        self.deploy_app_engine_management_application()
        self.deploy_start_pipeline_function()
        self.deploy_stop_pipeline_function()

    def get_google_cloud_env_var(self):
        google_cloud_project = self.try_get_google_cloud_project()
        if not google_cloud_project:
            raise IllegalArgumentException('Cannot figure out project name. '
                                           'Please define GOOGLE_CLOUD_PROJECT environment variable')
        return google_cloud_project

    def try_get_google_cloud_project(self):
        var = 'GOOGLE_CLOUD_PROJECT'
        if var in os.environ:
            return os.environ[var].strip()
        else:
            try:
                cmd = 'gcloud config get-value project'
                return subprocess.check_output(cmd, shell=True).strip().decode("utf-8")
            except subprocess.CalledProcessError:
                raise IllegalArgumentException('Please define %s environment variable' % var)

    def authenticate_gcloud(self):
        var = 'GOOGLE_APPLICATION_CREDENTIALS'
        if var in os.environ:
            service_account_credentials_json = os.environ[var].strip()
            if os.path.isfile(service_account_credentials_json):
                self.do_authenticate_gcloud(service_account_credentials_json)
            else:
                log('Service account credentials file not found: %s' % service_account_credentials_json)

    def do_authenticate_gcloud(self, service_account_credentials_json):
        # Authenticate gcloud using "auth/credential_file_override" required because of issue#152
        # See:
        # https://hub.docker.com/r/google/cloud-sdk/
        # https://github.com/GoogleCloudPlatform/cloud-sdk-docker/issues/152#issuecomment-433717308
        cmd = 'gcloud config set auth/credential_file_override %s' % service_account_credentials_json
        log('Authenticate gcloud: %s' % cmd)
        subprocess.check_call(cmd, shell=True)

    def set_default_project_for_gcloud(self):
        cmd = "gcloud config set project %s" % self.google_cloud_project
        log('Set default project for gcloud: %s' % cmd)
        subprocess.check_call(cmd, shell=True)

    def enable_apis(self):
        self.enable_api("monitoring.googleapis.com")
        self.enable_api("dataflow.googleapis.com")
        self.enable_api("firebase.googleapis.com")
        self.enable_api("firestore.googleapis.com")
        self.enable_api("cloudfunctions.googleapis.com")
        self.enable_api("appengine.googleapis.com")

    def enable_api(self, api_name):
        cmd = 'gcloud services enable %s' % api_name
        log('Enable api: %s' % cmd)
        subprocess.check_call(cmd, shell=True)

    def create_storage_buckets(self):
        cmd = 'gsutil ls'
        bucket_list = subprocess.check_output(cmd, shell=True).decode("utf-8")
        self.create_storage_bucket(bucket_list, self.upload_bucket_name, self.upload_bucket_url,
                                   'Create a Google Cloud Storage bucket for FastQ files')
        self.create_storage_bucket(bucket_list, self.reference_db_bucket_name, self.reference_db_bucket_url,
                                   'Create a Google Cloud Storage bucket for reference database files')
        self.create_storage_bucket(bucket_list, self.dataflow_bucket_name, self.dataflow_bucket_url,
                                   'Create a Google Cloud Storage bucket for Dataflow files')
        self.create_storage_bucket(bucket_list, self.results_bucket_name, self.results_bucket_url,
                                   'Create a Google Cloud Storage bucket for output files')

    def create_pub_sub_topics(self):
        self._create_pub_sub_topic(self.autostop_pub_sub_topic)
        self._recreate_bucket_notifications = self._create_pub_sub_topic(self.upload_pub_sub_topic)

    def _create_pub_sub_topic(self, topic_name):
        cmd = 'gcloud pubsub topics list'
        list = subprocess.check_output(cmd, shell=True).decode("utf-8")
        if topic_name in list:
            log('PubSub topic already already exists: %s' % list)
            create_new = False
        else:
            cmd = 'gcloud pubsub topics create %s' % topic_name
            log('Create a PubSub topic: %s' % cmd)
            subprocess.check_call(cmd, shell=True)
            create_new = True
        return create_new

    def create_storage_bucket(self, bucket_list, bucket_name, bucket_url, message):
        if bucket_url in bucket_list:
            log('Bucket %s already exists' % bucket_name)
        else:
            cmd = 'gsutil mb %s' % bucket_url
            log((message + ': %s') % cmd)
            subprocess.check_call(cmd, shell=True)

    def create_bucket_notifications(self):
        handler = BucketNotificationHandler(self.upload_pub_sub_topic, self.upload_bucket_url, self._recreate_bucket_notifications)
        handler.create_bucket_notifications()

    def deploy_start_pipeline_function(self):
        cmd = 'gcloud functions deploy run_dataflow_job \
                --no-allow-unauthenticated \
                --runtime nodejs10 \
                --trigger-event google.storage.object.finalize \
                --trigger-resource %s' % self.upload_bucket_name
        log('Deploy start pipeline function: %s' % cmd)
        wd = os.getcwd()
        os.chdir(self.dir_file + "/start_function")
        subprocess.check_call(cmd, shell=True)
        os.chdir(wd)

    def deploy_stop_pipeline_function(self):
        cmd = 'gcloud functions deploy stop_dataflow_job \
                --no-allow-unauthenticated \
                --runtime python37 \
                --trigger-topic %s' % self.autostop_pub_sub_topic
        log('Deploy stop pipeline function: %s' % cmd)
        wd = os.getcwd()
        os.chdir(self.dir_file + "/stop_function")
        subprocess.check_call(cmd, shell=True)
        os.chdir(wd)

    def install_required_libs(self):
        cmd = 'mvn install:install-file -Dfile=NanostreamDataflowMain/libs/japsa.jar -DgroupId=coin -DartifactId=japsa -Dversion=1.9-3c -Dpackaging=jar'
        log('Add japsa dependency: %s' % cmd)
        subprocess.check_call(cmd, shell=True)

        cmd = 'mvn install:install-file -Dfile=NanostreamDataflowMain/libs/pal1.5.1.1.jar -DgroupId=nz.ac.auckland -DartifactId=pal -Dversion=1.5.1.1 -Dpackaging=jar'
        log('Add pal dependency: %s' % cmd)
        subprocess.check_call(cmd, shell=True)

    def deploy_dataflow_templates(self):
        self.deploy_dataflow_template('species', self.ref_species_dir)
        self.deploy_dataflow_template('resistance_genes', self.ref_genes_dir)

    def deploy_dataflow_template(self, processing_mode, all_references_dir_gcs_uri):
        template_name = 'nanostream-' + processing_mode
        alignment_window = 20
        stats_update_frequency = 30

        memory_output_limit = 0

        if processing_mode == 'resistance_genes':
            resistance_genes_list_param = '--resistanceGenesList=%s ' % self.resistance_genes_list
            machine_config_params = ' '
        else:
            resistance_genes_list_param = ' '  # not required for other modes
            machine_config_params = '--enableStreamingEngine ' \
                                    '--workerMachineType=n1-highmem-8 ' \
                                    '--diskSizeGb=100 '

        output_gcs_uri = self.results_bucket_url + 'clinic_processing_output/'

        autostop_pub_sub_topic_full_path = 'projects/%s/topics/%s' % (self.google_cloud_project, self.autostop_pub_sub_topic)

        cmd = 'mvn compile exec:java ' \
              '-f NanostreamDataflowMain/pipeline/pom.xml ' \
              '-Dexec.mainClass=com.google.allenday.nanostream.NanostreamApp ' \
              '-Dexec.args="' \
              '--project=%s ' \
              '--runner=DataflowRunner ' \
              '--streaming=true ' \
              '--processingMode=%s ' \
              '--alignmentWindow=%s ' \
              '--statisticUpdatingDelay=%s ' \
              '%s ' \
              '--outputGcsUri=%s ' \
              '--autoStopTopic=%s ' \
              '--gcpTempLocation=%s ' \
              '--stagingLocation=%s ' \
              '--templateLocation=%s ' \
              '--memoryOutputLimit=%s ' \
              '%s ' \
              '" ' \
              '-Dexec.cleanupDaemonThreads=false' \
              % (
                  self.google_cloud_project,
                  processing_mode,
                  alignment_window,
                  stats_update_frequency,
                  resistance_genes_list_param,
                  output_gcs_uri,
                  autostop_pub_sub_topic_full_path,
                  self.dataflow_bucket_url + 'tmp',
                  self.dataflow_bucket_url + 'staging',
                  self.dataflow_bucket_url + 'templates/' + template_name,
                  memory_output_limit,
                  machine_config_params
              )
        log('Compile and deploy Dataflow template for processing mode %s: %s' % (processing_mode, cmd))
        subprocess.check_call(cmd, shell=True)

    def initialize_app_engine_in_project(self):
        cmd = 'gcloud app describe'
        try:
            subprocess.check_output(cmd, stderr=subprocess.STDOUT, shell=True).decode("utf-8")
            # Do nothing here. App Engine already initialized within current project
        except subprocess.CalledProcessError:
            cmd = 'gcloud app create --region=%s' % self.app_engine_region
            log('Initialize an App Engine application within the project: %s' % cmd)
            subprocess.check_output(cmd, shell=True).decode("utf-8")

    def initialize_firebase_project(self):
        handler = FirebaseHandler(self.google_cloud_project)
        handler.add_firebase_to_project()
        self._config_data = handler.prepare_firebase_config_data()

    def write_config_files(self):
        self._config_data['uploadBucketName'] = self.upload_bucket_name
        self._config_data['referenceNamesList'] = self.reference_name_list
        self._config_data['uploadPubSubTopic'] = self.upload_pub_sub_topic
        handler = ConfigHandler(self._config_data, self.dir_file)
        handler.write_configs()

    def deploy_app_engine_management_application(self):
        #     cmd = 'mvn clean package appengine:deploy \
        #             -DskipTests=true \
        #             -DcloudSdkHome=/usr/lib/google-cloud-sdk/ \
        #             -f NanostreamDataflowMain/webapp/pom.xml'
        #     log('Compile and deploy App Engine management application: %s' % cmd)
        #     subprocess.check_call(cmd, shell=True)

        cmd = 'mvn clean package -DskipTests=true -f NanostreamDataflowMain/webapp/pom.xml'
        log('Compile App Engine management application: %s' % cmd)
        subprocess.check_call(cmd, shell=True)

        cmd = 'mvn appengine:stage -DcloudSdkHome=/usr/lib/google-cloud-sdk/ -f NanostreamDataflowMain/webapp/pom.xml'
        log('Stage App Engine management application: %s' % cmd)
        subprocess.check_call(cmd, shell=True)

        # This step required because appengine maven plugin for unknown reason cleans gcloud project settings
        self.set_default_project_for_gcloud()

        cmd = 'gcloud --quiet app deploy NanostreamDataflowMain/webapp/target/appengine-staging/app.yaml '
        log('Deploy App Engine management application: %s' % cmd)
        subprocess.check_call(cmd, shell=True)


class BucketNotificationHandler:

    def __init__(self, upload_pub_sub_topic, upload_bucket_url, recreate_bucket_notifications):
        self.upload_pub_sub_topic = upload_pub_sub_topic
        self.upload_bucket_url = upload_bucket_url
        self.recreate_bucket_notifications = recreate_bucket_notifications;

    def create_bucket_notifications(self):
        cmd = 'gsutil notifications list %s' % self.upload_bucket_url
        notifications = subprocess.check_output(cmd, shell=True).decode("utf-8")
        if self.upload_pub_sub_topic in notifications:
            log('Bucket notification already exists: %s' % notifications)
            if self.recreate_bucket_notifications:
                log('Recreate bucket notification for bucket: %s' % self.upload_bucket_url)
                self._delete_bucket_upload_notifications()
                self._create_bucket_upload_notifications()
        else:
            self._create_bucket_upload_notifications()

    def _delete_bucket_upload_notifications(self):
        cmd = 'gsutil notification delete %s' % self.upload_bucket_url
        log('Delete bucket notification: %s' % cmd)
        subprocess.check_call(cmd, shell=True)

    def _create_bucket_upload_notifications(self):
        cmd = 'gsutil notification create -t %s -f json -e OBJECT_FINALIZE %s' % (
            self.upload_pub_sub_topic, self.upload_bucket_url)
        log('Create bucket notification: %s' % cmd)
        subprocess.check_call(cmd, shell=True)


class FirebaseHandler:

    def __init__(self, google_cloud_project):
        self.google_cloud_project = google_cloud_project

    # TODO: deploy security rules like (https://firebase.google.com/docs/firestore/security/get-started):
    # rules_version = '2';
    # service cloud.firestore {
    #   match /databases/{database}/documents {
    #     match /{document=**} {
    #       allow read: if true;
    #     }
    #   }
    # }

    def add_firebase_to_project(self):
        cmd = 'firebase projects:addfirebase %s' % self.google_cloud_project
        log('Trying add firebase to the project: %s' % cmd)
        try:
            subprocess.check_output(cmd, stderr=subprocess.STDOUT, shell=True).strip().decode("utf-8")
        except subprocess.CalledProcessError:
            log("Firebase already added to the project ")

    def prepare_firebase_config_data(self):
        web_app_id = self._get_or_create_firebase_web_app()
        cmd = 'firebase apps:sdkconfig WEB %s' % web_app_id
        log('Getting config data from firebase webapp: %s' % cmd)
        out = subprocess.check_output(cmd, shell=True).strip().decode("utf-8")
        log(out)
        return self._parse_config_data(out)

    def _get_or_create_firebase_web_app(self):
        web_app_id = self._try_get_id_from_app_list()
        log('Found web app id: %s' % web_app_id)
        if not web_app_id:
            web_app_id = self._try_create_web_app()
        return web_app_id

    def _try_get_id_from_app_list(self):
        cmd = 'firebase apps:list --project %s' % self.google_cloud_project
        app_list = subprocess.check_output(cmd, shell=True).strip().decode("utf-8")
        log('Application list: \n%s' % app_list)
        expected_app_name = 'web_%s' % self.google_cloud_project
        # │ web_nanostream-test1 │ 1:253229025431:web:e39391f630f6c68ba981d2     │ WEB      │
        pattern = '^[\s\│]+%s[\s\│]+(.+?)[\s\│]+WEB[\s\│]+$' % expected_app_name
        compile = re.compile(pattern, re.M)
        m = compile.search(app_list)
        if m:
            id = m.group(1)
            return id
        else:
            return None

    def _try_create_web_app(self):
        # Call firebase apps:create command to create firebase web application id
        cmd = 'firebase apps:create Web web_%s --project %s' % (self.google_cloud_project, self.google_cloud_project)

        # Call firebase apps:sdkconfig command to get apikey and other parameters
        log('create firebase app for web or get info: %s' % cmd)

        out = subprocess.check_output(cmd, shell=True).strip().decode("utf-8")

        # extract id from output: firebase apps:sdkconfig WEB 1:22222222222222222222222222222
        return re.sub(r'^.*firebase apps:sdkconfig WEB (.*)', r'\1', out, 0, re.S)

    def _parse_config_data(self, text):
        text = self._cleanup_sdkconfig_output(text)
        return json.loads(text)

    def _cleanup_sdkconfig_output(self, text):
        return re.sub(r'^.*firebase.initializeApp\(({.+?})\);.*', r'\1', text, 0, re.S)


class ConfigHandler:

    def __init__(self, config_data, dir_file):
        self.dir_file = dir_file
        self._config_data = config_data

    def write_configs(self):
        for template in TEMPLATES:
            self._write_config(template['inp'], template['out'])

    def _write_config(self, template_filename, output_filename):
        config_data_prepared = self._process_template(template_filename, self._config_data)
        log("Prepared config: %s" % config_data_prepared)
        self._write_config_file(config_data_prepared, output_filename)

    def _process_template(self, filename, parsed):
        with open(self.dir_file + '/' + filename, 'r') as file:
            data = file.read()
        t = Template(data)
        return t.substitute(**parsed)

    def _write_config_file(self, text, filename):
        filename = self.dir_file + '/' + filename
        log('Writing firebase config file: %s' % filename)
        with open(filename, 'w') as file:
            file.write(text)


class IllegalArgumentException(Exception):
    def __init__(self, msg, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.msg = msg


def log(text):
    dt = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print ("%s: %s" % (dt, text))


if __name__ == "__main__":
    try:
        app = Install()
        app.main()
    except IllegalArgumentException as e:
        log(e.msg)