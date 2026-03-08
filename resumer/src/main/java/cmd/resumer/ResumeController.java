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
            IMPORTANT: You must generate two separate LaTeX documents. Start the Resume with 
            \\\\documentclass and end it with \\\\end{document}. Then, add the marker: ===SPLIT_HERE===
            Then, start the Cover Letter with \\\\documentclass and end it with \\\\end{document}.
                    TASK 1 RESUME:
                    Refine the provided resume to match the job description.\s
                    1. Highlight skills that match the job requirements.
                    2. Use strong action verbs and a professional tone.
                    3. IMPORTANT: You must return the FULL, valid LaTeX document.
                    4. CRITICAL: Do NOT wrap the code in Markdown blocks (like ```latex).\s
                    5. CRITICAL: Your response MUST start exactly with the characters '\\\\documentclass'.\s
                    No spaces, no invisible characters, and no intro text.
                    
                    TASK 2, COVER LETTER:
                    Use the provided resume to create a tailored cover letter that match the job description.\s
                    1. Highlight skills that match the job requirements.
                    2. Use strong action verbs and a professional tone while keeping the original voice
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

            // Split resume and cover letter
            String[] parts = response.split("===SPLIT_HERE===");
            String resumeLatex = parts[0].trim();
            String coverLetterLatex = parts.length > 1 ? parts[1].trim() : "";

            // Convert to pdf
            byte[] resumePDF = texToPDF.compileWithApi(resumeLatex);
            byte[] coverLetterPDF = texToPDF.compileWithApi(coverLetterLatex);

            String resumeBase64 = Base64.getEncoder().encodeToString(resumePDF);
            String coverLetterBase64 = Base64.getEncoder().encodeToString(coverLetterPDF);

            // Escape the response for the HTML attribute to prevent "quote" breaking
            String escapedResume = response.replace("\"", "&quot;").replace("'", "&apos;");
            String escapedCoverLetter = response.replace("\"", "&quot;").replace("'", "&apos;");

            return """
    <div>
        <h4 style='color: #28a745; text-align: center;'>✨ Refinement Complete!</h4>
        
        <div style='display: flex; gap: 20px; align-items: flex-start;'>
            
            <div style='flex: 1;'>
                <h5 style='text-align: center;'>📄 Refined Resume</h5>
                <iframe src='data:application/pdf;base64,%s' 
                        style='width:100%%; height:600px; border:1px solid #eee; border-radius: 4px;'>
                </iframe>
                
                <div style='margin-top: 15px; padding: 10px; background: #f8f9fa; border-radius: 8px; text-align: center;'>
                    <form action="https://www.overleaf.com/docs" method="POST" target="_blank" onsubmit="this.snip.value = atob('%s')">
                        <input type="hidden" name="snip" value="">
                        <button type="submit" style="background: #47a141; color: white; border: none; padding: 10px 15px; border-radius: 5px; cursor: pointer; font-weight: bold; font-size: 0.9em;">
                            🚀 Open Resume in Overleaf
                        </button>
                    </form>
                </div>
            </div>

            <div style='flex: 1;'>
                <h5 style='text-align: center;'>✉️ Cover Letter</h5>
                <iframe src='data:application/pdf;base64,%s' 
                        style='width:100%%; height:600px; border:1px solid #eee; border-radius: 4px;'>
                </iframe>
                
                <div style='margin-top: 15px; padding: 10px; background: #f8f9fa; border-radius: 8px; text-align: center;'>
                    <form action="https://www.overleaf.com/docs" method="POST" target="_blank" onsubmit="this.snip.value = atob('%s')">
                        <input type="hidden" name="snip" value="">
                        <button type="submit" style="background: #007bff; color: white; border: none; padding: 10px 15px; border-radius: 5px; cursor: pointer; font-weight: bold; font-size: 0.9em;">
                            🚀 Open Letter in Overleaf
                        </button>
                    </form>
                </div>
            </div>
            
        </div>
    </div>
    """.formatted(resumeBase64, escapedResume, coverLetterBase64, escapedCoverLetter);

        } catch (Exception e) {
            return "Error: Could not read the file. Make sure it's a valid PDF or Word doc. " + e.getMessage();
        }
    }
}