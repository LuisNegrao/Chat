import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;
import java.util.LinkedList;

class Client {

    private SocketChannel sc;
    private String nick;
    private String status;
    private String room;

    public Client(SocketChannel sChannel) {

        this.sc = sChannel;
        this.nick = null;
        this.room = "lobby";
        this.status = "init";
    }


    public SocketChannel getSc() {
        return this.sc;
    }

    public void setSc(SocketChannel sc) {
        this.sc = sc;
    }

    public String getNick() {
        return this.nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRoom() {
        return this.room;
    }

    public void setRoom(String room) {
        this.room = room;
    }
}

class Room {

    private String name;
    private LinkedList<Client> clients;

    public Room (String name) {
        this.name = name;
        clients = new LinkedList<>();
    }

    public void addClients(Client client) {
        client.setRoom(this.name);
        clients.add(client);
    }

    public void removeClient(Client client) {
        client.setRoom(null);
        clients.remove(client);
        //TODO remove room if size == 0
    }

    public String getName () {
        return this.name;
    }

    public LinkedList<Client> getClients() {
        return this.clients;
    }

}




public class Server {

    private static ByteBuffer buffer = ByteBuffer.allocate(16384);

    private static Charset charset = Charset.forName("UTF8");
    private static CharsetDecoder decoder = charset.newDecoder();
    private static CharsetEncoder encoder = charset.newEncoder();
    private static String message = "";
    static LinkedList<Client> cli1ents = new LinkedList<>();
    private static LinkedList<Room> rooms = new LinkedList<>();

    public static void main(String[] args) {

        int port = args[0];

        try {
            
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);

            ServerSocket serverSocket = serverSocketChannel.socket();
            InetSocketAddress iSocketAddress = new InetSocketAddress(port);
            socket.bind(iSocketAdress);

            Selector selector = Selector.open();

            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println( "Listening on port "+port );
            
            while (true) {

                int num = selector.select();

                if (num == 0) 
                    continue;
                
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {

                    SelectionKey key = iterator.next();

                    if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                        
                        Socket socket = serverSocket.accept();

                        SocketChannel sc = socket.getChannel();
                        sc.configureBlocking(false);

                        sc.register(selector, SelectionKey.OP_READ);

                    } else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {

                        SocketChannel sc = null;

                        try {
                            
                            sc = (SocketChannel)key.channel();
                            boolean ok = processInput( sc );

                            if(!ok) {
                                key.cancel();

                                Socket s = null;
                                try {
                                    s = sc.socket();
                                    s.close();
                                } catch (Exception e) {
                                    System.err.println("Error closing socket" + s);
                                }
                            }

                        } catch (IOException e) {

                            key.cancel();

                            try {
                                sc.close();
                            } catch (IOException e2) {
                                System.out.println(e2);
                            }
                            System.out.println( "Closed "+sc );
                        }

                    }

                }

                keys.clear();

            }

        } catch (Exception e) {
            System.err.println(e);
        }

    }

    static private boolean processInput( SocketChannel sc ) throws IOException {
        // Read the message to the buffer
        buffer.clear();
        sc.read( buffer );
        buffer.flip();
    
        // If no data, close the connection
        if (buffer.limit()==0) {
          return false;
        }
    
        // Decode and print the message to stdout
        String message2 = decoder.decode(buffer).toString();
        System.out.print( message2 );
    
        return true;
    }
    
}