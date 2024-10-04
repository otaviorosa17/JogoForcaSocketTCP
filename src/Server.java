import java.io.*;
import java.net.*;
import java.text.Normalizer;
import java.util.concurrent.ConcurrentHashMap;

class Server {
    private static ConcurrentHashMap<String, PlayerHandler> players = new ConcurrentHashMap<>();
    private static String wordToGuess = null;
    private static StringBuilder currentGuessState;
    private static int attemptsLeft = 6;
    private static boolean gameInProgress = false;
    private static String wordChooser = null;

    public static void main(String argv[]) throws Exception {
        ServerSocket welcomeSocket = new ServerSocket(6789);
        System.out.println("Servidor iniciado, aguardando jogadores...");

        while (true) {
            Socket connectionSocket = welcomeSocket.accept();

            // Configuração de Keep-Alive
            connectionSocket.setKeepAlive(true);

            PlayerHandler handler = new PlayerHandler(connectionSocket);
            new Thread(handler).start();
        }
    }

    // Classe para lidar com cada jogador
    static class PlayerHandler implements Runnable {
        private Socket socket;
        private String playerId;
        private String playerName;
        private BufferedReader inFromClient;
        private DataOutputStream outToClient;
        private boolean isConnected;

        public PlayerHandler(Socket socket) throws IOException {
          this.socket = socket;
          this.isConnected = true;
          this.inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
          this.outToClient = new DataOutputStream(socket.getOutputStream());
          
          // Envia a mensagem e força o envio com flush
          outToClient.writeBytes("Digite seu nome de jogador: ");
          outToClient.flush(); // Garante que a mensagem seja enviada imediatamente
      
          this.playerName = inFromClient.readLine(); // Agora o jogador pode digitar o nome após a mensagem
      }
      

        @Override
        public void run() {
            try {
                
                autenticarJogador(playerName);

                String clientSentence;

                while (isConnected && (clientSentence = inFromClient.readLine()) != null) {
                    clientSentence = removeAcentos(clientSentence).toLowerCase();
                    System.out.println(playerId + " enviou: " + clientSentence);

                    if (!gameInProgress && clientSentence.equals("forca")) {
                        iniciarForca();
                    } else if (gameInProgress && !playerId.equals(wordChooser)) {
                        processarTentativa(clientSentence);
                    } else {
                        outToClient.writeBytes("Comando invalido ou jogo em progresso!\n");
                    }
                }
            } catch (IOException e) {
                System.out.println(playerId + " desconectado.");
                this.isConnected = false;
            }
        }

        private void autenticarJogador(String nome) throws IOException {

          nome = removeAcentos(nome).toLowerCase(); // Remove acentos e padroniza o nome em minúsculas
      
          if (players.containsKey(nome)) {
              this.playerId = nome;
              players.get(nome).reconnect(socket); // Reestabelece a conexão
              outToClient.writeBytes("Reconexao bem-sucedida. Bem-vindo de volta, " + nome + "!\n");
              System.out.println(playerId + " reconectado.");
          } else {
              this.playerId = nome;
              players.put(nome, this); // Adiciona o novo jogador ao mapa de jogadores
              outToClient.writeBytes("Bem-vindo ao jogo, " + nome + "!\n"); // Exibe a mensagem de boas-vindas
              System.out.println(playerId + " conectado.");
          }
      }
      

        public void reconnect(Socket newSocket) throws IOException {
            this.socket = newSocket;
            this.inFromClient = new BufferedReader(new InputStreamReader(newSocket.getInputStream()));
            this.outToClient = new DataOutputStream(newSocket.getOutputStream());
            this.isConnected = true;
        }

        private void iniciarForca() throws IOException {
            if (!gameInProgress) {
                outToClient.writeBytes("Voce escolheu o jogo da Forca! Digite a palavra a ser adivinhada:\n");
                wordToGuess = removeAcentos(inFromClient.readLine().toLowerCase());

                if (wordToGuess == null || wordToGuess.isEmpty()) {
                    outToClient.writeBytes("Erro: Palavra invalida.\n");
                    return;
                }

                wordChooser = playerId;
                currentGuessState = new StringBuilder("_".repeat(wordToGuess.length()));
                attemptsLeft = 6;
                gameInProgress = true;

                System.out.println("Palavra escolhida por " + playerId + ": " + wordToGuess);
                notificarOutroJogador("O jogo da Forca comecou! Tente adivinhar a palavra:\n" + currentGuessState, obterOutroJogador());
            } else {
                outToClient.writeBytes("Jogo ja esta em progresso!\n");
            }
        }

        private void processarTentativa(String tentativa) throws IOException {
            if (tentativa.length() == 1) {
                char letra = tentativa.charAt(0);
                boolean letraEncontrada = false;

                for (int i = 0; i < wordToGuess.length(); i++) {
                    if (wordToGuess.charAt(i) == letra) {
                        currentGuessState.setCharAt(i, letra);
                        letraEncontrada = true;
                    }
                }

                if (!letraEncontrada) {
                    attemptsLeft--;
                    outToClient.writeBytes("Letra incorreta! Tentativas restantes: " + attemptsLeft + "\n");
                    notificarOutroJogador(playerId + " errou a letra '" + letra + "'. Tentativas restantes: " + attemptsLeft, wordChooser);
                } else {
                    outToClient.writeBytes("Letra correta! " + currentGuessState + "\n");
                    notificarOutroJogador(playerId + " acertou a letra '" + letra + "'. " + currentGuessState, wordChooser);
                }

                verificarFimDeJogo();
            } else {
                outToClient.writeBytes("Por favor, tente uma letra por vez.\n");
            }
        }

        private void verificarFimDeJogo() throws IOException {
            if (currentGuessState.toString().equals(wordToGuess)) {
                gameInProgress = false;
                outToClient.writeBytes("Parabens, voce ganhou! A palavra era: " + wordToGuess + "\n");
                notificarOutroJogador(playerId + " ganhou o jogo! A palavra era: " + wordToGuess, wordChooser);
            } else if (attemptsLeft <= 0) {
                gameInProgress = false;
                outToClient.writeBytes("Voce perdeu! A palavra era: " + wordToGuess + "\n");
                notificarOutroJogador("O jogo terminou! A palavra era: " + wordToGuess, wordChooser);
            }
        }

        private void notificarOutroJogador(String mensagem, String outroPlayerId) throws IOException {
            PlayerHandler outroPlayer = players.get(outroPlayerId);
            if (outroPlayer != null && outroPlayer.isConnected) {
                outroPlayer.outToClient.writeBytes(mensagem + "\n");
            }
        }

        private String obterOutroJogador() {
            for (String playerId : players.keySet()) {
                if (!playerId.equals(this.playerId)) {
                    return playerId;
                }
            }
            return null;
        }

        private String removeAcentos(String str) {
            return Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
        }
    }
}
