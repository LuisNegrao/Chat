import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;


class Client {

    private SocketChannel sc;
    private String nick;
    private String status;
    private String room;

	public Client(SocketChannel sc){
		this.sc = sc;
		this.nick="UNKNOWN";
		this.status = "init";
		this.room = "";
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
    static LinkedList<Client> clients = new LinkedList<>();
    private static LinkedList<Room> rooms = new LinkedList<>();

    public static void main(String[] args) {

        int port = Integer.parseInt(args[0]);

        try {
            
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);

            ServerSocket serverSocket = serverSocketChannel.socket();
            InetSocketAddress iSocketAddress = new InetSocketAddress(port);
            serverSocket.bind(iSocketAddress);

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
                        System.out.println("New Connection");

                        SocketChannel sc = socket.getChannel();
                        sc.configureBlocking(false);

                        sc.register(selector, SelectionKey.OP_READ);

                    } else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {

                        SocketChannel sc = null;
                        System.out.println("boas");
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

        boolean isnew = true;
        Client cliente = new Client(sc);

        for (Client client : clients) {
            if(client.getSc() == sc) {
                isnew = false;
            }
        }

        if (isnew) {
            clients.add(cliente);
            System.out.println("Novo Cliente: " + clients.size() + " Clientes");
        }

    
        // Decode and print the message to stdout
        String message2 = decoder.decode(buffer).toString();
        if (message2.contains("\n")) {

            System.out.print(cliente.getNick() +" sent: "+ message2);
            // TODO precessar a menssagem
        }
    
        return true;
    }

    private static void messageProcessing (Client client, String message) {

        String[] msgs = message.split("\n");

        for (String mesg : msg) {
            
            String[] msg = mesg.split(" ");
            boolean command = (msg[0].charAt(0) == "/" && msg[0].charAt(1) == "/");

            if(command) {
                switch (msg[0]) {

                    case "/nick":
                        if (command) {
                            if(client.getNick() == "UNKNOWN")
                            client.setNick(msg[1]);
                            client.getSc().write(encoder.encode(CharBuffer.wrap("OK\n")));
                            //TODO broadCast method and check if nick is available
                        } else {
                            client.getSc().write(encoder.encode(CharBuffer.wrap("ERROR\n")));
                        }
                        break;
                    case "/join":
                        if (command) {
                            if (!client.getStatus().equals("init")) {
                                Room room = checkRooms();
                            }
                        }
                        
                        break;
                
                    default:
                        break;
                }
            }

        }

    }
    
}