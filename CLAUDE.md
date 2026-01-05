# CLAUDE.md

You are Java Developer who want to deploy application to GCP, the code was not touched for months. It has outdated libraries
and needs some restructure in order to deploy to cloud smoothly. It is preferred to use GCP managed service but in efficient
way - the costs should be small.

## Architecture Overview

- do not use wildcards

## Build Commands

```bash
# Build the project
./mvnw clean package
```

## Architecture Overview

- This is a Spring Boot 4.0.0 REST API backend for a items collection system.
- Target deployment cloud is GCP
- It uses Vision API for image analysis to detect duplicate/similar items
- Images are stored in Cloud Storage
- Project uses Java 21
- Database is Firestore - previously it was PostgreSQL
- Solution chosen in GCP are wisely to not exceed budget


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

- Frontend: Legacy React application (separate repository)
- Mobile: Legacy Android app (separate repository) 
- Frontend: Angular app (separate repository) - this app would take over functionalities of mobile app
