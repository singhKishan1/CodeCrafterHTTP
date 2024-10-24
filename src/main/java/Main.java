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

  public static void readPathOfHTTPRequest(BufferedReader bufferedReader, PrintWriter printWriter, Socket socket)
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
        System.out.println("Line --> " + line);
        if (line.startsWith("User-Agent")) {
          userAgent = line;
        } else if (line.startsWith("Content-Length:")) {
          contentLength = Integer.parseInt(line.split(":")[1].trim());
        } else if (line.startsWith("Accept-Encoding")) {
          acceptEncoding = line.split(":")[1].trim();
        }
      }

      // Process the request path if there's no User-Agent or it's empty
      System.out.println("Processing Path Request ==> " + requestPath);

      String response;
      if (requestPath.contains("/files/")) {
        // Handle file operations...
        // (Code for handling file writing or reading should be here)
        // Read the body of the POST request based on Content-Length
        String fileName = requestPath.split("/")[2];
        if (requestLine.contains("POST")) {

          char[] bodyContent = new char[contentLength];
          bufferedReader.read(
              bodyContent, 0,
              contentLength); // Read exactly Content-Length chars
          String fileContent = new String(bodyContent).trim();
          System.out.println("FileName ===> " + fileName);
          System.out.println("Content ===> " + fileContent);
          // Create and write to the file
          File dir = new File(directory);
          if (!dir.exists()) {
            dir.mkdirs(); // Creates the directory and all non-existent parent
                          // directories
            System.out.println("Directory created: " + dir.getAbsolutePath());
          }
          try (FileWriter writer = new FileWriter(directory + File.separator + fileName)) {
            writer.write(fileContent);
            System.out.println("File created and content written.");
          } catch (IOException e) {
            e.printStackTrace();
            printWriter.write(
                "HTTP/1.1 500 Internal Server Error\r\nContent-Length: 0\r\n\r\n");
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
      } else if (requestPath.contains("/echo/")) {
        // Extract the message after "/echo/"
        sendResponse(socket, requestPath, acceptEncoding);
      } else if (userAgent != null && !userAgent.isEmpty()) {
        // Process user-agent related response
        String message = userAgent.split(" ")[1];
        String response1 = String.format(
            "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
            message.length(), message);
        printWriter.write(response1);
        printWriter.flush();
      } else if (requestPath.length() > 1) {
        // Handle 404 Not Found for unknown paths
        response = "HTTP/1.1 404 Not Found\r\n\r\n";
        printWriter.write(response);
        printWriter.flush();
      } else {
        // Handle root path request
        System.out.println("root request -->");
        response = "HTTP/1.1 200 OK\r\n\r\n";
        printWriter.write(response);
        printWriter.flush();
      }
    }
    // Close the PrintWriter once all processing is done
    printWriter.close();
  }

  public static void sendResponse(Socket socket, String requestPath, String acceptEncoding) throws IOException {
    PrintWriter writer = new PrintWriter(socket.getOutputStream());
    String content = requestPath.split("/")[2];
    System.out.println("Send repone---> ");

    boolean isGzipAccepted = acceptEncoding != null && acceptEncoding.contains("gzip");
    if (isGzipAccepted) {
      byte[] compressedContent = compressContent(content);

      // Write the headers
      String headers = "HTTP/1.1 200 OK\r\n" +
          "Content-Encoding: gzip\r\n" +
          "Content-Type: text/plain\r\n" +
          "Content-Length: " + compressedContent.length + "\r\n\r\n";
      writer.write(headers);
      writer.flush(); // Ensure headers are sent

      // Send the compressed binary data
      socket.getOutputStream().write(compressedContent);
      socket.getOutputStream().flush();
    } else {
      // Plain text response without compression
      String response = "HTTP/1.1 200 OK\r\n" +
          "Content-Type: text/plain\r\n" +
          "Content-Length: " + content.length() + "\r\n\r\n" +
          content;
      writer.write(response);
      writer.flush();
    }

    // Close the socket after sending the response
    socket.close();
  }

  // Compress the content and return the byte array
  public static byte[] compressContent(String content) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
    gzipOutputStream.write(content.getBytes());
    gzipOutputStream.finish(); // Ensure all content is written and compressed
    gzipOutputStream.close(); // Close the GZIP stream

    // Return compressed byte array
    return byteArrayOutputStream.toByteArray();
  }

  public static void sendResponseToServer(Socket socket) throws IOException {
    PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    readPathOfHTTPRequest(bufferedReader, printWriter, socket);
  }

  public static void main(String[] args) {
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
