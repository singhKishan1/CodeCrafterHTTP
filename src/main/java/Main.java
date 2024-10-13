import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

  public static boolean readPathOfHTTPRequest(BufferedReader bufferedReader)
      throws IOException {
    String requestLine = bufferedReader.readLine();
    if (requestLine != null && !requestLine.isEmpty()) {
      System.out.println("RequestedLine ==> " + requestLine);

      String[] requestParts = requestLine.split(" ");
      if (!requestParts[1].equals("/")) {
        return false;
      }
    }

    return true;
  }

  public static void sendResponseToServer(Socket socket) throws IOException {
    PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

    StringBuilder msg;
    if (readPathOfHTTPRequest(bufferedReader)) {
      msg = new StringBuilder("HTTP/1.1 200 OK\r\n\r\n");
    } else {
      msg = new StringBuilder("HTTP/1.1 404 Not Found\r\n\r\n");
    }

    printWriter.write(msg.toString());
    printWriter.flush();
    printWriter.close();
  }

  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible
    // when running tests.
    System.out.println("Logs from your program will appear here!");

    // Uncomment this block to pass the first stage

    try {
      ServerSocket serverSocket = new ServerSocket(4221);

      // Since the tester restarts your program quite often, setting SO_REUSEADDR
      // ensures that we don't run into 'Address already in use' errors
      serverSocket.setReuseAddress(true);

      Socket socket = serverSocket.accept(); // Wait for connection from client.
      System.out.println("accepted new connection");
      sendResponseToServer(socket);
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}
