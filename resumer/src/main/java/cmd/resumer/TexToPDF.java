package cmd.resumer;

import org.json.JSONObject;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

public class TexToPDF {
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
}
