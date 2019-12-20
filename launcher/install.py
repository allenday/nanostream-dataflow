# -*- coding: utf-8 -*-

import subprocess
import os
import re
from string import Template
import json

FIREBASE_CONFIG_TEMPLATE_FILENAME = 'firebase.config.js.template'
FIREBASE_CONFIG_FILENAME = '../NanostreamDataflowMain/webapp/src/main/app-vue-js/src/firebase.config.js'


class Install:

    def __init__(self):
        self.google_cloud_project = self.get_google_cloud_env_var()
        self.upload_bucket_name = self.google_cloud_project + '-upload-bucket'
        self.dataflow_bucket_name = self.google_cloud_project + '-dataflow'
        self.upload_pub_sub_topic = self.google_cloud_project + '-pubsub-topic'
        self.upload_subscription = self.google_cloud_project + '-upload-subscription'
        self.app_engine_region = 'us-central'  # TODO: parametrize

        self.upload_bucket_url = 'gs://%s/' % self.upload_bucket_name
        self.dataflow_bucket_url = 'gs://%s/' % self.dataflow_bucket_name

        self.upload_subscription_fullname = 'projects/%s/subscriptions/%s' % (
            self.google_cloud_project,
            self.upload_subscription
        )
        self.firebase_handler = FirebaseHandler(self.google_cloud_project)

        print "Used names: \n  project: %s\n  uploads bucket: %s\n  dataflow bucket: %s\n" \
              "  pubsub topic: %s\n  subscription: %s\n  app engine region: %s" % (
            self.google_cloud_project,
            self.upload_bucket_name,
            self.dataflow_bucket_name,
            self.upload_pub_sub_topic,
            self.upload_subscription,
            self.app_engine_region
        )

    def main(self):
        self.set_default_project_for_gcloud()
        self.enable_apis()
        self.create_storage_buckets()
        self.configure_bucket_file_upload_notifications()
        self.create_pub_sub_subscription()
        self.install_required_libs()
        self.deploy_dataflow_templates()
        self.initialize_app_engine_in_project()
        self.initialize_firebase_project()
        self.deploy_app_engine_management_application()

    def get_google_cloud_env_var(self):
        if 'GOOGLE_CLOUD_PROJECT' in os.environ:
            return os.environ['GOOGLE_CLOUD_PROJECT'].strip()
        else:
            try:
                cmd = 'gcloud config get-value project'
                return subprocess.check_output(cmd, shell=True).strip()
            except subprocess.CalledProcessError:
                raise IllegalArgumentException('Please define GOOGLE_CLOUD_PROJECT environment variable')

    def set_default_project_for_gcloud(self):
        cmd = "gcloud config set project %s" % self.google_cloud_project
        print 'Set default project for gcloud: %s' % cmd
        subprocess.check_call(cmd, shell=True)

    def enable_apis(self):
        self.enable_api("dataflow.googleapis.com")
        self.enable_api("firebase.googleapis.com")

    def enable_api(self, api_name):
        cmd = 'gcloud services enable %s' % api_name
        print 'Enable apis: %s' % cmd
        subprocess.check_call(cmd, shell=True)

    def create_storage_buckets(self):
        cmd = 'gsutil ls'
        bucket_list = subprocess.check_output(cmd, shell=True)

        if self.upload_bucket_url in bucket_list:
            print 'Bucket %s already exists' % self.upload_bucket_name
        else:
            cmd = 'gsutil mb %s' % self.upload_bucket_url
            print 'Create a Google Cloud Storage bucket for FastQ files: %s' % cmd
            subprocess.check_call(cmd, shell=True)

        if self.dataflow_bucket_url in bucket_list:
            print 'Bucket %s already exists' % self.dataflow_bucket_name
        else:
            cmd = 'gsutil mb %s' % self.dataflow_bucket_url
            print 'Create a Google Cloud Storage bucket for Dataflow files: %s' % cmd
            subprocess.check_call(cmd, shell=True)

    def configure_bucket_file_upload_notifications(self):
        cmd = 'gsutil notifications list %s' % self.upload_bucket_url
        notifications = subprocess.check_output(cmd, shell=True)
        if self.upload_pub_sub_topic in notifications:
            print 'Bucket notification already exists: %s' % notifications
        else:
            cmd = 'gsutil notification create -t %s -f json -e OBJECT_FINALIZE %s' % (self.upload_pub_sub_topic, self.upload_bucket_url)
            print 'Create bucket notification: %s' % cmd
            subprocess.check_call(cmd, shell=True)

    def create_pub_sub_subscription(self):
        cmd = 'gcloud pubsub subscriptions list'
        subsriptions = subprocess.check_output(cmd, shell=True)
        if self.upload_subscription in subsriptions:
            print 'PubSub subscription already exists: %s' % subsriptions
        else:
            cmd = 'gcloud pubsub subscriptions create %s --topic %s' % (self.upload_subscription, self.upload_pub_sub_topic)
            print 'Create a PubSub subscription: %s' % cmd
            subprocess.check_call(cmd, shell=True)

    def install_required_libs(self):
        cmd = 'mvn install:install-file -Dfile=NanostreamDataflowMain/libs/japsa.jar -DgroupId=coin -DartifactId=japsa -Dversion=1.9-3c -Dpackaging=jar'
        print 'Add japsa dependency: %s' % cmd
        subprocess.check_call(cmd, shell=True)

        cmd = 'mvn install:install-file -Dfile=NanostreamDataflowMain/libs/pal1.5.1.1.jar -DgroupId=nz.ac.auckland -DartifactId=pal -Dversion=1.5.1.1 -Dpackaging=jar'
        print 'Add pal dependency: %s' % cmd
        subprocess.check_call(cmd, shell=True)

    def deploy_dataflow_templates(self):
        self.deploy_dataflow_template('species')
        self.deploy_dataflow_template('resistance_genes')

    def deploy_dataflow_template(self, processing_mode):
        template_name = 'nanostream-' + processing_mode
        alignment_window = 20
        stats_update_frequency = 30

        memory_output_limit = 0

        resistance_genes_list = self.upload_bucket_url + 'gene-info/resistance_genes_list.txt'
        output_dir = 'clinic_processing_output/'

        reference_database = 'genomeDB'
        all_references_dir_gcs_uri = self.upload_bucket_url + 'references/'

        cmd = 'mvn compile exec:java ' \
              '-f NanostreamDataflowMain/pipeline/pom.xml ' \
              '-Dexec.mainClass=com.google.allenday.nanostream.NanostreamApp ' \
              '-Dexec.args="' \
              '--project=%s ' \
              '--runner=DataflowRunner ' \
              '--streaming=true ' \
              '--processingMode=%s ' \
              '--inputDataSubscription=%s ' \
              '--alignmentWindow=%s ' \
              '--statisticUpdatingDelay=%s ' \
              '--resistanceGenesList=%s ' \
              '--resultBucket=%s ' \
              '--outputDir=%s ' \
              '--referenceNamesList=%s ' \
              '--allReferencesDirGcsUri=%s ' \
              '--gcpTempLocation=%s ' \
              '--stagingLocation=%s ' \
              '--templateLocation=%s ' \
              '--memoryOutputLimit=%s ' \
              '" ' \
              '-Dexec.cleanupDaemonThreads=false' \
              % (
                  self.google_cloud_project,
                  processing_mode,
                  self.upload_subscription_fullname,
                  alignment_window,
                  stats_update_frequency,
                  resistance_genes_list,
                  self.upload_bucket_url,
                  output_dir,
                  reference_database,
                  all_references_dir_gcs_uri,
                  self.dataflow_bucket_url + 'tmp',
                  self.dataflow_bucket_url + 'staging',
                  self.dataflow_bucket_url + 'templates/' + template_name,
                  memory_output_limit
              )
        print 'Compile and deploy Dataflow template for processing mode %s: %s' % (processing_mode, cmd)
        subprocess.check_call(cmd, shell=True)

    def initialize_app_engine_in_project(self):
        cmd = 'gcloud app describe'
        try:
            subprocess.check_output(cmd, stderr=subprocess.STDOUT, shell=True)
            # Do nothing here. App Engine already initialized within current project
        except subprocess.CalledProcessError:
            cmd = 'gcloud app create --region=%s' % self.app_engine_region
            print 'Initialize an App Engine application within the project: %s' % cmd
            subprocess.check_output(cmd, shell=True)

    def initialize_firebase_project(self):
        self.firebase_handler.add_firebase_to_project()
        self.firebase_handler.write_firebase_config()

    def deploy_app_engine_management_application(self):
        cmd = 'mvn clean package appengine:deploy -DskipTests=true -f NanostreamDataflowMain/webapp/pom.xml'
        print 'Compile and deploy App Engine management application: %s' % cmd
        subprocess.check_call(cmd, shell=True)


