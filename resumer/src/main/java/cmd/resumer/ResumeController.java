package cmd.resumer;

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
            // 1. Convert the uploaded file into plain text
            // (For now, we'll assume it's a simple text file or use a helper)
            String resumeContent = new String(file.getBytes());

            // 2. Create the "AI Instructions" (The Prompt)
            String prompt = "I have a resume and a job description. " +
                    "Please rewrite the resume to highlight the most relevant skills " +
                    "for this specific job. Keep it professional.\n\n" +
                    "RESUME:\n" + resumeContent + "\n\n" +
                    "JOB DESCRIPTION:\n" + jobDescription;

            // 3. Send it to Gemini and return the AI's response
            return chatModel.call(prompt);

        } catch (Exception e) {
            return "Error processing resume: " + e.getMessage();
        }
    }
}