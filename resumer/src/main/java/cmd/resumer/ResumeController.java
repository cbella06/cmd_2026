package cmd.resumer;

import org.apache.tika.Tika;
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


    @PostMapping("/api/resume/refine")
    @ResponseBody
    public String refine(@RequestParam("file") MultipartFile file,
                         @RequestParam("jobDescription") String jobDescription,
                         @RequestParam("tone") String tone) {
        try {
            // 1. Extract text from PDF/DOCX using Tika
            Tika tika = new Tika();
            String resumeText = tika.parseToString(file.getInputStream());

            //testing
//            System.out.println("Extracted Resume Text: " + resumeText);
            // 2. The "Mega Prompt" for better results
            String prompt = """
            You are a ruthless, expert technical recruiter and a master LaTeX typesetter. Your objective is to aggressively edit and optimize the provided resume to PERFECTLY match the job description. The tone must be %s.
            IMPORTANT: You must generate two separate LaTeX documents. Start the Resume with 
            \\\\documentclass and end it with \\\\end{document}. Then, add the marker: ===SPLIT_HERE===
            Then, start the Cover Letter with \\\\documentclass and end it with \\\\end{document}.
                    TASK 1: RESUME REFINEMENT
                    Refine the provided resume to match the job description using a %s tone.\s
                    --- STRICT STRUCTURAL RULES ---
                    1. PRESERVE THE TEMPLATE: Do NOT change the LaTeX preamble, custom commands, document class, or the overall section structure. You are a content editor, not a template designer.
                    2. SURGICAL EDITS ONLY: Only modify the text content (bullet points, and the order of items) strictly within the existing sections.
                   
                    --- CONTENT CURATION RULES ---
                    3. STRATEGIC REORDERING: Within the existing sections (e.g., Projects, Experience), you MUST change the physical order entries in the LaTeX code. The most relevant experiences to the top of their respective lists based on the Job Description.
                    4. PRUNE THE IRRELEVANT: you MUST identify and completely remove 1 to 2 bullet points per project/role if they are entirely irrelevant to the job description, Delete the lines entirely
                    5. ENHANCE THE RELEVANT: Rewrite the remaining bullets to highlight matching skills. You must upgrade the vocabulary if there is a stronger alternative. Use the 'Action verb + task/project + outcome' format with strong action verbs. Highlight a variety of skills across the resume rather highlighting the same skills
                    
                    --- OUTPUT RULES ---
                    6. IMPORTANT: You must return the FULL, valid LaTeX document.
                    7. CRITICAL: Do NOT wrap the code in Markdown blocks (like ```latex).\s
                    8. CRITICAL: Your response MUST start exactly with the characters '\\\\documentclass'.\s
                    No spaces, no invisible characters, and no intro text.
                    
                    TASK 2, COVER LETTER:
                    Write a highly conversational, pragmatic and human sounding cover letter. Use the provided resume to create a tailored cover letter that match the job description using a %s tone.\s
                    1. THE PERSONA: Adopt the persona of the based on their resume (e.g., student, junior developer, experienced manager). Write confidently and practically. Do NOT grovel, flatter the company extensively, or sound overly enthusiastic. Be direct, professional, and grounded.
                    2. BANNED WORDS: You are strictly forbidden from using the following words: profound, groundbreaking, thrilled, delve, testament, perfectly align, furthermore, showcased, deeply resonates, innovative, synergy, or dynamic. 
                    3. THE "ONE STORY" RULE: Do not just list skills or summarize the resume. Analyze the resume and pick ONE OR TWO highly relevant project, job experience, or achievement that best matches the job description. Briefly describe the technical/business challenge solved and how that specific experience translates to the employer's needs.                    
                    4. SHOW, DON'T TELL: Instead of claiming a skill (e.g., "I have a strong foundation in Python"), describe the actual system or process built using that skill.
                    5. LENGTH & STRUCTURE: Maximum 5 short paragraphs. Follow the intro, body conclusion format with one specific skill or experience per body paragraph. It should be between 250 and 450 words. If a company name or hiring manager is mentioned in the Job Description, use it in the greeting.                    
                    6. IMPORTANT: You must return the FULL, valid LaTeX document.
                    7. CRITICAL: Do NOT wrap the code in Markdown blocks (like ```latex).\s
                    8. CRITICAL: Your response MUST start exactly with the characters '\\\\documentclass'.\s
                    No spaces, no invisible characters, and no intro text.
                                
            JOB DESCRIPTION:
            %s

            RESUME CONTENT:
            %s
            """.formatted(tone, tone, tone, jobDescription, resumeText);

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

            // We need to properly escape the LaTeX for a hidden HTML input value
            String finalResumeLatex = resumeLatex.replace("\"", "&quot;").replace("'", "&apos;");
            String finalCoverLetterLatex = coverLetterLatex.replace("\"", "&quot;").replace("'", "&apos;");

            // Inside your refine method...
            return """
<div style='width: 100%%; animation: fadeIn 0.5s ease-in;'>
    <h4 style='color: #28a745; text-align: center; margin-bottom: 30px;'>✨ Documents Ready</h4>
    
    <div style='display: flex; gap: 30px; justify-content: center;'>
        
        <div style='flex: 1; min-width: 400px;'>
            <h5 style='text-align: center;'>✉️ Tailored Cover Letter</h5>
            <iframe src='data:application/pdf;base64,%s' 
                    style='width:100%%; height:750px; border:none; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.05);'>
            </iframe>
            <div style='margin-top: 15px; padding: 15px; background: #f8f9fa; border-radius: 8px; text-align: center;'>
                <form action="https://www.overleaf.com/docs" method="POST" target="_blank">
                    <input type="hidden" name="snip" value="%s">
                    <button type="submit" style="background: #47a141; color: white; border: none; padding: 12px 20px; border-radius: 6px; cursor: pointer; font-weight: bold; width: 100%%;">
                     Open Letter in Overleaf
                     </button>
                </form>
            </div>
        </div>

        <div style='flex: 1; min-width: 400px;'>
            <h5 style='text-align: center;'>📄 Refined Resume</h5>
            <iframe src='data:application/pdf;base64,%s' 
                    style='width:100%%; height:750px; border:none; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.05);'>
            </iframe>
            <div style='margin-top: 15px; padding: 15px; background: #f8f9fa; border-radius: 8px; text-align: center;'>
                <form action="https://www.overleaf.com/docs" method="POST" target="_blank">
                    <input type="hidden" name="snip" value="%s">
                    <button type="submit" style="background: #47a141; color: white; border: none; padding: 12px 20px; border-radius: 6px; cursor: pointer; font-weight: bold; width: 100%%;">
                        Open Resume in Overleaf
                    </button>
                </form>
            </div>
        </div>
        
    </div>
</div>
""".formatted(coverLetterBase64, finalCoverLetterLatex, resumeBase64, finalResumeLatex);

        } catch (Exception e) {
            return "Error: Could not read the file. Make sure it's a valid PDF or Word doc. " + e.getMessage();
        }
    }
}