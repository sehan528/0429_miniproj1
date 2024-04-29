package com.example.test1;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) {
        String hostName = "localhost";
        int portNumber = 12345;

        try (Socket socket = new Socket(hostName, portNumber);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Scanner stdIn = new Scanner(System.in);

            System.out.print("Enter your nickname: ");
            String nickname = stdIn.nextLine();
            out.println(nickname);

            String serverResponse = in.readLine();
            if (serverResponse.startsWith("This nickname is already in use.")) {
                System.out.println(serverResponse);
                System.out.print("Enter a different nickname: ");
                nickname = stdIn.nextLine();
                out.println(nickname);
            }

            Thread readThread = new Thread(new ServerMessageReader(in));
            readThread.start();

            String userInput;
            while (true) {
                userInput = stdIn.nextLine();
                out.println(userInput);
                if ("/bye".equals(userInput)) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Exception caught when trying to connect to " + hostName + " on port " + portNumber);
            e.printStackTrace();
        }
    }
}