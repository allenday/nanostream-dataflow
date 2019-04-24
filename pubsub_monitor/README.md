This is where various "push" PubSub subscriptions will be "pushed" to.
For some reason this feature requires URLs to act as endpoints,
so we might as well put some functional pages here that also act as
diagnostics to check if messages are being seng properly.

Recommended further work: consolidate these TOS/PP stuff into sub-domains?
Not sure if it's a good idea to.

## **Read all 4 steps before starting.**

1. To create the subscription:

```
gcloud beta pubsub subscriptions create upload_watcher \
            --topic file_upload \
            --push-endpoint \
                https://{SERVICE-NAME}-dot-nano-stream1.appspot.com/pubsub/push?token={PUBSUB_VERIFICATION_TOKEN} \
            --ack-deadline 30
```

OR you can do it in GUI.

2. Localise {variables} to be edited for your environment, above and in `app.yaml`. Note your `PUBSUB_VERIFICATION_TOKEN`, This can be any random string of your choice.

3. Deploy the app on GCP by `cd` into the folder with `app.yaml`, then `gcloud app deploy`.

4. The website will be publicly accessible and its URL will be the `service` string in `app.yaml`, followed by `-dot-<project-id>.appspot.com`. For example, `upload-watcher-dot-nano-stream.appspot.com`

# Original README from GOOGLE, can be ignored if the above instructions worked:

## Python Google Cloud Pub/Sub sample for Google App Engine Flexible Environment

[![Open in Cloud Shell][shell_img]][shell_link]

[shell_img]: http://gstatic.com/cloudssh/images/open-btn.png
[shell_link]: https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/GoogleCloudPlatform/python-docs-samples&page=editor&open_in_editor=appengine/flexible/pubsub/README.md

This demonstrates how to send and receive messages using [Google Cloud Pub/Sub](https://cloud.google.com/pubsub) on [Google App Engine Flexible Environment](https://cloud.google.com/appengine).

### Setup

Before you can run or deploy the sample, you will need to do the following:

1. Enable the Cloud Pub/Sub API in the [Google Developers Console](https://console.developers.google.com/project/_/apiui/apiview/pubsub/overview).

2. Create a topic and subscription.

        $ gcloud beta pubsub topics create [your-topic-name]
        $ gcloud beta pubsub subscriptions create [your-subscription-name] \
            --topic [your-topic-name] \
            --push-endpoint \
                https://[your-app-id].appspot.com/pubsub/push?token=[your-token] \
            --ack-deadline 30

3. Update the environment variables in ``app.yaml``.

### Running locally

Refer to the [top-level README](../README.md) for instructions on running and deploying.

When running locally, you can use the [Google Cloud SDK](https://cloud.google.com/sdk) to provide authentication to use Google Cloud APIs:

    $ gcloud init

Install dependencies, preferably with a virtualenv:

    $ virtualenv env
    $ source env/bin/activate
    $ pip install -r requirements.txt

Then set environment variables before starting your application:

    $ export PUBSUB_VERIFICATION_TOKEN=[your-verification-token]
    $ export PUBSUB_TOPIC=[your-topic]
    $ python main.py

#### Simulating push notifications

The application can send messages locally, but it is not able to receive push messages locally. You can, however, simulate a push message by making an HTTP request to the local push notification endpoint. There is an included ``sample_message.json``. You can use
``curl`` or [httpie](https://github.com/jkbrzt/httpie) to POST this:

    $ curl -i --data @sample_message.json ":8080/pubsub/push?token=[your-token]"

Or

    $ http POST ":8080/pubsub/push?token=[your-token]" < sample_message.json

Response:

    HTTP/1.0 200 OK
    Content-Length: 2
    Content-Type: text/html; charset=utf-8
    Date: Mon, 10 Aug 2015 17:52:03 GMT
    Server: Werkzeug/0.10.4 Python/2.7.10

    OK

After the request completes, you can refresh ``localhost:8080`` and see the message in the list of received messages.

### Running on App Engine

Deploy using `gcloud`:

    gcloud app deploy app.yaml

You can now access the application at `https://your-app-id.appspot.com`. You can use the form to submit messages, but it's non-deterministic which instance of your application will receive the notification. You can send multiple messages and refresh the page to see the received message.
