# AWS Lambda Deployment Guide

## Lambda vs ECS Fargate: Which to Choose?

### AWS Lambda (Serverless)

**Pros:**
- ✅ Pay only for execution time (no idle costs)
- ✅ Auto-scales automatically
- ✅ No server management
- ✅ Free tier: 1M requests/month + 400,000 GB-seconds

**Cons:**
- ❌ Cold start latency (1-3 seconds for Java/Spring Boot)
- ❌ 15-minute execution timeout
- ❌ 10GB memory limit
- ❌ Requires code adaptation (Spring Cloud Function)
- ❌ Not ideal for long-running connections

**Best for:**
- Low to medium traffic (< 1000 requests/hour)
- Sporadic usage patterns
- Cost-sensitive projects
- APIs with acceptable cold start latency

### ECS Fargate (Containers)

**Pros:**
- ✅ No cold starts (always warm)
- ✅ Works with existing Spring Boot code (no changes)
- ✅ Better for sustained traffic
- ✅ Supports long-running connections
- ✅ More predictable performance

**Cons:**
- ❌ Runs 24/7 (costs even when idle)
- ❌ More expensive for low traffic
- ❌ Requires container management

**Best for:**
- Medium to high traffic (> 1000 requests/hour)
- Consistent traffic patterns
- Applications requiring low latency
- Production workloads

## Cost Comparison

### Lambda (Low Traffic: 10,000 requests/month)
- Requests: 10,000 × $0.0000002 = $0.002
- Compute: 10,000 × 1s × 1GB × $0.0000166667 = $0.17
- **Total: ~$0.20/month** (essentially free)

### Lambda (Medium Traffic: 1M requests/month)
- Requests: 1M × $0.0000002 = $0.20
- Compute: 1M × 1s × 1GB × $0.0000166667 = $16.67
- **Total: ~$17/month**

### ECS Fargate (Always Running)
- 2 tasks × 0.5 vCPU × $0.04048/hour × 730 hours = $29.55
- 2 tasks × 1GB × $0.004445/hour × 730 hours = $6.49
- **Total: ~$36/month** (minimum)

**Recommendation:**
- **Use Lambda if:** Traffic < 100,000 requests/month, can tolerate cold starts
- **Use ECS Fargate if:** Traffic > 100,000 requests/month, need consistent performance

## Lambda Deployment - Important Considerations

### 1. Code Modifications Required

Spring Boot applications need adaptation for Lambda:

**Add Spring Cloud Function dependency:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-function-adapter-aws</artifactId>
    <version>4.0.0</version>
</dependency>
```

**Create Function Handler:**
```java
@Bean
public Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> userRegistration() {
    return request -> {
        // Your Spring Boot application logic
        return response;
    };
}
```

### 2. Cold Start Optimization

Java/Spring Boot has significant cold start times (1-5 seconds):

**Mitigation strategies:**
- Use Provisioned Concurrency (costs extra)
- Implement SnapStart (Java 11+)
- Use GraalVM native image (complex setup)
- Keep Lambda warm with scheduled pings

### 3. Database Connection Pooling

Lambda functions are stateless and short-lived:

**Issues:**
- Each invocation creates new DB connections
- Connection pool doesn't work well
- Can exhaust RDS connections

**Solutions:**
- Use RDS Proxy (adds cost: ~$15/month)
- Reduce connection pool size
- Use Aurora Serverless v2

## Lambda Deployment Steps

### Step 1: Modify Application for Lambda

**Add Maven Plugin:**
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot.experimental</groupId>
            <artifactId>spring-boot-thin-layout</artifactId>
            <version>1.0.28.RELEASE</version>
        </dependency>
    </dependencies>
</plugin>
```

**Create Lambda Handler:**
```java
package com.example.userregistration;

import org.springframework.cloud.function.adapter.aws.SpringBootRequestHandler;

public class LambdaHandler extends SpringBootRequestHandler<Map<String, Object>, Map<String, Object>> {
}
```

### Step 2: Deploy Infrastructure

