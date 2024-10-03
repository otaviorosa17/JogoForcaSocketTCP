import java.io.*;
import java.net.*;
import java.util.HashMap;

class Server {
    private static HashMap<SocketAddress, String> players = new HashMap<>();
    private static int playerCount = 0;
    private static String wordToGuess = null;
    private static StringBuilder currentGuessState;
    private static int attemptsLeft = 6;
    private static boolean gameInProgress = false;

    public static void main(String argv[]) throws Exception {
        ServerSocket welcomeSocket = new ServerSocket(6789);

        System.out.println("Servidor iniciado, aguardando jogadores...");

        while (true) {
            // Aceitar uma nova conexão
            Socket connectionSocket = welcomeSocket.accept();
            SocketAddress playerAddress = connectionSocket.getRemoteSocketAddress();
            
            // Atribuir Player 1 ou Player 2
            playerCount++;
            String playerId = "Player " + playerCount;
            players.put(playerAddress, playerId);
            System.out.println(playerId + " conectado: " + playerAddress);

            // Criar uma nova thread para o cliente
            PlayerHandler handler = new PlayerHandler(connectionSocket, playerId);
            new Thread(handler).start();

            // Se mais de 2 jogadores tentarem se conectar, você pode impedir novas conexões
            if (playerCount > 2) {
                System.out.println("Apenas dois jogadores são permitidos.");
                connectionSocket.close();
            }
        }
    }

    // Classe para lidar com cada jogador em uma thread separada
    static class PlayerHandler implements Runnable {
        private Socket socket;
        private String playerId;
        private BufferedReader inFromClient;
        private DataOutputStream outToClient;

        public PlayerHandler(Socket socket, String playerId) {
            this.socket = socket;
            this.playerId = playerId;
        }

        @Override
        public void run() {
            try {
                inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outToClient = new DataOutputStream(socket.getOutputStream());

                String clientSentence;

                // Loop para receber mensagens do cliente
                while ((clientSentence = inFromClient.readLine()) != null) {
                    System.out.println(playerId + " enviou: " + clientSentence);

                    if (playerId.equals("Player 1") && clientSentence.equalsIgnoreCase("forca")) {
                        iniciarForca();
                    } else if (gameInProgress && playerId.equals("Player 2")) {
                        processarTentativa(clientSentence);
                    } else {
                        outToClient.writeBytes("Comando invalido ou jogo em progresso!\n");
                    }
                }
            } catch (IOException e) {
                System.out.println("Erro na comunicação com " + playerId);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Iniciar o jogo da forca
        private void iniciarForca() throws IOException {
            outToClient.writeBytes("Você escolheu o jogo da Forca! Digite a palavra a ser adivinhada:\n");
            wordToGuess = inFromClient.readLine().toLowerCase();
            currentGuessState = new StringBuilder("_".repeat(wordToGuess.length()));
            attemptsLeft = 6;
            gameInProgress = true;

            System.out.println("Palavra escolhida por Player 1: " + wordToGuess);
            // Notificar o Player 2 que o jogo começou
            notificarOutroJogador("O jogo da Forca começou! Tente adivinhar a palavra:\n" + currentGuessState.toString());
        }

        // Processar tentativas do jogador 2
        private void processarTentativa(String tentativa) throws IOException {
            if (tentativa.length() == 1) {
                char letra = tentativa.toLowerCase().charAt(0);
                boolean letraEncontrada = false;

                // Atualizar o estado da palavra
                for (int i = 0; i < wordToGuess.length(); i++) {
                    if (wordToGuess.charAt(i) == letra) {
                        currentGuessState.setCharAt(i, letra);
                        letraEncontrada = true;
                    }
                }

                if (!letraEncontrada) {
                    attemptsLeft--;
                    outToClient.writeBytes("Letra incorreta! Tentativas restantes: " + attemptsLeft + "\n");
                    notificarOutroJogador(playerId + " errou a letra '" + letra + "'. Tentativas restantes: " + attemptsLeft);
                } else {
                    outToClient.writeBytes("Letra correta! " + currentGuessState.toString() + "\n");
                    notificarOutroJogador(playerId + " acertou a letra '" + letra + "'. " + currentGuessState.toString());
                }

                // Verificar se o jogo terminou
                verificarFimDeJogo();
            } else {
                outToClient.writeBytes("Por favor, tente uma letra por vez.\n");
            }
        }

        // Verifica se o jogo terminou
        private void verificarFimDeJogo() throws IOException {
            if (currentGuessState.toString().equals(wordToGuess)) {
                gameInProgress = false;
                outToClient.writeBytes("Parabéns, você ganhou! A palavra era: " + wordToGuess + "\n");
                notificarOutroJogador(playerId + " ganhou o jogo! A palavra era: " + wordToGuess);
            } else if (attemptsLeft <= 0) {
                gameInProgress = false;
                outToClient.writeBytes("Você perdeu! A palavra era: " + wordToGuess + "\n");
                notificarOutroJogador("O jogo terminou! A palavra era: " + wordToGuess);
            }
        }

        // Notifica o outro jogador
        private void notificarOutroJogador(String mensagem) throws IOException {
            for (SocketAddress address : players.keySet()) {
                if (!players.get(address).equals(playerId)) {
                    Socket otherPlayerSocket = new Socket(address.toString(), 6789);
                    DataOutputStream otherOut = new DataOutputStream(otherPlayerSocket.getOutputStream());
                    otherOut.writeBytes(mensagem + "\n");
                    otherPlayerSocket.close();
                }
            }
        }
    }
}
