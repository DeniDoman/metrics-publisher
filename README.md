# Metrics Publisher

## Description

Metrics Publisher is a Kotlin-based server application designed to collect metrics from continuous integration pipelines and publish them directly in GitHub Pull Requests. It streamlines the process of monitoring performance, quality, and other critical metrics by integrating them into the code review workflow.

The application accepts metrics via a RESTful API, processes and stores them, compares them with reference values, and updates the corresponding GitHub Pull Requests with a detailed metrics report.

## Quick Start

### Prerequisites

- **Java Development Kit (JDK) 18** or higher
- **PostgreSQL** database
- **GitHub Personal Access Token** with appropriate permissions (repo access)

### Installation

1. **Clone the Repository**

       git clone https://github.com/DeniDoman/metrics-publisher.git
       cd metrics-publisher

2. **Set Up the PostgreSQL Database**

    - Create a new PostgreSQL database.
    - Note the database name, username, password, and host.

3. **Configure Environment Variables**

   Set the following environment variables required by the application:

    - `SECRET_HEADER`: A secret token used for API authentication.
    - `GH_REPO`: GitHub repository in the format `owner/repo` (e.g., `yourusername/yourrepo`).
    - `GH_TOKEN`: GitHub personal access token.
    - `GH_DEFAULT_BRANCH`: The default branch of your repository (e.g., `main`).
    - `DB_HOST`: Database host address (e.g., `localhost`).
    - `DB_NAME`: Name of the PostgreSQL database.
    - `DB_USERNAME`: Database username.
    - `DB_PASSWORD`: Database password.

   You can export these variables in your shell or include them in a `.env` file if using a tool like `dotenv`.

4. **Build the Application**

       ./gradlew build

5. **Run the Application**

       ./gradlew run

   The server will start and listen on `http://0.0.0.0:8080`.

## Usage Guide

### Sending Metrics

Metrics are sent to the application via a POST request to the `/api/v1/metrics` endpoint.

#### Endpoint Details

- **URL**: `http://localhost:8080/api/v1/metrics`
- **Method**: `POST`
- **Headers**:
    - `Authorization`: `Bearer <SECRET_HEADER>`
    - `Content-Type`: `application/json`
- **Request Body**:

       {
         "commitSha": "string",
         "name": "string",
         "value": number,
         "units": "string",
         "threshold": number,
         "isIncreaseBad": boolean
       }

    - `commitSha`: (string) The SHA hash of the commit associated with the metric.
    - `name`: (string) Name of the metric (e.g., `"build_time"`).
    - `value`: (number) Numeric value of the metric.
    - `units`: (string) Units of the metric (e.g., `"ms"`, `"%"`).
    - `threshold`: (number) Threshold for significant changes that warrant attention.
    - `isIncreaseBad`: (boolean) Indicates if an increase in the metric value is considered negative.

#### Example Request

       curl -X POST http://localhost:8080/api/v1/metrics \
         -H "Authorization: Bearer your-secret-token" \
         -H "Content-Type: application/json" \
         -d '{
           "commitSha": "abc123def456ghi789",
           "name": "build_time",
           "value": 120.5,
           "units": "seconds",
           "threshold": 5.0,
           "isIncreaseBad": true
         }'

### GitHub Pull Request Integration

To integrate metrics into your GitHub Pull Requests:

1. **Add a Placeholder in Your PR Template**

   Include the following placeholder in your GitHub Pull Request template or in the PR description:

       <!-- PR-METRICS-PUBLISHER:START -->

   The application uses this placeholder to insert the generated metrics report.

2. **Ensure Proper Permissions**

   The GitHub token provided must have permissions to read and write to Pull Requests in the repository.

### Health Check

To verify that the application is running correctly, you can send a GET request to the health check endpoint:

- **URL**: `http://localhost:8080/healthz`
- **Method**: `GET`

## Contribution Guidelines

### Project Structure

- **`main/kotlin/com/domanskii/`**: Main application source code.
    - **`Application.kt`**: Entry point of the application.
    - **`plugins/`**: Configuration for authentication, routing, and serialization using Ktor plugins.
    - **`providers/`**: Implementations for version control system (VCS) providers (e.g., GitHub).
    - **`services/`**: Core business logic, including metrics processing and markdown generation.
    - **`storage/`**: Database interaction layer using Exposed SQL library.
    - **`common/`**: Common data classes and utilities.
    - **`serialization/`**: Data classes for request and response serialization.

- **`test/kotlin/com/domanskii/`**: Test suites for unit and integration testing.

### Extending the Application

#### Adding Support for a New VCS Provider

1. **Implement the `VcsProvider` Interface**

   Create a new class in `providers/` that implements the `VcsProvider` interface, providing methods for checking reference commits and publishing metrics.

2. **Update Dependency Injection**

   Modify the application initialization in `Application.kt` to use your new VCS provider.

#### Enhancing Metrics Processing

1. **Modify `MetricsService`**

   Update or extend the `MetricsService` class to include additional processing logic or metrics analysis.

2. **Update `MarkdownService`**

   Customize the `MarkdownService` to alter how metrics are presented in the GitHub Pull Requests.

### Running Tests

- Execute all tests:

       ./gradlew test

- Test reports can be found in the `build/reports/tests/` directory after execution.

## License

MIT License