class FirebaseHandler:
    def __init__(self, google_cloud_project):
        self.dir_file = os.path.dirname(os.path.realpath(__file__))
        self.google_cloud_project = google_cloud_project

    def add_firebase_to_project(self):
        cmd = 'firebase projects:addfirebase %s' % self.google_cloud_project
        print 'Trying add firebase to the project: %s' % cmd
        try:
            subprocess.check_output(cmd, stderr=subprocess.STDOUT, shell=True).strip()
        except subprocess.CalledProcessError:
            print "The project already added to firebase"

    def write_firebase_config(self):
        web_app_id = self._get_or_create_firebase_web_app()
        cmd = 'firebase apps:sdkconfig WEB %s' % web_app_id
        print 'Getting config data from firebase webapp: %s' % cmd
        out = subprocess.check_output(cmd, shell=True).strip()
        print out

        parsed = self._parse_config_data(out)

        config_data = self._process_template(FIREBASE_CONFIG_TEMPLATE_FILENAME, parsed)
        print "Prepared firebase config: %s" % config_data
        self._write_firebase_config_file(config_data)

    def _get_or_create_firebase_web_app(self):
        web_app_id = self._try_get_id_from_app_list()
        print 'Found web app id: %s' % web_app_id
        if not web_app_id:
            web_app_id = self._try_create_web_app()
        return web_app_id

    def _try_get_id_from_app_list(self):
        cmd = 'firebase apps:list --project %s' % self.google_cloud_project
        app_list = subprocess.check_output(cmd, shell=True).strip()
        print 'Application list: %s' % app_list
        expected_app_name = 'web_%s' % self.google_cloud_project
        # │ web_tas-nanostream-test1 │ 1:253229025431:web:e39391f630f6c68ba981d2     │ WEB      │
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
        print 'create firebase app for web or get info: %s' % cmd

        out = subprocess.check_output(cmd, shell=True).strip()

        # extract id from output: firebase apps:sdkconfig WEB 1:22222222222222222222222222222
        return re.sub(r'^.*firebase apps:sdkconfig WEB (.*)', r'\1', out, 0, re.S)

    def _process_template(self, filename, parsed):
        with open(self.dir_file + '/' + filename, 'r') as file:
            data = file.read()
        t = Template(data)
        return t.substitute(**parsed)

    def _parse_config_data(self, text):
        text = self._cleanup_sdkconfig_output(text)
        return json.loads(text)

    def _cleanup_sdkconfig_output(self, text):
        return re.sub(r'^.*firebase.initializeApp\(({.+?})\);.*', r'\1', text, 0, re.S)

    def _write_firebase_config_file(self, text):
        filename = self.dir_file + '/' + FIREBASE_CONFIG_FILENAME
        print 'Writing firebase config file: %s' % filename
        with open(filename, 'w') as file:
            file.write(text)


class IllegalArgumentException(Exception):
    def __init__(self, msg):
        self.msg = msg


if __name__ == "__main__":
    try:
        app = Install()
        app.main()
    except IllegalArgumentException as e:
        print e.msg