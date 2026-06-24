# TeenPle EC2 deployment files

- `teenple.env.example`: copy to `/etc/teenple/teenple.env`, replace every placeholder, and set mode `600`.
- `teenple-backend.service`: install as `/etc/systemd/system/teenple-backend.service`.
- `nginx-api.teenple.app.conf`: install under `/etc/nginx/conf.d/` after issuing the TLS certificate.
- `github-actions-ssm.md`: configure GitHub Actions deployment through SSM.

The EC2 instance profile must allow the required S3 operations on both buckets and `rekognition:DetectModerationLabels`. Do not place long-lived AWS access keys in the environment file.

Expected application paths:

- JAR: `/opt/teenple/teenple-backend.jar`
- Firebase service account: `/opt/teenple/secrets/firebase-service-account.json`
- Application logs: `/var/log/teenple`

Before starting the service, create the `teenple` system user and the directories above, then grant that user ownership of `/opt/teenple` and `/var/log/teenple`.
