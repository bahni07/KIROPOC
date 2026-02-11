# AWS Deployment Guide

Complete guide for deploying the User Registration Service on AWS using native services.

## Architecture Overview

```
GitHub Actions (CI/CD)
    ↓
Amazon ECR (Container Registry)
    ↓
Amazon ECS Fargate (Container Orchestration)
    ↓
Application Load Balancer (ALB)
    ↓
Amazon RDS PostgreSQL (Database)
```

## AWS Services Used

1. **Amazon ECR** - Docker container registry
2. **Amazon ECS Fargate** - Serverless container orchestration
3. **Application Load Balancer** - Load balancing and SSL termination
4. **Amazon RDS PostgreSQL** - Managed database
5. **AWS Secrets Manager** - Secure secrets storage
6. **CloudWatch Logs** - Application logging
7. **VPC** - Network isolation

## Cost Estimate

### Free Tier (First 12 months)
- ECS Fargate: First 20GB storage, 10GB data transfer
- RDS: 750 hours db.t3.micro, 20GB storage
- ALB: 750 hours, 15GB data processing
- ECR: 500MB storage

### After Free Tier (Monthly)
- **Minimal Usage**: $15-25/month
  - ECS Fargate: ~$10
  - RDS db.t3.micro: ~$12
  - ALB: ~$16
  - Data transfer: ~$1
  
- **Production Usage**: $50-100/month
  - ECS Fargate (2 tasks): ~$30
  - RDS db.t3.small: ~$25
  - ALB: ~$20
  - Data transfer: ~$5

## Prerequisites

