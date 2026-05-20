package AIchatbot;



import java.net.URI;
import java.net.http.*;
import java.util.*;

/**
 * APIService — handles all Groq HTTP calls and JSON parsing.
 */
public class APIService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final HttpClient client = HttpClient.newHttpClient();

    // ── Last raw response (for token extraction) ──────────────
    private String lastRawResponse = "";

    // ── Main call ─────────────────────────────────────────────
    public String call(String apiKey,
                       String model,
                       double temperature,
                       String systemPrompt,
                       List<Map<String, String>> history) throws Exception {

        StringBuilder msgs = new StringBuilder("[");
        msgs.append("{\"role\":\"system\",\"content\":").append(jsonString(systemPrompt)).append("}");

        int start = Math.max(0, history.size() - 20);
        for (int i = start; i < history.size(); i++) {
            msgs.append(",{\"role\":\"").append(history.get(i).get("role"))
                    .append("\",\"content\":").append(jsonString(history.get(i).get("content")))
                    .append("}");
        }
        msgs.append("]");

        String body = "{\"model\":\"" + model + "\","
                + "\"max_tokens\":2048,"
                + "\"temperature\":" + temperature + ","
                + "\"messages\":" + msgs + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type",  "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        lastRawResponse = res.body();

        if (res.statusCode() != 200) {
            String err = extractJsonValue(lastRawResponse, "message");
            throw new RuntimeException(err.isEmpty() ? "HTTP " + res.statusCode() : err);
        }

        return extractContent(lastRawResponse);
    }

    /** Returns token count from the last API response. */
    public int getLastTokenCount() {
        String tok = extractJsonValue(lastRawResponse, "total_tokens");
        if (tok.isEmpty()) return 0;
        try { return Integer.parseInt(tok.trim()); } catch (NumberFormatException e) { return 0; }
    }

    // ── JSON helpers (static so other classes can use them) ───

    public static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    public static String extractJsonValue(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return "";
        int colon = json.indexOf(":", idx);
        if (colon == -1) return "";
        String rest = json.substring(colon + 1).trim();

        if (rest.startsWith("\"")) {
            StringBuilder sb = new StringBuilder();
            int i = 1;
            while (i < rest.length()) {
                char c = rest.charAt(i);
                if (c == '\\' && i + 1 < rest.length()) {
                    char n = rest.charAt(i + 1);
                    switch (n) {
                        case 'n'  -> { sb.append('\n'); i += 2; }
                        case 't'  -> { sb.append('\t'); i += 2; }
                        case '"'  -> { sb.append('"');  i += 2; }
                        case '\\' -> { sb.append('\\'); i += 2; }
                        case 'r'  -> { sb.append('\r'); i += 2; }
                        default   -> { sb.append(n);    i += 2; }
                    }
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c); i++;
                }
            }
            return sb.toString();
        }

        // number value
        int end = 0;
        while (end < rest.length()
                && (Character.isDigit(rest.charAt(end)) || rest.charAt(end) == '.')) {
            end++;
        }
        return rest.substring(0, end);
    }

    public static String extractContent(String json) {
        int ci = json.indexOf("\"choices\"");
        if (ci == -1) return "No response.";
        String after = json.substring(ci);
        int cIdx = after.indexOf("\"content\"");
        if (cIdx == -1) return "No content.";
        return extractJsonValue(after.substring(cIdx), "content");
    }
}