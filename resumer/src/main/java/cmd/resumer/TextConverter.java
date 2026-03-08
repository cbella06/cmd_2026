package cmd.resumer;

import org.json.JSONObject;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

public class TextConverter {
    public byte[] compileWithApi(String latexCode) {
        String apiUrl = "https://latex.ytotech.com/builds/sync";
        RestTemplate restTemplate = new RestTemplate();

        // The API expects a JSON body with the compiler and the content
        String jsonPayload = String.format("{\"compiler\": \"pdflatex\", \"resources\": [{\"main\": true, \"content\": %s}]}",
            JSONObject.quote(latexCode));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

        // This returns the raw PDF bytes directly
        return restTemplate.postForObject(apiUrl, entity, byte[].class);
    }
    public String formatFeedbackToHtml(String feedbackText) {
        if (feedbackText == null || feedbackText.trim().isEmpty()) {
            return "<p style='margin: 0;'>No additional feedback provided.</p>";
        }

        // 1. ESCAPE HTML FIRST
        String safeText = feedbackText
            .replace("<", "&lt;")
            .replace(">", "&gt;");

        // 2. Clean the raw formatting
        String cleanFeedback = safeText
            .replace("SECTION 1: AI COACH NOTES", "")
            .replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>")
            .replaceAll("`(.*?)`", "<code style='background:#ffeeba; padding:2px 5px; border-radius:3px; color:#856404;'>$1</code>")
            .replace("\\%", "%")
            .trim();

        // 3. Tighter UL styling (removed default browser top/bottom gaps)
        StringBuilder formattedFeedback = new StringBuilder("<ul style='line-height: 1.5; color: #444; margin-top: 5px; margin-bottom: 0; padding-left: 20px;'>\n");

        boolean inList = false;

        for (String line : cleanFeedback.split("\\r?\\n")) {
            line = line.trim();

            // Skip completely blank lines to kill extra white space
            if (line.isEmpty()) continue;

            if (line.startsWith("*") || line.startsWith("-")) {
                if (inList) {
                    formattedFeedback.append("</li>\n"); // Close the previous bullet
                }
                // Reduced bottom margin from 8px to 6px
                formattedFeedback.append("  <li style='margin-bottom: 6px;'>")
                    .append(line.substring(1).trim());
                inList = true;
            } else {
                if (inList) {
                    // If the AI wrapped a sentence to a new line, append it to the current bullet!
                    formattedFeedback.append(" ").append(line);
                } else {
                    // If it's introductory text before the bullets, use a tight <p> tag
                    formattedFeedback.append("  <p style='margin: 0 0 6px 0;'>").append(line).append("</p>\n");
                }
            }
        }

        if (inList) {
            formattedFeedback.append("</li>\n"); // Close the final bullet
        }

        formattedFeedback.append("</ul>");

        return formattedFeedback.toString();
    }
}
