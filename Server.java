import java.io.*;
import java.net.*;

/**
 * Server
 */
public class Server {

    Socket socket = null;
    ServerSocket serverSocket = null;
    DataInputStream in = null;

    public Server(int port) {

        try {

            serverSocket = new ServerSocket(port);
            System.out.println("Server is Up");

            socket = serverSocket.accept();
            System.out.println("New Client Joined");

            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            String msg = "";

            while (!msg.equals("Quit")) {
                
                try {

                    msg = in.readUTF();
                    System.out.println(msg);

                } catch (Exception e) {
                    //TODO: handle exception
                    e.printStackTrace(); 
                }

            }

            socket.close();
            in.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {

        int port = Integer.parseInt(args[0]);
        Server server = new Server(port);
    }
    
}