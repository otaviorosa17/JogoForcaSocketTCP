import java.io.*;
import java.net.*;

class Client {
    public static void main(String argv[]) throws Exception {
        String sentence;
        Socket clientSocket = new Socket("localhost", 6789);

        // Cria fluxos de entrada e saída
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        // Thread para ouvir o servidor continuamente
        new Thread(() -> {
            try {
                String serverResponse;
                while ((serverResponse = inFromServer.readLine()) != null) {
                    System.out.println(serverResponse);  // Exibe a mensagem do servidor imediatamente
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Loop para enviar mensagens do usuário ao servidor
        while (true) {
            sentence = inFromUser.readLine();
            outToServer.writeBytes(sentence + "\n");
            outToServer.flush();
        }
    }
}