```bash
# Deploy CloudFormation stack
aws cloudformation create-stack \
  --stack-name user-registration-lambda \
  --template-body file://aws/lambda-infrastructure.yml \
  --parameters \
    ParameterKey=DatabasePassword,ParameterValue=YourPassword123! \
  --capabilities CAPABILITY_IAM

# Wait for completion
aws cloudformation wait stack-create-complete \
  --stack-name user-registration-lambda
```

### Step 3: Build and Deploy Lambda Package

```bash
# Build JAR
mvn clean package -DskipTests

# Get S3 bucket name
BUCKET=$(aws cloudformation describe-stacks \
  --stack-name user-registration-lambda \
  --query 'Stacks[0].Outputs[?OutputKey==`DeploymentBucketName`].OutputValue' \
  --output text)

# Upload to S3
aws s3 cp target/user-registration-1.0.0.jar \
  s3://$BUCKET/user-registration.jar

# Update Lambda function
aws lambda update-function-code \
  --function-name production-user-registration \
  --s3-bucket $BUCKET \
  --s3-key user-registration.jar
```

### Step 4: Test the API

```bash
# Get API endpoint
API_URL=$(aws cloudformation describe-stacks \
  --stack-name user-registration-lambda \
  --query 'Stacks[0].Outputs[?OutputKey==`ApiEndpoint`].OutputValue' \
  --output text)

# Test health endpoint
curl $API_URL/actuator/health

# Test registration
curl -X POST $API_URL/api/v1/register/email \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

## Performance Optimization

### 1. Provisioned Concurrency

Eliminates cold starts but adds cost:

```bash
aws lambda put-provisioned-concurrency-config \
  --function-name production-user-registration \
  --provisioned-concurrent-executions 2
```

**Cost:** ~$10/month for 2 instances

### 2. SnapStart (Java 11+)

Reduces cold start time by 90%:

```bash
aws lambda update-function-configuration \
  --function-name production-user-registration \
  --snap-start ApplyOn=PublishedVersions
```

**Cost:** Free, but requires Java 11+

### 3. Memory Allocation

More memory = faster execution:

```bash
# Increase to 2GB for better performance
aws lambda update-function-configuration \
  --function-name production-user-registration \
  --memory-size 2048
```

## Monitoring

### CloudWatch Metrics

```bash
# View invocations
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Invocations \
  --dimensions Name=FunctionName,Value=production-user-registration \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-02T00:00:00Z \
  --period 3600 \
  --statistics Sum

# View errors
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Errors \
  --dimensions Name=FunctionName,Value=production-user-registration \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-02T00:00:00Z \
  --period 3600 \
  --statistics Sum
```

### View Logs

```bash
aws logs tail /aws/lambda/production-user-registration --follow
```

## Limitations

### Lambda Constraints

1. **Timeout:** 15 minutes maximum
2. **Memory:** 10GB maximum
3. **Package size:** 250MB (unzipped), 50MB (zipped)
4. **Concurrent executions:** 1000 (default, can request increase)
5. **Cold starts:** 1-5 seconds for Java/Spring Boot

### Not Suitable For

- WebSocket connections
- Long-running processes (> 15 minutes)
- High-frequency, low-latency APIs
- Applications requiring persistent connections
- Real-time streaming

## Recommendation

**For your User Registration Service:**

I recommend **ECS Fargate** over Lambda because:

1. **Spring Boot is heavy** - Cold starts will be 2-5 seconds
2. **Database connections** - Lambda doesn't work well with connection pools
3. **Consistent performance** - Registration should be fast and reliable
4. **OAuth flows** - May require longer execution times
5. **Production-ready** - ECS is more battle-tested for Spring Boot

**Use Lambda only if:**
- Traffic is very low (< 10,000 requests/month)
- Cost is the primary concern
- You're willing to refactor the application
- You can tolerate cold start latency

## Hybrid Approach

You can use both:

- **ECS Fargate:** Main API (registration, verification)
- **Lambda:** Background tasks (email sending, cleanup jobs)

This gives you the best of both worlds!

## Next Steps

If you want to proceed with Lambda:

1. I can help refactor the code for Spring Cloud Function
2. Set up RDS Proxy for better database connections
3. Implement SnapStart for faster cold starts
4. Configure Provisioned Concurrency for critical endpoints

Or stick with ECS Fargate for a simpler, more reliable deployment.

What would you prefer?
