import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static final int PORT = 8080;
    private static final String SERVERDIR = "serverDir/";

    public static void main(String[] args){
        System.out.println("Server started on port " + PORT);

        try(ServerSocket serverSocket = new ServerSocket(PORT)){




            File dir = new File(SERVERDIR);
            if(!dir.exists())
                dir.mkdir();

            while(true){
                Socket socket = serverSocket.accept();
                System.out.println("Client connected at " + socket.getInetAddress());

                new ClientHandler(socket, SERVERDIR).start();
            }
        } catch (Exception e){
        }

    }


}
