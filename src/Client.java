import java.io.*;
import java.net.*;

class Client {
    public static void main(String argv[]) throws Exception {
        Socket clientSocket = new Socket("localhost", 6789);

        // Thread para escutar mensagens do servidor
        Thread listenThread = new Thread(new ListenFromServer(clientSocket));
        listenThread.start();

        // Criar e iniciar a thread para enviar dados do cliente para o servidor
        Thread sendThread = new Thread(new SendToServer(clientSocket));
        sendThread.start();
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

// Classe para enviar mensagens do cliente para o servidor
class SendToServer implements Runnable {
    private Socket socket;
    
    public SendToServer(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());

            while (true) {
                // Enviar comando do cliente para o servidor
                String sentence = inFromUser.readLine();
                outToServer.writeBytes(sentence + '\n');
            }
        } catch (IOException e) {
            System.out.println("Erro ao enviar mensagem para o servidor.");
        }
    }
}
