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
    public String refine(@RequestParam("file") MultipartFile file,
                         @RequestParam("jobDescription") String jobDescription) {

        // 1. We will extract text from the file here later
        // 2. We will send it to Gemini
        // 3. We will return the result
        return "System is ready! Received file: " + file.getOriginalFilename();
    }
}