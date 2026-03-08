package cmd.resumer;

import org.apache.tika.Tika;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    @Autowired
    private GoogleGenAiChatModel chatModel;

    @PostMapping("/refine")
    @ResponseBody
    public String refine(@RequestParam("file") MultipartFile file,
                         @RequestParam("jobDescription") String jobDescription) {
        try {
            // 1. Extract text from PDF/DOCX using Tika
            Tika tika = new Tika();
            String resumeText = tika.parseToString(file.getInputStream());

            //testing
            System.out.println("Extracted Resume Text: " + resumeText);
            // 2. The "Mega Prompt" for better results
            String prompt = """
            You are an expert career coach and a LaTeX typesetter.
                    TASK:
                    Refine the provided resume to match the job description.\s
                    1. Highlight skills that match the job requirements.
                    2. Use strong action verbs and a professional tone.
                    3. IMPORTANT: You must return the FULL, valid LaTeX document.
                    4. CRITICAL: Do NOT wrap the code in Markdown blocks (like ```latex).\s
                    5. CRITICAL: Your response MUST start exactly with the characters '\\\\documentclass'.\s
                    No spaces, no invisible characters, and no intro text.
            JOB DESCRIPTION:
            %s

            RESUME CONTENT:
            %s
            """.formatted(jobDescription, resumeText);

            // 3. Send to Gemini
            return chatModel.call(prompt);
//            return resumeText;

        } catch (Exception e) {
            return "Error: Could not read the file. Make sure it's a valid PDF or Word doc. " + e.getMessage();
        }
    }
}