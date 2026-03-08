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
            You are an expert career coach.
            I am providing a resume and a job description.
            The resume is given as a .tex LaTeX file. Crucial: You must return the entire document 
            as valid LaTeX code. Do not remove, alter, or break any of the formatting tags, macros, 
            or structural elements (like \\begin{itemize} or \\textbf).
            Please rewrite the resume to:
            1. Highlight skills that match the job description.
            2. Use strong action verbs.
            3. Keep the tone professional.
            Do not output any additional text other than the straight .tex file.

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