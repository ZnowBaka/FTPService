import java.io.*;
import java.net.Socket;

public class Client {
private static final int PORT = 8080;
private static final String HOST = "127.0.0.1";
private static final String CLIENTDIR = "ClientDir/";

public static void main(String[] args){
    System.out.println("Client started on port " + PORT);
    File dir = new File(CLIENTDIR);
    if(!dir.exists())
        dir.mkdir();

    try(Socket socket = new Socket(HOST, PORT)){
        InputStream rawIn = socket.getInputStream();
        OutputStream rawOut = socket.getOutputStream();

        BufferedReader clientReader = new BufferedReader(new InputStreamReader(System.in));
        BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter serverWriter = new PrintWriter(socket.getOutputStream(),true);

        System.out.println("Connected to server at " + socket.getInetAddress());

        String serverMessage = serverReader.readLine();
        System.out.println("Server" + serverMessage);
        serverMessage = serverReader.readLine();
        System.out.println("Server: " + serverMessage);

        while (true){
            System.out.println(">");
            String clientInput = clientReader.readLine();

            if (clientInput == null) break;
            clientInput = clientInput.toUpperCase();

            if ("/EXIT".equals(clientInput)) {
                serverWriter.println("/EXIT");

                serverMessage = serverReader.readLine();
                System.out.println("Server: " + serverMessage);
            }

            if ("/HELP".equals(clientInput)) {
                serverWriter.println("/HELP");

                serverMessage = serverReader.readLine();
                System.out.println("Server: " + serverMessage);
            }

            if ("/UPLOAD".equals(clientInput)) {
                serverWriter.println("/UPLOAD");
                System.out.println("Enter the file name: ");
                String downloadFileName = clientReader.readLine();
                if(downloadFileName == null) break;
                serverWriter.println(downloadFileName);
                File downloadFile = new File(CLIENTDIR + downloadFileName);
                if(!downloadFile.exists() || !downloadFile.isFile()){
                    System.out.println("File not found!");
                    break;
                }
                System.out.println("File is ready to download, Procced: [yes/no]");
                String accept = clientReader.readLine();
                if(!"yes".equalsIgnoreCase(accept)){
                    System.out.println("File not downloaded!");
                    break;
                }
                long lengthToDownload = downloadFile.length();
                serverWriter.println(lengthToDownload);
                sendFileExactly(downloadFile,rawOut,lengthToDownload);
                System.out.println(serverReader.readLine());
                System.out.println("File uploaded!");
                continue;
            }

            if ("/DOWNLOAD".equals(clientInput)) {
                serverWriter.println("/DOWNLOAD");

                // Server: Asks for file name
                serverMessage = serverReader.readLine();
                System.out.println("Server: " + serverMessage);

                // Saves file name for later use and sends it to the server.
                // This operation sends a confirmation response, which we await.
                String fileName = clientReader.readLine();
                serverWriter.println(fileName);

                // Checks response for error cases, the user does not see this
                serverMessage = serverReader.readLine();
                if (serverMessage != null && serverMessage.startsWith("ERROR")) {
                    continue;
                }

                // User is prompted to accept the download [yes/no]
                System.out.println("Server: " + serverMessage);
                String acceptPrompt = clientReader.readLine();
                serverWriter.println(acceptPrompt);

                // In case the user wrote "no", or anything else, we handle the response
                if (!"yes".equalsIgnoreCase(acceptPrompt)) {
                    String cancelled = serverReader.readLine();
                    System.out.println("Server: " + cancelled);
                    continue;
                }

                String fileLengthResponse = serverReader.readLine();

                if (fileLengthResponse == null) {
                    System.out.println("Server: unexpected header: " + fileLengthResponse);
                    continue;
                }


                long length = Long.parseLong(fileLengthResponse);

                // Server is waiting for a response before it can start the file transfer
                serverWriter.println("OK");

                // Await data from the server
                receiveFileExactly(fileName, rawIn, length);

                // To ensure that only the file data is being transferred, we have to wait for a response
                String fileTransferCompleted = serverReader.readLine();

                // Show user Server response
                System.out.println("Server: " + fileTransferCompleted);
                System.out.println("File [" + fileName + "] has been downloaded");

            }
        }
    }
    catch (Exception e){
        e.printStackTrace();
    }


}
    private static void receiveFileExactly(String fileName, InputStream rawIn, long length) throws IOException {
        File file = new File(CLIENTDIR, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            long received = 0;
            while (received < length) {
                int toRead = (int) Math.min(buffer.length, length - received);
                int read = rawIn.read(buffer, 0, toRead);
                if (read == -1) throw new EOFException("unexpected EOF while receiving file");
                fos.write(buffer, 0, read);
                received += read;
            }
            fos.flush();
        }
    }
    private static void sendFileExactly(File file, OutputStream out, long length) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            long sent = 0;
            while (sent < length) {
                int toRead = (int) Math.min(buffer.length, length - sent);
                int read = fis.read(buffer, 0, toRead);
                if (read == -1) throw new EOFException("unexpected EOF while reading server file");
                out.write(buffer, 0, read);
                sent += read;
            }
            out.flush();
        }
    }
}
