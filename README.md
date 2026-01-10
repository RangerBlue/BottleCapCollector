# Bottle Cap Collector - Backend

Spring Boot REST API backend for the Bottle Cap Collector system.

## Related Projects

- [React Frontend](https://github.com/RangerBlue/bottle-cap-collector-front/)
- [Android App](https://github.com/RangerBlue/mBottleCapCollector)
- [Website](https://rangerblue.github.io/bottle-cap-collector-front/)

## Tech Stack

- Java 21
- Spring Boot 4.0.0
- Google Cloud Platform:
  - Firestore (database)
  - Cloud Storage (image storage)
  - Vision API (image analysis)
  - Vertex AI (embeddings for similarity search)
  - Cloud Run (deployment)

## Local Development

### Prerequisites

- Java 21
- Maven
- Docker (for Firestore emulator)

### Running Firestore Emulator

```bash
gcloud emulators firestore start --host-port=127.0.0.1:8081
```

### Building the Project

```bash
./mvnw clean package
```

### Running Locally

```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

## GCP Deployment

### Prerequisites

1. Install and configure `gcloud` CLI:

```bash
# Login to GCP
gcloud auth login

# Set your project
gcloud config set project bottlecapcollector-480817
```

### Step 1: Enable Required APIs

```bash
gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  firestore.googleapis.com \
  storage.googleapis.com \
  vision.googleapis.com \
  aiplatform.googleapis.com
```

### Step 2: Create Cloud Storage Bucket

```bash
gcloud storage buckets create gs://bottlecap-images \
  --location=us-central1 \
  --uniform-bucket-level-access
```

### Step 3: Deploy to Cloud Run

```bash
gcloud run deploy bottlecap-collector \
  --source . \
  --region us-central1 \
  --platform managed \
  --allow-unauthenticated \
  --memory 1Gi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 3 \
  --set-env-vars "GCP_PROJECT_ID=bottlecapcollector-480817,CLOUD_STORAGE_BUCKET=bottlecap-images,GOOGLE_CLIENT_ID=<your-oauth-client-id>,GOOGLE_CLIENT_SECRET=<your-oauth-secret>,CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com"
```

### Step 4: Grant Service Account Permissions

Cloud Run uses a default service account. Grant it access to GCP services:

```bash
SA_NAME=collection-item-prod
PROJECT_ID=bottlecapcollector-480817
gcloud iam service-accounts create $SA_NAME
SA_ACCOUNT=${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com


# Firestore access
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:$SA_ACCOUNT" \
   --role="roles/firestore.user"

# Cloud Storage access
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:$SA_ACCOUNT" \
  --role="roles/storage.objectAdmin"

## Vision API access
#gcloud projects add-iam-policy-binding $PROJECT_ID \
#  --member="serviceAccount:$SA_ACCOUNT" \
#  --role="roles/visionai.admin"

# Vertex AI access (for embeddings)
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:$SA_ACCOUNT" \
  --role="roles/aiplatform.user"
```

### Environment Variables

| Variable                | Description                              | Required |
|-------------------------|------------------------------------------|----------|
| `GCP_PROJECT_ID`        | GCP project ID                           | Yes      |
| `CLOUD_STORAGE_BUCKET`  | Cloud Storage bucket name                | Yes      |
| `GOOGLE_CLIENT_ID`      | OAuth2 client ID                         | Yes      |
| `GOOGLE_CLIENT_SECRET`  | OAuth2 client secret                     | Yes      |
| `CORS_ALLOWED_ORIGINS`  | Comma-separated allowed origins          | Yes      |
| `LEGACY_USER_ID`        | Legacy API user ID                       | No       |
| `LEGACY_COLLECTION_KEY` | Legacy API collection key                | No       |
| `VERTEX_AI_LOCATION`    | Vertex AI region (default: us-central1)  | No       |
| `FIRESTORE_DATABASE_ID` | Firestore database ID (default: default) | No       |

### Cost Optimization

The deployment is configured for cost efficiency:

- **Min instances = 0**: Scales to zero when no traffic (pay only when used)
- **Memory/CPU**: 1Gi/1 CPU (adjust based on actual usage)
- **Max instances = 3**: Limits scaling to control costs

### Updating Deployment

To deploy updates:

```bash
gcloud run deploy bottlecap-collector --source . --region us-central1
```

## API Documentation

When running, OpenAPI documentation is available at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI spec: `http://localhost:8080/v3/api-docs`

## Architecture

```
Controller -> Service -> Repository -> Firestore
     |
     v
   DTO <-> Entity (via MapStruct)
```

### Image Processing Pipeline

1. Upload image to Cloud Storage
2. Calculate HSB color histogram (OpenCV)
3. Analyze image with Vision API
4. Generate embeddings with Vertex AI
5. Use embeddings for similarity search
