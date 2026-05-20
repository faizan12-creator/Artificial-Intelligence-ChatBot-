package AIchatbot;



import java.util.*;

/**
 * ChatSession — stores title + message history for one conversation.
 */
public class ChatSession {
    public String title;
    public List<Map<String, String>> messages = new ArrayList<>();

    // Sahi Constructor (No void, and exact name match)
    public ChatSession(String title) {
        this.title = title;
    }

    public int messageCount() {
        return messages.size();
    }
}