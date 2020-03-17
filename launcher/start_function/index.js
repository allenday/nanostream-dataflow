const config = require('./config.js');
const request = require('request');


const appengineUrl = "https://" + config.projectId + ".appspot.com/api/v1";
// const appengineUrl = 'http://localhost:8080/api/v1';

const launchJobUrl = '/jobs/launch';


/**
 * Responds to any HTTP request.
 * This entry for development/debug purposes only.
 * Usage: `npm run locally`, then make GET request to
 * http://localhost:8082/?processingFileName=dogbite/20180326_spiked1_ch10000_readNA12889_strand.fastq&uploadBucketName=nanostream-test1-upload-bucket
 *
 * @param {!express:Request} req HTTP request context.
 * @param {!express:Response} res HTTP response context.
 */
exports.run_dataflow_job_http = (req, res) => {

    console.dir(req.originalUrl);  // https://expressjs.com/en/api.html#req.originalUrl

    const processingFileName = req.query.processingFileName || req.body.processingFileName || 'Missing parameter processingFileName!';
    const uploadBucketName = req.query.uploadBucketName || req.body.uploadBucketName || 'Missing parameter uploadBucketName!';

    main(processingFileName, uploadBucketName);

    res.status(200).send(processingFileName + " | " + uploadBucketName);
};

/**
 * Triggered from a change to a Cloud Storage bucket.
 *
 * @param {!Object} event Event payload.
 * @param {!Object} context Metadata for the event.
 */
exports.run_dataflow_job = (event, context) => {
    const processingFileName = event.name;
    const uploadBucketName = event.bucket;
    main(processingFileName, uploadBucketName);
};

function main(processingFileName, uploadBucketName)
{
    console.log(`Processing file: ${processingFileName}`);
    console.log(`Upload bucket name: ${uploadBucketName}`);
    const targetInputFolder = _extractTargetInputFolder(processingFileName);
    console.log(`Target folder: ${targetInputFolder}`);

    if (targetInputFolder && uploadBucketName) {
        _run_pipeline_job_if_required(targetInputFolder, uploadBucketName);
    }
}

/**
 * dogbite/20180326_spiked1_ch10003_readNA12889_strand.fastq => dogbite
 *
 * @param processingFileName
 * @returns {null|*}
 * @private
 */
function _extractTargetInputFolder(processingFileName) {
    if (processingFileName.includes('/')) {
        return processingFileName.replace(new RegExp("\/.*"), '');
    } else {
        return null;
    }
}

function _run_pipeline_job_if_required(targetInputFolder, uploadBucketName) {
    const url = appengineUrl + launchJobUrl;
    const params = {targetInputFolder: targetInputFolder, uploadBucketName: uploadBucketName};
    console.log('Sending request to ' + url, params);
    request.post(url,
        { form: params },
        function (error, response, body) {
            console.log('response.statusCode: ', response.statusCode)
            console.log('response body: ', body)
            if (error) {
                console.log('error: ', error);
            }
        }
    );
}

