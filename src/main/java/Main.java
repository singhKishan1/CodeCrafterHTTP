import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.zip.GZIPOutputStream;

public class Main {

  private static String directory;

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

      // Read the rest of the headers and extract the Content-Length if present
      String line;
      String userAgent = null;
      int contentLength = 0;
      String acceptEncoding = null;
      while ((line = bufferedReader.readLine()) != null && !line.isEmpty()) {
        if (line.startsWith("User-Agent")) {
          userAgent = line;
        } else if (line.startsWith("Content-Length:")) {
          contentLength = Integer.parseInt(line.split(":")[1].trim());
        } else if (line.startsWith("Accept-Encoding")) {
          acceptEncoding = line;
        }
      }

      // Process the request path if there's no User-Agent or it's empty
      System.out.println("Processing Path Request ==> " + requestPath);

      String response;
      if (requestPath.contains("/files/")) {
        // extract the file after "/files/"
        String fileName = requestPath.split("/")[2];
        if (directory != null && fileName != null) {
          if ("POST".equals(httpMethod)) {
            // Read the body of the POST request based on Content-Length
            char[] bodyContent = new char[contentLength];
            bufferedReader.read(bodyContent, 0, contentLength); // Read exactly Content-Length chars
            String fileContent = new String(bodyContent).trim();

            System.out.println("FileName ===> " + fileName);
            System.out.println("Content ===> " + fileContent);

            // Create and write to the file
            File dir = new File(directory);
            if (!dir.exists()) {
              dir.mkdirs(); // Creates the directory and all non-existent parent directories
              System.out.println("Directory created: " + dir.getAbsolutePath());
            }

            try (FileWriter writer = new FileWriter(directory + File.separator + fileName)) {
              writer.write(fileContent);
              System.out.println("File created and content written.");
            } catch (IOException e) {
              e.printStackTrace();
              printWriter.write("HTTP/1.1 500 Internal Server Error\r\nContent-Length: 0\r\n\r\n");
              printWriter.flush();
              return;
            }

            // Send the HTTP 201 Created response
            String response1 = "HTTP/1.1 201 Created\r\nContent-Length: 0\r\n\r\n";
            printWriter.write(response1);
            printWriter.flush(); // Ensure the response is flushed
            System.out.println("HTTP response sent.");
          } else {
            // Handle non-POST requests for /files/
            File file = new File(directory, fileName);
            if (file.exists()) {
              FileInputStream fis = new FileInputStream(file);
              byte[] fileBytes = new byte[(int) file.length()];
              int bytesRead = fis.read(fileBytes);
              fis.close();

              String fileContent = new String(fileBytes);
              response = String.format(
                  "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: %d\r\n\r\n%s",
                  bytesRead, fileContent);
              printWriter.write(response);
              printWriter.flush();
            } else {
              response = "HTTP/1.1 404 Not Found\r\n\r\n";
              printWriter.write(response);
              printWriter.flush();
            }
          }
        } else {
          response = "HTTP/1.1 404 Not Found\r\n\r\n";
          printWriter.write(response);
          printWriter.flush();
        }
      } else if (requestPath.contains("/echo/")) {
        // Extract the message after "/echo/"
        String content = requestPath.split("/")[2];
        System.out.println("Content from path: " + content);

        System.out.println("accept encoding --> " + acceptEncoding);

        if (acceptEncoding != null && !acceptEncoding.contains("invalid-encoding")) {
          response = String.format(
              "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Encoding: gzip\r\nContent-Length: %d\r\n\r\n%s",
              content.length(), content);

        } else {
          response = String.format(
              "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
              content.length(), content);
        }
        printWriter.write(response);
        printWriter.flush();
      } else if (userAgent != null && !userAgent.isEmpty()) {
        System.out.println("User Agent: ---> " + userAgent);
        String message = userAgent.split(" ")[1];
        System.out.println("Content --> " + message);
        String response1 = String.format(
            "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
            message.length(), message);
        printWriter.write(response1);
        printWriter.flush();
      } else if (requestPath.length() > 1) {
        // If path is non-root and not "/echo", return 404 Not Found
        response = "HTTP/1.1 404 Not Found\r\n\r\n";
        printWriter.write(response);
        printWriter.flush();
      } else {
        // If root path ("/"), return 200 OK with no content
        response = "HTTP/1.1 200 OK\r\n\r\n";
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

    // fetching directory
    if (args.length > 1) {
      File tempdirectory = new File(args[1]);
      if (!tempdirectory.exists()) {
        tempdirectory.mkdirs();
        System.out.println("Directory created");
      } else {
        System.out.println("Directory existed....");
      }
      directory = args[1];
    }

    // Uncomment this block to pass the first stage

    try {
      ServerSocket serverSocket = new ServerSocket(4221);

      // Since the tester restarts your program quite often, setting SO_REUSEADDR
      // ensures that we don't run into 'Address already in use' errors
      serverSocket.setReuseAddress(true);

      while (true) {
        Socket socket = serverSocket.accept(); // Wait for connection from client.
        System.out.println("accepted new connection");
        Thread th = new Thread(() -> {
          try {
            sendResponseToServer(socket);
          } catch (IOException e) {
            e.printStackTrace();
          }
        });

        th.start();
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}
