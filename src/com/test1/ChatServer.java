package com.test1;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class ChatServer {

    // < Room idx , Message / response >
    private static Map<Integer, List<PrintWriter>> chatRooms = new HashMap<>();
    // < 사용자 이름 , Room idx >
    private static Map<String, Integer> userList = new HashMap<>();
    // 고유 room idx를 생성하기 위한 카운터
    private static int nextroomIdx = 1;


    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("server is rdy.");

            while (true) {
                Socket socket = serverSocket.accept();
                new ChatThread(socket).start();
            }

        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    /**
     * 지정된 대화방의 모든 클라이언트에게 메시지를 브로드캐스트합니다.
     * @param roomIdx 채팅방 위치
     * @param message 사용자가 입력한 메세지
     */
    static void broadcast(int roomIdx, String message) {
        List<PrintWriter> clients = chatRooms.get(roomIdx);
        System.out.println(clients);
        synchronized (chatRooms) {
            if (clients != null) {
                for (PrintWriter client : clients) {
                    System.out.println(client);
                    client.println(message);
                }
            }
        }
    }

    static void removeClient(int roomIdx, PrintWriter client) {
        List<PrintWriter> clients = chatRooms.get(roomIdx);
        if (clients != null) {
            clients.remove(client);
            if (clients.isEmpty()) {
                chatRooms.remove(roomIdx);
                broadcast(-1, "Room " + roomIdx + " 가 삭제 되었습니다.");
                ChatLogManager.saveChatLog(roomIdx);
            }
        }
    }

    /**
     * 클라이언트와의 통신을 처리하기 위해 Thread를 상속받은 Class.
     */
    static class ChatThread extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String User;
        private int roomIdx = -1;

        /**
         * Constructor for initializing.
         * @param socket 클라이언트와 연결된 소켓.
         */
        public ChatThread(Socket socket) {
            this.socket = socket;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                commandHelp();
                User = in.readLine();

                if (userList.containsKey(User)) {
                    out.println("이미 존재하는 닉네임이니 다른 닉네임을 입력하세요.");
                    User = in.readLine();
                }

                // -1 == lobby
                userList.put(User, -1);
                out.println(User + "가 연결되었습니다." + socket.getInetAddress());
                out.println("Server에 오신것을 환영합니다. 도움이 필요하다면 /help 를 입력하세요.");
                broadcast(-1, User + "가 로비에 접속했습니다.");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        @Override
        public void run() {
            System.out.println(User + " 사용자 채팅시작!!");
            String message;
            try {
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/")) {
                        handleCommand(message);
                    } else {
                        broadcast(roomIdx, User + ": " + message);
                        ChatLogManager.addMessage(roomIdx, User + ": " + message);
                    }
                }
            } catch (IOException e) {
                // 클라이언트가 연결을 종료했을 때 예외 발생
                System.out.println(User + " 사용자가 연결을 종료했습니다.");
            } finally {
                removeClient(roomIdx, out);
                userList.remove(User);
                broadcast(-1, User + " 가 서버를 나갔습니다.");
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleCommand(String command) {
            String[] parts = command.split(" ");
            switch (parts[0]) {
                case "/help":
                    commandHelp();
                    break;
                case "/list":
                    listRooms();
                    break;
                case "/create":
                    int userLocation = userList.get(User);
                    if (userLocation == -1) { // 로비에 있을 때만 방 생성 가능
                        createRoom();
                    } else {
                        out.println("다른 방 안에 있는 동안에는 방을 만들 수 없습니다.");
                    }
                    break;
                case "/join":
                    if (parts.length == 2) {
                        joinRoom(Integer.parseInt(parts[1]));
                    } else {
                        out.println("Usage: /join [room number]");
                    }
                    break;
                case "/exit":
                    userLocation = userList.get(User);
                    if (userLocation == -1) { // 로비에 있을 때는 /exit 명령 불가
                        out.println("당신은 현재 로비에 있습니다.");
                    } else {
                        leaveRoom();
                    }
                    break;
                case "/save":
                    saveChat();
                    break;
                case "/users":
                    listUsers();
                    break;
                case "/roomusers":
                    listRoomUsers();
                    break;
                case "/whisper":
                    if (parts.length >= 3) {
                        sendWhisper(parts);
                    } else {
                        out.println("Usage: /whisper [nickname] [message]");
                    }
                    break;
                case "/bye":
                    out.println("Goodbye!");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    out.println("유효하지 않은 명령입니다. /help 을 입력하여 명령어를 확인해주세요.");
                    break;
            }
        }

        /**  /help 명령어 출력 */
        private void commandHelp() {
            out.println("Available commands:");
            out.println("/list - 활성화된 채팅방 목록 표시");
            out.println("/create - 새로운 대화방 생성");
            out.println("/join [room number] - 대화방 입장");
            out.println("/exit - 접속중인 대화방을 빠져 나옵니다");
            out.println("/users - 서버에 접속한 전체 유저를 보여줍니다");
            out.println("/roomusers - 현재 속한 대화방의 유저들을 보여줍니다");
            out.println("/whisper [nickname] [message] - 해당 유저에게 귓속말을 전송합니다");
            out.println("/save - 그간 대화한 내역들을 txt로 저장합니다");
            out.println("/bye - 서버와 통신을 종료합니다.");
        }

        private void listRooms() {
            out.println("Available rooms:");
            for (int roomNumber : chatRooms.keySet()) {
                out.println("Room " + roomNumber + " (" + chatRooms.get(roomNumber).size() + " users)");
            }
        }

        private void createRoom() {
            int roomNumber = nextroomIdx++;
            chatRooms.put(roomNumber, new ArrayList<>());
            ChatLogManager.createChatLog(roomNumber);
            joinRoom(roomNumber);
            broadcast(-1, User + " 가 " + roomNumber + "번 방을 생성했습니다.");
        }

        private void joinRoom(int roomNumber) {
            int previousRoom = userList.get(User);
            if (previousRoom != -1) {
                leaveRoom(); // 기존 방에서 나오기
            }
            List<PrintWriter> clients = chatRooms.get(roomNumber);
            if (clients != null) {
                clients.add(out);
                userList.put(User, roomNumber); // 유저의 위치를 새로운 방으로 변경
                roomIdx = roomNumber;
                broadcast(roomIdx, User + " 가 " + roomNumber + "번 방에 입장했습니다.");
            } else {
                out.println("Room " + roomNumber + " 은 존재하지 않습니다.");
            }
        }

        private void leaveRoom() {
            int previousRoom = userList.get(User);
            removeClient(previousRoom, out);
            userList.put(User, -1); // 유저의 위치를 로비(-1)로 변경
            roomIdx = -1;
            broadcast(-1, User + " 가 방을 나갔습니다.");
        }

        private void saveChat() {
            int userLocation = userList.get(User);
            if (userLocation == -1) {
                out.println("lobby에선 해당 명령어를 사용할 수 없습니다.");
            } else {
                ChatLogManager.saveChatLog(roomIdx); // 대화 기록 저장
                out.println("Room " + roomIdx + " 의 대화 내역이 저장 되었습니다.");
            }
        }
        /**  /users - 서버에 접속한 전체 유저를 보여줍니다 */
        private void listUsers() {
            out.println("접속 중인 Users :");
            for (String user : userList.keySet()) {
                out.println(user);
            }
        }

        /**  /roomusers 유저에게 귓속말 보내기 */
        private void listRoomUsers() {
            if (roomIdx != -1) {
                out.println("Users in Room " + roomIdx + ":");
                for (Map.Entry<String, Integer> entry : userList.entrySet()) {
                    if (entry.getValue() == roomIdx) {
                        out.println(entry.getKey());
                    }
                }
            } else {
                out.println("당신은 현재 Room에 있지 않습니다.");
            }
        }

        /**  /whisper 유저에게 귓속말 보내기 */
        private void sendWhisper(String[] parts) {
            if (parts.length < 3) {
                out.println("Usage: /whisper [nickname] [message]");
                return;
            }

            String recipient = parts[1];
            String message = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));

            // 받는 사람이 존재하는지 확인하고 귓속말을 보냄
            if (userList.containsKey(recipient)) {
                int recipientRoom = userList.get(recipient);
                List<PrintWriter> recipients = chatRooms.get(recipientRoom);

                if (recipients != null) {
                    for (PrintWriter recipientWriter : recipients) {
                        recipientWriter.println(User + " whispers to " + recipient + ": " + message);
                    }
                }
            } else {
                out.println("User " + recipient + " 는 접속 중이지 않습니다.");
            }
        }
    }
}