1. **AWS Account** - [Sign up](https://aws.amazon.com/)
2. **AWS CLI** - [Install](https://aws.amazon.com/cli/)
3. **GitHub Repository** - Already have: https://github.com/bahni07/KIROPOC

## Setup Instructions

### Step 1: Configure AWS CLI

```bash
# Install AWS CLI (Windows)
msiexec.exe /i https://awscli.amazonaws.com/AWSCLIV2.msi

# Configure credentials
aws configure
# Enter:
# - AWS Access Key ID
# - AWS Secret Access Key
# - Default region: us-east-1
# - Default output format: json
```

### Step 2: Deploy Infrastructure with CloudFormation

```bash
# Navigate to project directory
cd D:\Codebase\KiroPOC

# Deploy the stack
aws cloudformation create-stack \
  --stack-name user-registration-prod \
  --template-body file://aws/infrastructure.yml \
  --parameters \
    ParameterKey=EnvironmentName,ParameterValue=production \
    ParameterKey=DatabaseUsername,ParameterValue=postgres \
    ParameterKey=DatabasePassword,ParameterValue=YourSecurePassword123! \
  --capabilities CAPABILITY_IAM

# Wait for stack creation (takes 10-15 minutes)
aws cloudformation wait stack-create-complete \
  --stack-name user-registration-prod

# Get outputs
aws cloudformation describe-stacks \
  --stack-name user-registration-prod \
  --query 'Stacks[0].Outputs'
```

### Step 3: Update Application Secrets

```bash
# Get the secret ARN
SECRET_ARN=$(aws cloudformation describe-stacks \
  --stack-name user-registration-prod \
  --query 'Stacks[0].Outputs[?OutputKey==`ApplicationSecretsArn`].OutputValue' \
  --output text)

# Update secrets with your actual values
aws secretsmanager update-secret \
  --secret-id $SECRET_ARN \
  --secret-string '{
    "SPRING_DATASOURCE_PASSWORD": "YourSecurePassword123!",
    "EMAIL_USERNAME": "your_email@gmail.com",
    "EMAIL_PASSWORD": "your_app_password",
    "GOOGLE_CLIENT_ID": "your_google_client_id",
    "GOOGLE_CLIENT_SECRET": "your_google_client_secret",
    "AMAZON_CLIENT_ID": "your_amazon_client_id",
    "AMAZON_CLIENT_SECRET": "your_amazon_client_secret",
    "ENCRYPTION_KEY": "your_base64_encryption_key"
  }'
```

### Step 4: Configure GitHub Secrets

Add these secrets to your GitHub repository:

```bash
# Using GitHub CLI
gh secret set AWS_ACCESS_KEY_ID --body "YOUR_AWS_ACCESS_KEY"
gh secret set AWS_SECRET_ACCESS_KEY --body "YOUR_AWS_SECRET_KEY"

# Or manually at:
# https://github.com/bahni07/KIROPOC/settings/secrets/actions
```

Required secrets:
- `AWS_ACCESS_KEY_ID` - Your AWS access key
- `AWS_SECRET_ACCESS_KEY` - Your AWS secret key

### Step 5: Build and Push Initial Image

```bash
# Get ECR repository URI
ECR_URI=$(aws cloudformation describe-stacks \
  --stack-name user-registration-prod \
  --query 'Stacks[0].Outputs[?OutputKey==`ECRRepositoryURI`].OutputValue' \
  --output text)

# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin $ECR_URI

# Build and push
docker build -t user-registration:latest .
docker tag user-registration:latest $ECR_URI:latest
docker push $ECR_URI:latest
```

### Step 6: Update ECS Service

```bash
# Force new deployment with the image
aws ecs update-service \
  --cluster production-user-registration-cluster \
  --service user-registration-service \
  --force-new-deployment
```

### Step 7: Get Application URL

```bash
# Get load balancer URL
ALB_URL=$(aws cloudformation describe-stacks \
  --stack-name user-registration-prod \
  --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerURL`].OutputValue' \
  --output text)

echo "Application URL: $ALB_URL"

# Test the application
curl $ALB_URL/actuator/health
```

## Automated Setup Script

For easier setup, use the automated script:

### Windows (PowerShell)

```powershell
.\scripts\setup-aws.ps1
```

### Linux/Mac (Bash)

```bash
chmod +x scripts/setup-aws.sh
./scripts/setup-aws.sh
```

The script will:
1. Validate AWS CLI installation
2. Deploy CloudFormation stack
3. Configure secrets
4. Build and push Docker image
5. Deploy to ECS

## CI/CD Workflow

Once set up, the workflow is:

1. **Push to develop** → Tests run automatically
2. **Create PR to main** → Tests run, requires approval
3. **Create release** → Builds image, pushes to ECR, deploys to ECS

```bash
# Create a release
gh release create v1.0.0 \
  --title "Release v1.0.0" \
  --notes "Initial production release"
```

GitHub Actions will automatically:
- Build and test the application
- Build Docker image
- Push to Amazon ECR
- Update ECS service with new image

## Monitoring and Logs

### View Application Logs

```bash
# Get log stream
aws logs tail /ecs/production/user-registration --follow

# Or use AWS Console
# https://console.aws.amazon.com/cloudwatch/
```

### View ECS Service Status

```bash
aws ecs describe-services \
  --cluster production-user-registration-cluster \
  --services user-registration-service
```

### View RDS Status

```bash
aws rds describe-db-instances \
  --db-instance-identifier production-user-registration-db
```

## Scaling

### Manual Scaling

```bash
# Scale to 4 tasks
aws ecs update-service \
  --cluster production-user-registration-cluster \
  --service user-registration-service \
  --desired-count 4
```

### Auto Scaling (Optional)

Add to CloudFormation template:

```yaml
ECSAutoScalingTarget:
  Type: AWS::ApplicationAutoScaling::ScalableTarget
  Properties:
    MaxCapacity: 10
    MinCapacity: 2
    ResourceId: !Sub service/${ECSCluster}/${ECSService.Name}
    RoleARN: !Sub arn:aws:iam::${AWS::AccountId}:role/aws-service-role/ecs.application-autoscaling.amazonaws.com/AWSServiceRoleForApplicationAutoScaling_ECSService
    ScalableDimension: ecs:service:DesiredCount
    ServiceNamespace: ecs

ECSAutoScalingPolicy:
  Type: AWS::ApplicationAutoScaling::ScalingPolicy
  Properties:
    PolicyName: cpu-scaling
    PolicyType: TargetTrackingScaling
    ScalingTargetId: !Ref ECSAutoScalingTarget
    TargetTrackingScalingPolicyConfiguration:
      PredefinedMetricSpecification:
        PredefinedMetricType: ECSServiceAverageCPUUtilization
      TargetValue: 70.0
```

## SSL/HTTPS Setup

### Option 1: AWS Certificate Manager (Free)

```bash
# Request certificate
aws acm request-certificate \
  --domain-name yourdomain.com \
  --validation-method DNS

# Update ALB listener to use HTTPS
aws elbv2 create-listener \
  --load-balancer-arn <ALB_ARN> \
  --protocol HTTPS \
  --port 443 \
  --certificates CertificateArn=<CERT_ARN> \
  --default-actions Type=forward,TargetGroupArn=<TG_ARN>
```

### Option 2: CloudFront (CDN + SSL)

Add CloudFront distribution in front of ALB for:
- Free SSL certificate
- Global CDN
- DDoS protection
- Better performance

## Database Backups

RDS automatically creates daily backups (7-day retention).

### Manual Snapshot

```bash
aws rds create-db-snapshot \
  --db-instance-identifier production-user-registration-db \
  --db-snapshot-identifier manual-snapshot-$(date +%Y%m%d)
```

### Restore from Snapshot

```bash
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier restored-db \
  --db-snapshot-identifier manual-snapshot-20260211
```

## Troubleshooting

### ECS Tasks Not Starting

```bash
# Check task status
aws ecs describe-tasks \
  --cluster production-user-registration-cluster \
  --tasks $(aws ecs list-tasks \
    --cluster production-user-registration-cluster \
    --service-name user-registration-service \
    --query 'taskArns[0]' --output text)

# Check logs
aws logs tail /ecs/production/user-registration --follow
```

### Database Connection Issues

```bash
# Verify security group rules
aws ec2 describe-security-groups \
  --filters Name=group-name,Values=production-rds-sg

# Test connection from ECS task
aws ecs execute-command \
  --cluster production-user-registration-cluster \
  --task <TASK_ID> \
  --container user-registration \
  --interactive \
  --command "/bin/bash"
```

### High Costs

1. Check CloudWatch metrics for unused resources
2. Consider using Fargate Spot for non-production
3. Use RDS Reserved Instances for 40% savings
4. Enable S3 lifecycle policies for logs

## Cleanup

To delete all resources:

```bash
# Delete CloudFormation stack
aws cloudformation delete-stack \
  --stack-name user-registration-prod

# Wait for deletion
aws cloudformation wait stack-delete-complete \
  --stack-name user-registration-prod

# Delete ECR images
aws ecr batch-delete-image \
  --repository-name user-registration \
  --image-ids imageTag=latest
```

## Security Best Practices

1. ✅ Use Secrets Manager for sensitive data
2. ✅ Enable VPC Flow Logs
3. ✅ Use private subnets for RDS
4. ✅ Enable CloudTrail for audit logs
5. ✅ Use IAM roles (not access keys) where possible
6. ✅ Enable MFA for AWS account
7. ✅ Regularly rotate credentials
8. ✅ Use AWS WAF for DDoS protection

## Next Steps

1. Set up custom domain with Route 53
2. Configure SSL certificate with ACM
3. Set up CloudWatch alarms
4. Configure auto-scaling
5. Set up CI/CD pipeline
6. Enable AWS WAF for security
7. Configure backup strategy

## Support

- AWS Documentation: https://docs.aws.amazon.com/
- AWS Support: https://console.aws.amazon.com/support/
- GitHub Issues: https://github.com/bahni07/KIROPOC/issues
