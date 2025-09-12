import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler extends Thread {
    private final Socket socket;
    private final String SERVERDIR;
    private final List<String> commandList = List.of(
            "/EXIT",
            "/HELP",
            "/UPLOAD",
            "/DOWNLOAD"
    );

    public ClientHandler(Socket socket, String SERVERDIR) {
        super("Client-" + socket.getInetAddress());
        this.socket = socket;
        this.SERVERDIR = "serverDir/";
    }
    public void run(){
        try(
                InputStream rawinput = socket.getInputStream();
                OutputStream rawOutput = socket.getOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(rawinput));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(rawOutput),true)
        ){
            writer.println("Welcome to the file sharing server!");
            writer.println("list of commands: " + String.join(", ", commandList));

            String clientInput;
            while((clientInput = reader.readLine()) != null){
                switch(clientInput.toUpperCase()){
                    case ("/UPLOAD"):

                        // Saves file name for later use and sends it to the server.
                        // This operation sends a confirmation response, which we await.
                        String fileName = reader.readLine();
                        File file = new File(SERVERDIR + fileName);
                        String fileLengthResponse = reader.readLine();
                        if (fileLengthResponse == null) {
                            System.out.println("Server: unexpected header: " + fileLengthResponse);
                            continue;
                        }

                        long length = Long.parseLong(fileLengthResponse);

                        // Await data from the server
                        receiveFileExactly(file, rawinput, length);

                        // To ensure that only the file data is being transferred, we have to wait for a response
                        writer.println("File [" + fileName + "] has been downloaded");

                        break;

                    case ("/DOWNLOAD"):
                        writer.println("Enter the file name: ");
                        String downloadFileName = reader.readLine();

                        if(downloadFileName == null) break;

                        File downloadFile = new File(SERVERDIR + downloadFileName);
                        if(!downloadFile.exists() || !downloadFile.isFile()){
                            writer.println("File not found!");
                            break;
                        }
                        writer.println("File is ready to download, Procced: [yes/no]");
                        String accept = reader.readLine();
                        if(!"yes".equalsIgnoreCase(accept)){
                            writer.println("File not downloaded!");
                            break;
                        }
                        long lengthToDownload = downloadFile.length();
                        writer.println(lengthToDownload);
                        sendFileExactly(downloadFile,rawOutput,lengthToDownload);
                        writer.println("File downloaded!");
                        break;

                    case ("/HELP"):
                        writer.println("list of commands: " + String.join(", ", commandList));
                        break;

                    case ("/EXIT"):
                        try{
                            socket.close();
                        } catch (IOException e){
                        }
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    private void sendFileExactly(File file, OutputStream out, long length) throws IOException {
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
    private static void receiveFileExactly (File file, InputStream rawIn,long length) throws IOException {;
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
    }

