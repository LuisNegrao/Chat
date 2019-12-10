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
        System.out.println(this.nick + " changed nick to " + nick);
		this.nick = nick;
		if(status == "init") status = "outside";
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
        if(room != "") 
            status = "inside";
        else 
            status = "outside";
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
        System.out.println(client.getNick() + " joined " + this.name);
    }

    //DONE remove room if size == 0
    public void removeClient(Client client) {
        client.setRoom(null);
        client.setStatus("outside");
        clients.remove(client);
        if(this.clients.size() == 0) {
            Server.deleteRoom(this);
        }
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
                                    System.err.println("ERRORr closing socket" + s);
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

    static private boolean processInput( SocketChannel sc ) throws IOException{
        // Read the message to the buffer
        buffer.clear();
        sc.read( buffer );
        buffer.flip();
    
        // If no data, close the connection
        if (buffer.limit()==0) {
          return false;
        }

        boolean isnew = true;
        Client cliente = isNewClient(sc);
       
        if (cliente == null) {
            cliente = new Client(sc);
            clients.add(cliente);
            System.out.println("Novo Cliente: " + clients.size() + " Clientes");
        }

    
        // Decode and print the message to stdout
        message += decoder.decode(buffer).toString();
        if (message.contains("\n")) {
            System.out.print(cliente.getNick() +" sent: "+ message);
            // TODO precessar a menssagem
            messageProcessing(cliente, message);
            message = "";
        }
    
        return true;
    }

    private static void messageProcessing (Client client, String message) throws CharacterCodingException, IOException{

        String[] msgs = message.split("\n");

        for (String mesg : msgs) {
            
            String[] msg = mesg.split(" ");
            boolean command = (msg[0].charAt(0) == '/' && msg[0].charAt(1) != '/');
            if(command) {
                switch (msg[0]) {

                    case "/nick":
                        if (getClient(msg[1]) == null) {
                            //TODO check is name is up for grabs
                            //System.out.println(msg[1]);
                            String oldNic =  client.getNick();
                            client.setNick(msg[1]);
                            client.getSc().write(encoder.encode(CharBuffer.wrap("OK\n")));
                            //TODO broadCast method and check if nick is available
                            broadCast(client, "NEWNICK " + oldNic +" "+ client.getNick());
                        } else {
                            client.getSc().write(encoder.encode(CharBuffer.wrap("ERROR\n")));
                        }
                        break;
                    case "/join":
                            if (!client.getStatus().equals("init")) {
                                Room room = checkRooms(msg[1]);
                                if(room == null) {
                                    room = new Room(msg[1]);
                                    rooms.add(room);
                                    System.out.println("Created Room: " + room.getName());
                                }
                                if(!client.getRoom().equals("")) {
                                    //TODO broadcast msg that left room to join another
                                    checkRooms(client.getRoom()).removeClient(client);
                                    broadCast(client, "LEFT " + client.getNick());
                                    
                                }
                                room.addClients(client);
						        client.getSc().write(encoder.encode(CharBuffer.wrap("OK\n")));
                                broadCast(client, "JOINED " + client.getNick());
                            } 
                            else {
                                client.getSc().write(encoder.encode(CharBuffer.wrap("ERROR\n")));
                            }
        
                        break;
                        //TODO verify leave problem
                    case "/leave":
                        if (!client.getRoom().equals("")) {
                            Room room = checkRooms(client.getRoom());
                            if (room == null) {
                                client.getSc().write(encoder.encode(CharBuffer.wrap("ERROR\n")));
                            } else {
                                client.getSc().write(encoder.encode(CharBuffer.wrap("OK\n")));
                                broadCast(client, "LEFT " + client.getNick());
                                room.removeClient(client);
                                //TODO Broadcast leave msg
                            }
                        } else {
                            client.getSc().write(encoder.encode(CharBuffer.wrap("ERROR\n")));
                        }
                        break;
                    case "/bye":
                        Room room = checkRooms(client.getRoom());
                        if (room != null) {
                            //TODO broadcast leave msg
                            broadCast(client, "LEFT " + client.getNick());
                            room.removeClient(client);
                        }
                        client.getSc().write(encoder.encode(CharBuffer.wrap("BYE\n")));
                        clients.remove(client);
                        System.out.println("Closed connection from: " + client.getSc().socket());
                        client.getSc().close();
                        break;
                    case "/priv":
                        Client reciver = getClient(msg[1]); //TODO getClient method
                        
                        if(reciver == null) {
                            client.getSc().write(encoder.encode(CharBuffer.wrap("ERROR\n")));
                        } else {
                            client.getSc().write(encoder.encode(CharBuffer.wrap("OK\n")));
                            String privMessage = "";
                            for(int i = 2; i < msg.length-1; i++) {
                                privMessage += (msg[i] + " ");
                            }
                            privMessage += msg[msg.length - 1] +"\n";
                            
                            reciver.getSc().write(encoder.encode(CharBuffer.wrap("PRIVATE "+ client.getNick() + " " + privMessage)));
                        }
                        break;
                    default:
                        client.getSc().write(encoder.encode(CharBuffer.wrap("ERROR\n")));
                        break;
                }
            } else {
                if (client.getRoom() == "") {
                    client.getSc().write(encoder.encode(CharBuffer.wrap("ERROR\n")));
                } else {
                    //TODO verify mesg.substring 
                    broadCast(client, "MESSAGE " + client.getNick() + " " + mesg.substring(1));
                }
            }

        }

    }

    private static void broadCast(Client client, String message)  throws CharacterCodingException, IOException {

        if (client.getRoom() == "") {
            return;
        }
        Room room = checkRooms(client.getRoom());
        for (Client client2 : room.getClients()) {
            if ((message.startsWith("MESSAGE") && client2 == client) || client2 != client)  {
                client2.getSc().write(encoder.encode(CharBuffer.wrap(message+"\n")));
            }
        }


    }

    private static Room checkRooms(String name) {

        if (rooms.isEmpty()) {
            return null;
        }

        for (Room room : rooms) {
            if (room.getName().equals(name)) {
                return room;
            }
        }
        return null;

    }

    private static Client getClient(String name) {
        for (Client client : clients) {
            if(client.getNick().equals(name))
                return client;
        }
        return null;
    }

    public static void deleteRoom(Room room)  {
        rooms.remove(room);
    }
    
    private static Client isNewClient (SocketChannel sc) {

        for (Client client : clients) {
            if(client.getSc() == sc) {
                return client;
            }
        }
        return null;
    }

    // private static Client checkNick(String name) {

    //     for (Client client : clients) {
    //         if(client.getNick() == name){
    //             return client;
    //         }
    //     }
    //     return null;
    // }

}