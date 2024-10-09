import java.io.*;
import java.net.*;
import java.util.*;

class Server {
    private static final int MAX_PLAYERS = 2; // Máximo de jogadores permitidos
    private static final int MIN_PLAYERS = 2; // Mínimo de jogadores permitidos
    private static final List<PlayerHandler> players = new ArrayList<>(); // Lista para armazenar os jogadores
    private static String wordToGuess = null; // Palavra que será adivinhada
    private static StringBuilder currentProgress; // Progresso atual da adivinhação
    private static PlayerHandler chooser = null; // Jogador que escolhe a palavra
    private static int triesLeft = 5;
    private static List<String> guessedLetters = new ArrayList<>();
    private static List<String> wrongLetters = new ArrayList<>();
    

    public static void main(String[] args) throws Exception {
        ServerSocket welcomeSocket = new ServerSocket(6789);

        System.out.println("Servidor iniciado. Aguardando jogadores...");

        while (players.size() < MAX_PLAYERS) {
            Socket connectionSocket = welcomeSocket.accept();
            if (players.size() < MAX_PLAYERS) {
                PlayerHandler player = new PlayerHandler(connectionSocket);
                new Thread(player).start();
            }
        }
    }

    // Método para verificar se um nome já está em uso
    public static boolean isNameTaken(String name) {
        for (PlayerHandler player : players) {
            if (player.playerName.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    // Enviar mensagem para todos os jogadores
    public static void broadcastMessage(String message) {
        for (PlayerHandler player : players) {
            player.sendMessage(message);
        }
    }

    // Iniciar o jogo
    public static void startGame() {
        broadcastMessage("O jogo da Forca comecou! Tente adivinhar a palavra:");
        broadcastCurrentProgress();
    }

    // Atualizar progresso atual para todos
    public static void broadcastCurrentProgress() {
        if (currentProgress != null) {
            broadcastMessage("Palavra: " + currentProgress.toString());
        }
    }

    public static boolean addGessedLetter(String letter) {
        for (String i : guessedLetters) {
            if (i.contains(letter)) {
                return false;
            }
        }
        guessedLetters.add(letter);
        return true;
    }
    public static boolean addWrongLetter(String letter) {
        for (String i : wrongLetters) {
            if (i.contains(letter)) {
                return false;
            }
        }
        wrongLetters.add(letter);
        return true;
    }
    // Classe que gerencia cada jogador
    static class PlayerHandler implements Runnable {
        private Socket socket;
        private BufferedReader inFromClient;
        private DataOutputStream outToClient;
        private String playerName;
        private String otherPlayerName;

        public PlayerHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.outToClient = new DataOutputStream(socket.getOutputStream());
            this.playerName = "";
        }

        @Override
        public void run() {
            try {
                // Solicitar nome do jogador
                sendMessage("Digite seu nome de jogador:");
                String tempName = inFromClient.readLine().trim();

                // Verificar se o nome já está em uso
                while (isNameTaken(tempName)) {
                    sendMessage("Nome ja em uso. Escolha outro nome:");
                    tempName = inFromClient.readLine().trim();
                }

                // Nome válido, agora setar o nome do jogador
                this.playerName = tempName;
                players.add(this);
                sendMessage("Bem-vindo ao jogo, " + playerName + "!");
                broadcastMessage("Jogador conectado. Lobby: " + players.size() + "/" + MAX_PLAYERS + " Aguardando para iniciar...");

                // Aguardando comando 'chooser' para definir quem sera o escolhedor
                if (chooser == null) {
                    sendMessage("Digite 'chooser' para se tornar o escolhedor da palavra.");
                }

                String clientSentence;
                while ((clientSentence = inFromClient.readLine()) != null) {
                    clientSentence = clientSentence.trim();

                    if (clientSentence.equalsIgnoreCase("chooser") && chooser == null) {
                        chooser = this;
                        sendMessage("Voce e o escolhedor da palavra! Digite a palavra secreta:");
                        sendToGuessers("Outro jogador esta escolhendo a palavra...");
                        wordToGuess = inFromClient.readLine().trim().toLowerCase();
                        while (players.size() < MIN_PLAYERS) {
                            try {
                                sendMessage("Aguardando outro jogador...");
                                Thread.sleep(2000);
                            }
                            catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        while (inFromClient.ready()) {
                            inFromClient.readLine();
                        }
                        initializeGame();
                        startGame();
                    } else if (clientSentence.length() == 1 && chooser != this && wordToGuess != null) {
                        // Verificar se o palpite está correto
                        handleGuess(clientSentence);
                    } else {
                        sendMessage("Comando invalido!");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Método para lidar com o palpite do jogador
        public void handleGuess(String guess) {
            guess.toLowerCase();
            updateProgress(guess);
            if (wordToGuess == null) {
                sendMessage("A palavra ainda nao foi escolhida pelo outro jogador, aguarde...");
                return;
            }
            if (addGessedLetter(guess)) {
                if (wordToGuess.contains(guess)) {
                    broadcastMessage("Palpite correto. Tentativas Restantes: " + String.valueOf(triesLeft));
                    if (currentProgress.toString().equalsIgnoreCase(wordToGuess)) {
                        broadcastMessage("O jogador " + playerName + " ganhou! A palavra era: " + wordToGuess);
                        resetGame();
                        return;
                    }
                } else {
                        triesLeft--;
                        broadcastMessage("Palpite incorreto. Tentativas Restantes: " + String.valueOf(triesLeft));
                        addWrongLetter(guess);
                        if (triesLeft == 0) {
                            broadcastMessage("O jogador " + chooser.playerName + " ganhou! A palavra era: " + wordToGuess);
                            resetGame();
                            return;
                        }
                    }
            } else {
                sendMessage("Esta letra ja foi palpitada");
            }
            broadcastCurrentProgress();
            String letrasPrint = String.join(", ", wrongLetters);
            broadcastMessage("Letras erradas: " + letrasPrint);
        }

        // Método para atualizar o progresso do jogo
        public void updateProgress(String guess) {
            if (wordToGuess != null) {
                for (int i = 0; i < wordToGuess.length(); i++) {
                    if (wordToGuess.charAt(i) == guess.charAt(0)) {
                        currentProgress.setCharAt(i, guess.charAt(0));
                    }
                }
            }
        }

        // Método para inicializar o progresso do jogo
        public void initializeGame() {
            currentProgress = new StringBuilder("_".repeat(wordToGuess.length()));
        }

        // Método para enviar mensagem para o jogador atual
        public void sendMessage(String message) {
            try {
                outToClient.writeBytes(message + "\n");
                outToClient.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Método que envia uma mensagem apenas para os adivinhadores
        public void sendToGuessers(String message) {
            for (PlayerHandler player : players) {
                if (player != chooser) { // Evitar enviar ao escolhedor
                    player.sendMessage(message);
                }
            }
        }

        // Resetar o jogo para uma nova rodada
        public void resetGame() {
            chooser = null;
            wordToGuess = null;
            currentProgress = null;
            triesLeft = 5;
            guessedLetters.removeAll(guessedLetters);
            wrongLetters.removeAll(wrongLetters);
            broadcastMessage("Jogo finalizado! Digite 'chooser' para uma nova rodada.");
        }
    }
}
