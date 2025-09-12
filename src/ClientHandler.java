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
        this.SERVERDIR = SERVERDIR;
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

                        break;

                    case ("/DOWNLOAD"):
                        writer.println("Enter the file name: ");
                        String fileName = reader.readLine();

                        if(fileName == null) break;

                        File file = new File(SERVERDIR + fileName);
                        if(!file.exists() || !file.isFile()){
                            writer.println("File not found!");
                            break;
                        }
                        writer.println("File is ready to download, Procced: [yes/no]");
                        String accept = reader.readLine();
                        if(!"yes".equalsIgnoreCase(accept)){
                            writer.println("File not downloaded!");
                            break;
                        }
                        long length = file.length();
                        writer.println(length);
                        System.out.println("you got here");
                        sendFileExactly(file,rawOutput,length);
                        System.out.println("You got here too");
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
}
