import subprocess
import os


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
        self.deploy_dataflow_template()
        self.initialize_app_engine_in_project()
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
        cmd = 'gcloud services enable dataflow.googleapis.com'
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

        cmd = 'mvn install:install-file -Dfile=NanostreamDataflowMain/libs/genomics-dataflow-core.jar -DgroupId=com.google.allenday.genomics.core -DartifactId=genomics-dataflow-core -Dversion=0.0.1 -Dpackaging=jar'
        print 'Add genomics.core dependency: %s' % cmd
        subprocess.check_call(cmd, shell=True)

    def deploy_dataflow_template(self):
        alignment_window = 20
        stats_update_frequency = 30
        reference_database = 'genomeDB'
        firestore_collection_name_prefix = 'prefix_'
        firestore_document_name_prefix = 'doc_'
        resistance_genes_list = self.upload_bucket_url + 'gene-info/resistance_genes_list.txt'
        aligned_output_dir = 'clinic_processing_output/%s/result_aligned_bam/'
        all_references_dir_gcs_uri = 'self.dataflow_bucket_url' + 'references/'

        # '--outputDocumentNamePrefix=%s ' \
        # firestore_document_name_prefix,
        # '--processingMode=species ' \

        cmd = 'mvn compile exec:java ' \
              '-f NanostreamDataflowMain/pipeline/pom.xml ' \
              '-Dexec.mainClass=com.google.allenday.nanostream.NanostreamApp ' \
              '-Dexec.args="' \
              '--project=%s ' \
              '--runner=DataflowRunner ' \
              '--streaming=true ' \
              '--processingMode=species ' \
              '--inputDataSubscription=%s ' \
              '--alignmentWindow=%s ' \
              '--statisticUpdatingDelay=%s ' \
              '--referenceNamesList=%s ' \
              '--outputCollectionNamePrefix=%s ' \
              '--resistanceGenesList=%s ' \
              '--resultBucket=%s ' \
              '--alignedOutputDir=%s ' \
              '--allReferencesDirGcsUri=%s ' \
              '--gcpTempLocation=%s ' \
              '--stagingLocation=%s ' \
              '--templateLocation=%s ' \
              '" ' \
              '-Dexec.cleanupDaemonThreads=false' \
              % (
                    self.google_cloud_project,
                    self.upload_subscription_fullname,
                    alignment_window,
                    stats_update_frequency,
                    reference_database,
                    firestore_collection_name_prefix,
                    resistance_genes_list,
                    self.upload_bucket_url,
                    aligned_output_dir,
                    all_references_dir_gcs_uri,
                    self.dataflow_bucket_url + 'tmp',
                    self.dataflow_bucket_url + 'staging',
                    self.dataflow_bucket_url + 'templates/nanostream-species'
                    )
        print 'Compile and deploy Dataflow template: %s' % cmd
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

    def deploy_app_engine_management_application(self):
        cmd = 'mvn clean package appengine:deploy -DskipTests=true -f NanostreamDataflowMain/webapp/pom.xml'
        print 'Compile and deploy App Engine management application: %s' % cmd
        subprocess.check_call(cmd, shell=True)


class IllegalArgumentException(Exception):
    def __init__(self, msg):
        self.msg = msg


if __name__ == "__main__":
    try:
        app = Install()
        app.main()
    except IllegalArgumentException as e:
        print e.msg