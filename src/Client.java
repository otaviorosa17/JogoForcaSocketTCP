import java.io.*;
import java.net.*;

 class Client {
    public static void main(String argv[]) throws Exception{
      String sentence;
      String rcvdString;
      Socket clientSocket = new Socket("localhost", 6789);
      while(true) {
        BufferedReader inFromUser =
        new BufferedReader(new InputStreamReader(System.in));
  
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
  
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
  
        sentence = inFromUser.readLine();
  
        outToServer.writeBytes(sentence + '\n');
        rcvdString = inFromServer.readLine();
        System.out.println(rcvdString);
      }
 }
}



