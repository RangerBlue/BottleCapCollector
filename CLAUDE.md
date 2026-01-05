# CLAUDE.md

You are Java Developer who want to deploy application to GCP, the code was not touched for months. It has outdated libraries
and needs some restructure in order to deploy to cloud smoothly. It is preferred to use GCP managed service but in efficient
way - the costs should be small.

## Build Commands

```bash
# Build the project
./mvnw clean package

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=BottleCapServiceTests

# Run a single test method
./mvnw test -Dtest=BottleCapServiceTests#testMethodName

# Run the application
./mvnw spring-boot:run

# Start local PostgreSQL database (requires Docker)
docker-compose -f docker/docker-compose.yml up -d
```

## Architecture Overview

- This is a Spring Boot 4.0.0 REST API backend for a bottle cap collection system.
- Target deployment cloud is GCP
- It uses Vision API for image analysis to detect duplicate/similar bottle caps
- Images are stored in Cloud Storage
- Project uses Java 21
- Database is Firestore - previously it was PostgreSQL
- Solution chosen in GCP are have wisely to not exceed budget


### Layered Architecture

```
Controller → Service → Repository → Database
     ↓
    DTO ←→ Entity (via MapStruct)
```

### Key Components

- **controllers/**: REST endpoints - `BottleCapController` for CRUD
- **service/**: Business logic 
- **repository/**: repositories for data access
- **model/**: database entites
- **mapper/**: MapStruct mappers for entity ↔ DTO conversion
- **dto/**: Data transfer objects 

### Image Processing Pipeline

The system uses OpenCV to:
1. Calculate HSB (Hue, Saturation, Brightness) histograms for image
2. Upload file to Cloud Storage
3. Call Vision API to get embeddings and metadata
4. Based on HSB select amount of first stage similarity caps
5. Use selected caps to compare based on embeddings
6. Show similar caps

### Security Model

- Spring Security 
- GET endpoints are public
- POST/PUT/DELETE require ADMIN role
- Stateless session, Oauth2

### Database

- Firebase

### Configuration

- Application properties use `bcc` prefix (see `AppProperties` class)
- Caching enabled with "caps" cache name
- Async and scheduling enabled for admin operations

## Related Projects

- Frontend: React application (separate repository)
- Mobile: Android app (separate repository)
