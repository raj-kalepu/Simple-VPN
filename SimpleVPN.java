// Simple VPN
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.util.Base64;

public class SimpleVPN {

    public static void main(String[] args) throws Exception {
        int port = 12345;

        // Start the server in a new thread
        Thread serverThread = new Thread(() -> {
            try {
                startServer(port);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.start();

        // Start the client
        startClient("localhost", port);
    }

    // Server Code
    public static void startServer(int port) throws Exception {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("VPN Server started on port " + port);

        // Generate a secret key for AES encryption
        SecretKey secretKey = KeyGenerator.getInstance("AES").generateKey();

        // Wait for a client connection
        Socket socket = serverSocket.accept();
        System.out.println("Client connected: " + socket.getInetAddress());

        // Send the secret key to the client
        ObjectOutputStream keyOut = new ObjectOutputStream(socket.getOutputStream());
        keyOut.writeObject(secretKey.getEncoded());
        keyOut.flush();

        // Setup input and output streams
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        while (true) {
            // Read encrypted data from the client
            String encryptedMessage = in.readUTF();
            System.out.println("Encrypted message received: " + encryptedMessage);

            // Decrypt the message
            String decryptedMessage = decryptMessage(encryptedMessage, secretKey);
            System.out.println("Decrypted message: " + decryptedMessage);

            // Encrypt a response
            String response = "Server response: " + decryptedMessage.toUpperCase();
            String encryptedResponse = encryptMessage(response, secretKey);

            // Send encrypted response to the client
            out.writeUTF(encryptedResponse);
            out.flush();
        }
    }

    // Client Code
    public static void startClient(String host, int port) throws Exception {
        Socket socket = new Socket(host, port);
        System.out.println("Connected to VPN Server.");

        // Receive the secret key from the server
        ObjectInputStream keyIn = new ObjectInputStream(socket.getInputStream());
        byte[] keyBytes = (byte[]) keyIn.readObject();
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

        // Setup input and output streams
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter messages to send to the server. Type 'exit' to quit.");

        while (true) {
            System.out.print("Your message: ");
            String message = consoleInput.readLine();

            if ("exit".equalsIgnoreCase(message)) {
                break;
            }

            // Encrypt the message
            String encryptedMessage = encryptMessage(message, secretKey);

            // Send the encrypted message to the server
            out.writeUTF(encryptedMessage);
            out.flush();

            // Receive encrypted response from the server
            String encryptedResponse = in.readUTF();
            System.out.println("Encrypted response from server: " + encryptedResponse);

            // Decrypt the response
            String decryptedResponse = decryptMessage(encryptedResponse, secretKey);
            System.out.println("Decrypted response: " + decryptedResponse);
        }

        socket.close();
        System.out.println("Disconnected from VPN Server.");
    }

    // AES Encryption
    public static String encryptMessage(String message, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(message.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // AES Decryption
    public static String decryptMessage(String encryptedMessage, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedMessage));
        return new String(decryptedBytes);
    }
}
