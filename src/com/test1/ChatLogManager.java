package com.test1;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ChatLogManager {
    private static Map<Integer, StringBuilder> chatLogs = new HashMap<>();

    public static void createChatLog(int roomIdx) {
        chatLogs.put(roomIdx, new StringBuilder());
    }

    public static void addMessage(int roomIdx, String message) {
        StringBuilder chatLog = chatLogs.get(roomIdx);
        if (chatLog != null) {
            chatLog.append(message).append("\n");
        }
    }

    public static void saveChatLog(int roomIdx) {
        StringBuilder chatLog = chatLogs.get(roomIdx);
        if (chatLog != null) {
            try (FileWriter writer = new FileWriter("room_" + roomIdx + ".txt", true)) {
                writer.write(chatLog.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            chatLogs.remove(roomIdx);
        }
    }
}
