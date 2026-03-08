# ReTeX
[![Watch the demo](resumer/src/main/resources/static/logo.png)]([https://www.youtube.com/watch?v=VIDEO_ID](https://youtu.be/SJlcgVJyo9k
)<br>
(Click image for demo video)

*Made for [**cmd-f 2026**] - 24-hour Hackathon*

**"Stop wrestling with margins. Start winning interviews."**

**ReTeX** is a full-stack web application designed for all job seekers who want to skip the "Word document struggle." It uses the Google Gemini 2.5 Flash model to parse job descriptions and your current resume, then auto-refine and compile cover letter and resume to matches the specific job description with additional job application advice.

## How to Run
### 1. Prerequisites
* **Java 17** or higher.
* **Maven** (for dependency management).
* A **Google AI Studio API Key** (Obtainable at [aistudio.google.com](https://aistudio.google.com/)).

### 2. Configuration
ReTeX uses environment variables to keep your credentials secure. Ensure the following is set in your environment or `application.properties`:

```bash
# Your Google Gemini API Key
export SPRING_AI_GOOGLE_GENAI_API_KEY=your_api_key_here
```
### 3. Installation & Launch
```bash
# Clone the repository
git clone [https://github.com/BellaChen/ReTeX.git](https://github.com/BellaChen/ReTeX.git)

# Navigate to the directory
cd ReTeX

# Build and run with Maven
mvn spring-boot:run
```

Access the app at [http://localhost:8080](http://localhost:8080)


## Tech Stack
* **Language:** Java 17
* **Framework:** Spring Boot 3.5.x (Java)
* **AI Engine:** Spring AI,1.1.2 (**Google Gemini** Integration)
* **Build Tool:** Maven, Handling all the .m2 repository dependencies
* **Frontend:** HTML5, CSS3, HTMX
* **Latex:** Overleaf
