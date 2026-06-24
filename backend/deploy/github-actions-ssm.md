# GitHub Actions SSM deployment

This project deploys production by building the Spring Boot JAR in GitHub Actions,
uploading it to S3, then asking EC2 through SSM Run Command to download the JAR
and restart `teenple-backend`.

No application secrets are stored in GitHub Actions. Keep production environment
values in `/etc/teenple/teenple.env` on EC2.

## 1. EC2 prerequisites

The instance must already have:

- SSM Agent running.
- An IAM instance profile with permission to read the deploy artifact bucket.
- Java 21 installed at `/usr/bin/java`.
- AWS CLI installed.
- `/opt/teenple`, `/var/log/teenple`, and `/etc/teenple/teenple.env` prepared.
- `teenple-backend.service` installed and working.
- SSM managed instance ID matching the workflow target:
  - stored in GitHub Actions secret `SSM_INSTANCE_ID`

The EC2 role needs at least:

```json
{
  "Effect": "Allow",
  "Action": ["s3:GetObject"],
  "Resource": "arn:aws:s3:::YOUR_DEPLOY_BUCKET/deploy/releases/*"
}
```

## 2. Deploy bucket

Create a private S3 bucket for deployment artifacts. This is ideally separate
from user-upload buckets.

Example name:

```text
teenple-deploy-artifacts
```

If you intentionally reuse the existing `teenple-student-card` bucket, keep
deployment artifacts under this prefix only:

```text
s3://teenple-student-card/deploy/releases/
```

## 3. GitHub repository secrets

Add these in GitHub:

`Settings` -> `Secrets and variables` -> `Actions` -> `Repository secrets`

- `AWS_ROLE_TO_ASSUME`: IAM role ARN used by GitHub Actions through OIDC.
- `DEPLOY_BUCKET`: private S3 bucket name for JAR artifacts.
- `SSM_INSTANCE_ID`: EC2 instance ID managed by SSM.

Do not add `.env`, database passwords, JWT secrets, Firebase JSON, or mail
passwords to GitHub Actions secrets for this deployment flow.

## 4. GitHub OIDC role permissions

The GitHub Actions role needs permissions similar to:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:PutObject"],
      "Resource": "arn:aws:s3:::YOUR_DEPLOY_BUCKET/deploy/releases/*"
    },
    {
      "Effect": "Allow",
      "Action": ["ssm:SendCommand"],
      "Resource": [
        "arn:aws:ssm:ap-northeast-2::document/AWS-RunShellScript",
        "arn:aws:ec2:ap-northeast-2:YOUR_AWS_ACCOUNT_ID:instance/*"
      ],
      "Condition": {
        "StringEquals": {
          "ec2:ResourceTag/Name": "teenple-ec2"
        }
      }
    },
    {
      "Effect": "Allow",
      "Action": ["ssm:ListCommandInvocations"],
      "Resource": "*"
    }
  ]
}
```

The role trust policy must allow GitHub OIDC from this repository and branch
`main`.

## 5. Deployment trigger

The workflow runs when code is pushed to `main`. It can also be run manually from
the GitHub Actions tab.

Before merging to `main`, make sure `develop` builds successfully:

```bash
./gradlew clean bootJar
```
