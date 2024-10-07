import java.io.*;
import java.net.*;

class Client {
    public static void main(String argv[]) throws Exception {
        Socket clientSocket = new Socket("localhost", 6789);

        // Thread para escutar mensagens do servidor
        Thread listenThread = new Thread(new ListenFromServer(clientSocket));
        listenThread.start();

        // Enviar dados do cliente para o servidor
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

        while (true) {
            // Enviar comando do cliente para o servidor
            String sentence = inFromUser.readLine();
            outToServer.writeBytes(sentence + '\n');
            outToServer.flush(); // Garante que os dados sejam enviados imediatamente
        }
    }
}

// Classe para escutar mensagens do servidor
class ListenFromServer implements Runnable {
    private Socket socket;

    public ListenFromServer(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String serverMessage;

            // Continuamente escutar e imprimir mensagens recebidas do servidor
            while ((serverMessage = inFromServer.readLine()) != null) {
                System.out.println(serverMessage);
            }
        } catch (IOException e) {
            System.out.println("Conexao com o servidor foi perdida.");
        }
    }
}
