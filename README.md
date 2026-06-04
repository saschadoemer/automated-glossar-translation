# Context-Based Glossary Translation

This project provides a Spring Boot-based service for translating glossary terms using Large Language Models (LLMs) like Google Gemini or OpenAI GPT-4. To ensure high-quality and consistent translations, the service uses a "Master Dictionary" to provide context and existing translations for similar terms.

## Features

- **LLM-Powered Translation**: Leverage Google Gemini or OpenAI for high-quality translations.
- **Context Enrichment**: Uses a master dictionary to provide the LLM with relevant context and existing translations.
- **Fuzzy Matching**: Optionally find similar terms in the master dictionary if an exact match is not found.
- **Asynchronous Processing**: Translation jobs run in the background, allowing for large glossary processing.
- **Excel Export**: Results are provided in a well-structured Excel format including confidence scores and synonyms.

## Prerequisites

- **Java 23**: The project uses the latest Java features.
- **Maven**: For building and managing dependencies.
- **API Keys**: You need an API key for either [Google Gemini](https://ai.google.dev/) or [OpenAI](https://platform.openai.com/).

## Configuration

The application can be configured via environment variables or by modifying `src/main/resources/application.yml`.

| Environment Variable | Description | Default |
|----------------------|-------------|---------|
| `GEMINI_API_KEY` | Your Google Gemini API key | -       |
| `GEMINI_MODEL_NAME` | Gemini model to use | -       |
| `OPENAI_API_KEY` | Your OpenAI API key | -       |
| `OPENAI_MODEL_NAME` | OpenAI model to use | -       |

## Getting Started

### Build the Application

```bash
mvn clean package
```

### Run Locally

```bash
java -jar target/context-based-glossar-translation-1.0-SNAPSHOT.jar
```

### Run with Docker

```bash
docker build -t glossar-translation .
docker run -p 8080:8080 -e GEMINI_API_KEY=your_key glossar-translation
```

---

## Translation Process

To perform a successful translation, follow these steps:

### Step 1: Upload the Master Dictionary

Before starting a translation, you must provide a master dictionary that serves as the context source.

- **Endpoint**: `POST /translation/master-dictionary/upload`
- **Payload**: `file` (Multipart file, Excel format)
- **Description**: This file should contain existing translations and identifiers. See [Data Formats](#data-formats) for details.

### Step 2: Start a Translation Job

Once the master dictionary is uploaded, you can start a translation job for your glossary.

- **Endpoint**: `POST /translation`
- **Parameters**:
    - `file`: The CSV file containing the terms to translate.
    - `targetLanguage`: The target language code (e.g., `en-US`, `fr-FR`).
    - `llmType`: `gemini` (default) or `openai`.
    - `fuzzy`: `true`/`false` (default) to enable fuzzy matching in the master dictionary.
    - `waitTime`: Optional wait time (in seconds) between LLM calls to avoid rate limits.
- **Response**: Returns a `jobId`.

### Step 3: Download the Results

Monitor the progress and download the final Excel file once completed.

- **Endpoint**: `GET /translation/result/{jobId}`
- **Response**: 
    - `202 Accepted`: If the job is still in progress (returns JSON with progress details).
    - `200 OK`: If the job is finished (returns the generated Excel file).

---

## Data Formats

### Master Dictionary (Excel)

The master dictionary must be an Excel file (`.xlsx`) with the following structure:
- **Header Row**: Should contain language codes (e.g., `de-DE`, `en-US`, `fr-FR`).
- **Column 1**: Unique identifier for the term.
- **Column 2**: German translation (used as the primary key/fallback).
- **Subsequent Columns**: Translations for the languages specified in the header.

### Glossary to Translate (CSV)

The glossary should be a simple CSV file where:
- **Column 1**: Contains the terms or identifiers you want to translate.

---

## API Documentation

The project includes OpenAPI documentation (Swagger UI), which you can access while the application is running:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:8080/v3/api-docs`
