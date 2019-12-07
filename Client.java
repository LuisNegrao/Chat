import java.io.*;
import java.net.*;

/**
 * Client
 */
public class Client {

    Socket socket = null;
    DataInputStream in = null;
    DataOutputStream out = null;

    public Client(String address, int port) {

        try { 

            socket = new Socket(address, port);
            in = new DataInputStream(System.in);
            out = new DataOutputStream(socket.getOutputStream());

        } catch (UnknownHostException e) {

            System.out.println("Socket Oppening Error");
            e.printStackTrace();

        } catch (IOException e) {

            System.out.println("Socket Oppening Error 2.0");
            e.printStackTrace();

        }

        String msg = "";

        while (!msg.equals("Quit")) {

            try {
                msg = in.readLine();
                System.out.println(msg);
                out.writeUTF(msg);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        try {
            socket.close();
            in.close();
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        
        String address = args[0];
        int port = Integer.parseInt(args[1]);

        Client client = new Client(address, port);

    }
    
}