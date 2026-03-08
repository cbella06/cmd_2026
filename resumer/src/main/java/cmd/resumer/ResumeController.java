package cmd.resumer;

import org.apache.tika.Tika;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Base64;

@RestController
public class ResumeController {

    @Autowired
    private GoogleGenAiChatModel chatModel;
    private final TexToPDF texToPDF = new TexToPDF();

    @PostMapping("/refine")
    @ResponseBody
    public String refine(@RequestParam("file") MultipartFile file,
                         @RequestParam("jobDescription") String jobDescription) {
        try {
            // 1. Extract text from PDF/DOCX using Tika
            Tika tika = new Tika();
            String resumeText = tika.parseToString(file.getInputStream());

            //testing
//            System.out.println("Extracted Resume Text: " + resumeText);
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
            System.out.println("Converting...");
            String response = chatModel.call(prompt);

            // Testing
            System.out.print(response);

            // Convert to pdf
            byte[] pdfBytes = texToPDF.compileWithApi(response);
//          byte[] pdfBytes = texToPDF.compileWithApi(resumeText);

            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

// Escape the response for the HTML attribute to prevent "quote" breaking
            String escapedLatex = response.replace("\"", "&quot;").replace("'", "&apos;");

            return """
    <div>
        <h4 style='color: #28a745;'>✨ Refinement Complete!</h4>
        
        <iframe src='data:application/pdf;base64,%s' 
                style='width:100%%; height:600px; border:1px solid #eee; border-radius: 4px;'>
        </iframe>
        
        <div style='margin-top: 20px; padding: 15px; background: #f8f9fa; border-radius: 8px; text-align: center;'>
            <p style='margin-bottom: 10px; font-size: 0.9em; color: #555;'>
                Want to make manual tweaks or download the source?
            </p>
            <form action="https://www.overleaf.com/docs" method="POST" target="_blank">
                <input type="hidden" name="snip" value="%s">
                <button type="submit" style="background: #47a141; color: white; border: none; padding: 12px 25px; border-radius: 5px; cursor: pointer; font-weight: bold;">
                    🚀 Open in Overleaf (Get PDF/Edit)
                </button>
            </form>
        </div>
    </div>
    """.formatted(base64Pdf, escapedLatex);

        } catch (Exception e) {
            return "Error: Could not read the file. Make sure it's a valid PDF or Word doc. " + e.getMessage();
        }
    }
}