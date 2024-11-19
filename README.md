# Metrics Publisher

## Description

Metrics Publisher is a Kotlin-based server application designed to collect metrics from continuous integration pipelines and publish them directly in GitHub Pull Requests. It streamlines the process of monitoring performance, quality, and other critical metrics by integrating them into the code review workflow.

The application accepts metrics via a RESTful API, processes and stores them, compares them with reference values, and updates the corresponding GitHub Pull Requests with a detailed metrics report.

## Quick Start

### Prerequisites

- **Java Development Kit (JDK)**: Ensure you have JDK 18 or higher installed.
- **PostgreSQL Database**: Set up a PostgreSQL database to store metrics.
- **GitHub Personal Access Token**: Obtain a GitHub token with the necessary permissions to read and write to repositories.

### Environment Variables

Set the following environment variables required by the application:

- `SECRET_HEADER`: A secret token used for authenticating API requests.
- `GH_REPO`: The GitHub repository in the format `owner/repo` (e.g., `domanskii/metrics-publisher`).
- `GH_TOKEN`: Your GitHub Personal Access Token.
- `GH_DEFAULT_BRANCH`: The default branch of your repository (e.g., `main` or `master`).
- `DB_HOST`: The hostname of your PostgreSQL database.
- `DB_NAME`: The name of your PostgreSQL database.
- `DB_USERNAME`: The username for your PostgreSQL database.
- `DB_PASSWORD`: The password for your PostgreSQL database.

### Running the Application

1. **Clone the Repository**:

   ```bash
   git clone https://github.com/DeniDoman/metrics-publisher.git
   cd metrics-publisher
   ```

2. **Set Environment Variables**:

   Export the required environment variables in your shell or set them in your IDE.

   ```bash
   export SECRET_HEADER=your_secret_header
   export GH_REPO=owner/repo
   export GH_TOKEN=your_github_token
   export GH_DEFAULT_BRANCH=main
   export DB_HOST=localhost
   export DB_NAME=metrics_db
   export DB_USERNAME=db_user
   export DB_PASSWORD=db_password
   ```

3. **Build the Application**:

   ```bash
   ./gradlew build
   ```

4. **Run the Application**:

   ```bash
   ./gradlew run
   ```

   The application will start and listen on `http://0.0.0.0:8080`.

## Usage Guide

### Submitting Metrics

Send a POST request to the `/api/v1/metrics` endpoint with your metric data.

- **Endpoint**: `POST /api/v1/metrics`
- **Headers**:
    - `Authorization`: `Bearer your_secret_header`
    - `Content-Type`: `application/json`
- **Body**:

  ```json
  {
  "commitSha": "your_commit_sha",
  "name": "metric_name",
  "value": 10.0,
  "units": "ms",
  "threshold": 5.0,
  "isIncreaseBad": true
  }
  ```
  
  - `commitSha`: (string) The SHA hash of the commit associated with the metric.
  - `name`: (string) Name of the metric (e.g., `"build_time"`).
  - `value`: (number) Numeric value of the metric.
  - `units`: (string) Units of the metric (e.g., `"ms"`, `"%"`).
  - `threshold`: (number) Threshold for significant changes that warrant attention.
  - `isIncreaseBad`: (boolean) Indicates if an increase in the metric value is considered negative.

#### Example using `curl`:

```bash
curl -X POST http://localhost:8080/api/v1/metrics 
-H "Authorization: Bearer your_secret_header" 
-H "Content-Type: application/json" 
-d '{
"commitSha": "abc123def456",
"name": "response_time",
"value": 120.5,
"units": "ms",
"threshold": 10.0,
"isIncreaseBad": true
}'
```

### How It Works

1. **Submit Metric**: A metric is submitted via the API.
2. **Store Metric**: The application stores the metric in the database.
3. **Determine Reference Commit**: The application checks if the commit is a reference commit (merged into the default branch).
    - If it is, the metric is stored as a reference, and processing stops here (Step 4 is skipped).
    - If not, the application retrieves the reference metric for comparison and continues processing.
4. **Update GitHub PR**: The application updates the corresponding GitHub Pull Request by inserting the comparison Markdown table into the PR body.

## Contribution Guidelines

### Project Structure

- `common`: Contains common data classes like `Metric` and `MetricDiff`.
- `services`: Business logic for processing metrics and generating markdown.
    - `MetricsService`: Handles metric processing and coordination between storage and VCS provider.
    - `MarkdownService`: Generates markdown tables from metrics.
- `providers`: VCS provider interface and implementations.
    - `VcsProvider`: Interface for VCS operations.
    - `GitHub`: Implementation of `VcsProvider` for GitHub.
- `storage`: Database interaction layer.
    - `Storage`: Interface defining storage operations.
    - `StorageImpl`: Implementation of `Storage` using Exposed and PostgreSQL.
    - `DatabaseFactory`: Initializes the database connection.
    - `Tables`: Defines the database schema.
- `plugins`: Ktor plugins configuration.
    - `Authentication.kt`: Configures authentication.
    - `Routing.kt`: Sets up HTTP routes.
    - `Serialization.kt`: Configures content negotiation and serialization.
- `serialization`: Data transfer objects for API requests and responses.
- `Application.kt`: The main entry point of the application.

### Extending the Project

To add support for another VCS provider:

1. **Implement the `VcsProvider` Interface**: Create a new class in the `providers` package that implements `VcsProvider`.
2. **Update Dependency Injection**: Modify `Application.kt` to use your new provider based on configuration.
3. **Add Any Necessary Configuration**: Include additional environment variables or configuration files as needed.

### Running Tests

The project includes unit and integration tests.

- **Run All Tests**:

  ```bash
  ./gradlew test
  ```

- Test classes are located in the `test` directory, mirroring the structure of the main source code.

## License

MIT License