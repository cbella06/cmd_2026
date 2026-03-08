package cmd.resumer;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Base64;

@RestController
public class ResumeController {

    @Autowired
    private GoogleGenAiChatModel chatModel;
    private final TextConverter textConverter = new TextConverter();

    @PostMapping("/api/resume/refine")
    @ResponseBody
    public String refine(@RequestParam("file") MultipartFile file,
                         @RequestParam("jobDescription") String jobDescription,
                         @RequestParam("tone") String tone) {

        String resumeText;

        try {
            // 1. Extract text from PDF/DOCX using Tika
            Tika tika = new Tika();
            resumeText = tika.parseToString(file.getInputStream());
        }
        catch (IOException | TikaException e) {
            return "Error: Could not read the file. Make sure it's a valid TeX, PDF, or Word doc. " + e.getMessage();
        }

        // 2. The "Mega Prompt" for better results
        String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"));

        String prompt = """
        You are a ruthless, expert technical recruiter and a master LaTeX typesetter. Your objective is to aggressively edit and optimize the provided resume to PERFECTLY match the job description. The tone must be %s.
        IMPORTANT OUTPUT FORMAT: You must return exactly THREE sections separated by the marker: ===SPLIT_HERE===
    
        SECTION 1: AI COACH NOTES
        FORMATTING. Use MD format. DO NOT INCLUDE ANYTHING OTHER THAN THE BULLET POINTS. NO SECTION TITLE. Provide 3 to 4 bullet points of specific, actionable advice. Tell the user what exact metrics, hardware tools, or technical specifics they need to manually add to improve their chances for this specific job description. Do NOT use LaTeX for this section. Use plain text bullet points.
        
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
                5. LENGTH & STRUCTURE: 250-450 words, maximum 5 short paragraphs (Intro, Body, Conclusion). You must separate each paragraph with a new line (vspace). Keep the original LaTeX formatting. Focus each body paragraph on exactly ONE specific skill or experience.\s
                6. HEADER & GREETING RULES: Use the date %s in the letterhead. You must STRICTLY extract the company name, office location, and recruiter name from the Job Description. If any of these details are missing, omit them entirely—DO NOT invent or guess them. Put a new line (vspace) after the letterhead                    
                7. IMPORTANT: You must return the FULL, valid LaTeX document.
                8. CRITICAL: Do NOT wrap the code in Markdown blocks (like ```latex).\s
                9. CRITICAL: Your response MUST start exactly with the characters '\\\\documentclass'.\s
                No spaces, no invisible characters, and no intro text.

        JOB DESCRIPTION:
        %s

        RESUME CONTENT:
        %s
        """.formatted(tone, tone, tone, today, jobDescription, resumeText);

        // 3. Send to Gemini
        String response;
        try {
             response = chatModel.call(prompt);
        } catch (Exception e) {
            // Log it to your terminal so you can debug it during the hackathon
            System.err.println("Refinement Pipeline Failed: " + e.getMessage());
            e.printStackTrace();

            // Return a styled error message to the frontend via HTMX
            return """
            <div style='background: #fff0f0; border-left: 5px solid #dc3545; padding: 20px; margin: 20px 0; border-radius: 4px; font-family: sans-serif;'>
                <h4 style='color: #dc3545; margin-top: 0;'>AI Processing Error</h4>
                <p style='color: #333;'>We couldn't generate your documents. The AI service might be overloaded or the file was unreadable.</p>
                <p style='font-size: 0.8em; color: #666; font-family: monospace;'>Developer Detail: %s</p>
            </div>
            """.formatted(e.getMessage());
        }

        // Print response to terminal for Testing
        System.out.print(response);

        // Split resume and cover letter
        String[] parts = response.split("===SPLIT_HERE===");
        String advice = parts[0].trim();
        String resumeLatex = parts.length > 1 ? parts[1].trim() : "";
        String coverLetterLatex = parts.length > 2 ? parts[2].trim() : "";


        // Convert to pdf
        byte[] resumePDF = textConverter.compileWithApi(resumeLatex);
        byte[] coverLetterPDF = textConverter.compileWithApi(coverLetterLatex);
        String formattedAdvice = textConverter.formatFeedbackToHtml(advice);

        String resumeBase64 = Base64.getEncoder().encodeToString(resumePDF);
        String coverLetterBase64 = Base64.getEncoder().encodeToString(coverLetterPDF);

        // We need to properly escape the LaTeX for a hidden HTML input value
        String finalResumeLatex = resumeLatex.replace("\"", "&quot;").replace("'", "&apos;");
        String finalCoverLetterLatex = coverLetterLatex.replace("\"", "&quot;").replace("'", "&apos;");

        // Inside your refine method...
        return """
        <div style='width: 100%%; animation: fadeIn 0.5s ease-in;'>
            <h4 style='color: #086528; text-align: center; margin-bottom: 30px;'>Documents Ready!</h4>
           \s
            <div style='background: #f0f9ff; border-left: 5px solid #3b82f6; padding: 15px; margin-bottom: 20px; border-radius: 4px;'>
                    <h4 style='margin-top: 0; color: #856404;'>💡 AI Coach Notes</h4>
                    %s
                </div>
           \s
            <div style='display: flex; gap: 30px; justify-content: center;'>
               \s
                <div style='flex: 1; min-width: 400px;'>
                    <h5 style='text-align: center;'>Tailored Cover Letter</h5>
                    <iframe src='data:application/pdf;base64,%s'\s
                            style='width:100%%; height:750px; border:none; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.05);'>
                    </iframe>
                    <div style='margin-top: 15px; padding: 15px; background: #f8f9fa; border-radius: 8px; text-align: center;'>
                        <form action="https://www.overleaf.com/docs" method="POST" target="_blank">
                            <input type="hidden" name="snip" value="%s">
                            <button type="submit" style="background: #086528; color: white; border: none; padding: 12px 20px; border-radius: 6px; cursor: pointer; font-weight: bold; width: 100%%;">
                             Open Letter in Overleaf
                             </button>
                        </form>
                    </div>
                </div>
       \s
                <div style='flex: 1; min-width: 400px;'>
                    <h5 style='text-align: center;'>Refined Resume</h5>
                    <iframe src='data:application/pdf;base64,%s'\s
                            style='width:100%%; height:750px; border:none; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.05);'>
                    </iframe>
                    <div style='margin-top: 15px; padding: 15px; background: #f8f9fa; border-radius: 8px; text-align: center;'>
                        <form action="https://www.overleaf.com/docs" method="POST" target="_blank">
                            <input type="hidden" name="snip" value="%s">
                            <button type="submit" style="background: #086528; color: white; border: none; padding: 12px 20px; border-radius: 6px; cursor: pointer; font-weight: bold; width: 100%%;">
                                Open Resume in Overleaf
                            </button>
                        </form>
                    </div>
                </div>
               \s
            </div>
        </div>
       \s""".formatted(formattedAdvice, coverLetterBase64, finalCoverLetterLatex, resumeBase64, finalResumeLatex);
    }
}