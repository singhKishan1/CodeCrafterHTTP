import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

  public static void readPathOfHTTPRequest(BufferedReader bufferedReader, PrintWriter printWriter)
      throws IOException {
    // Read the first line, which is the request line (e.g., "GET /path HTTP/1.1")
    String requestLine = bufferedReader.readLine();
    if (requestLine != null && !requestLine.isEmpty()) {
      System.out.println("RequestedLine ==> " + requestLine);

      // Extract the path from the request line
      String[] requestParts = requestLine.split(" ");
      String httpMethod = requestParts[0]; // e.g., "GET"
      String requestPath = requestParts[1]; // e.g., "/echo/message"

      // Read the rest of the headers
      String line;
      String userAgent = null;
      while ((line = bufferedReader.readLine()) != null && !line.isEmpty()) {
        if (line.startsWith("User-Agent")) {
          userAgent = line;
        }
      }

      // Check if User-Agent was found
      if (userAgent != null && !userAgent.isEmpty()) {
        System.out.println("User agent: ==> " + userAgent);

        // If "User-Agent" header found, extract the message from User-Agent header
        // (this part may be incorrect in your logic)
        // Assuming you're trying to split the user agent based on spaces, not sure if
        // this is correct:
        String message = userAgent.split(" ")[1]; // This might need clarification
        String response = String.format(
            "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
            message.length(),
            message);
        printWriter.write(response);
        printWriter.flush();
      } else {
        // Process the request path if there's no User-Agent or it's empty
        System.out.println("Processing Path Request ==> " + requestPath);

        String response;
        if (requestPath.contains("/echo/")) {
          // Extract the message after "/echo/"
          String message = requestPath.split("/")[2];
          System.out.println("Message from path: " + message);
          response = String.format(
              "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
              message.length(),
              message);
        } else if (requestPath.length() > 1) {
          // If path is non-root and not "/echo", return 404 Not Found
          response = "HTTP/1.1 404 Not Found\r\n\r\n";
        } else {
          // If root path ("/"), return 200 OK with no content
          response = "HTTP/1.1 200 OK\r\n\r\n";
        }

        // Send the response
        printWriter.write(response);
        printWriter.flush();
      }
    }
    // Close the PrintWriter once all processing is done
    printWriter.close();
  }

  public static void sendResponseToServer(Socket socket) throws IOException {
    PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    readPathOfHTTPRequest(bufferedReader, printWriter);

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
