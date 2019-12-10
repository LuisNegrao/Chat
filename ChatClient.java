import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Client
 */
public class ChatClient {

    private static final String servAck = "OK";
    private static final String servNack = "ERROR";
    private static final String userMessage = "MESSAGE";
    private static final String privMessage = "PRIVATE";
    private static final String nickChange = "NEWNICK";
    private static final String joinRoom = "JOINED";
    private static final String leavRoom = "LEFT";
    private static final String servLeave = "BYE";

    DataOutputStream output;
    BufferedReader input;
    Socket socket;
    String serverMessage = "";
    String nickName;
    String channel;

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *    
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas coma interface gráfica

    
    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui

    public void sendMessage(String message, int isChat) {

        String time = new SimpleDateFormat("HH.mm.ss").format(new Date());

        if(isChat == 0) {
            chatArea.append(message +"\n");
        } else if(isChat == 1) {
           chatArea.append("["+time+"]:"+message+"\n");
        }

    }

    public void sendChatMessage(final String sender, final String message, String flag){
        String time = new SimpleDateFormat("HH.mm.ss").format(new Date());
        if(flag.equals("sender"))
            chatArea.append("["+time+"] <Private> to "+sender+":"+message+"\n");
        else if(flag.equals("receiver"))
            chatArea.append("["+time+"] <Private> from "+sender+":"+message+"\n");
        else if(flag.equals("normal"))
            chatArea.append("["+time+"] "+sender+":"+message+"\n");
            

    }

    public void sendMsgtoChannel(final String sender, final String message){
        String time = new SimpleDateFormat("HH.mm.ss").format(new Date());
        chatArea.append("["+time+"] [@"+channel+"] "+sender+": "+message+"\n");

    }

    //construtor
    public ChatClient(String server, int port) throws IOException{
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                try{
                    newMessage(chatBox.getText());
                } catch(IOException ex){} finally{
                    chatBox.setText("");
                }
            }
        });
        frame.addWindowListener(new WindowAdapter(){
            public void windowOpened(WindowEvent e){
                chatBox.requestFocus();
            }
        });
        
        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui
        boolean isConnected = false;
        
        try {
            
            socket = new Socket(server, port);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            output = new DataOutputStream(socket.getOutputStream());
            isConnected =  true;

        } catch (IOException e) {
            System.out.println("Connection Error");
        } finally {
            if (isConnected) {
                
                frame.setTitle("Chat at " + server + " " + port);
                sendMessage("Available Commands:", 0);
				sendMessage("/nick $nickname to set/change your nickname.", 0);
				sendMessage("/join $room to join/change your conversation room.", 0);
				sendMessage("/leave to leave your current coversation room.", 0);
				sendMessage("/priv $nickname to send a private message to $nickname.", 0);
				sendMessage("/bye to close the connection to the current server.", 0);
            }
            else sendMessage("Could not establish connection", 0);
        }
    }

    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException{
        // PREENCHER AQUI com código que envia a mensagem ao servidor
        serverMessage = message;
        message += "\n";
        byte [] msg = message.getBytes("UTF-8");
        output.write(msg);
    }

    public void servStateMessage(String servCmd){
        String clientCmd[] = serverMessage.split(" ");
        
        switch(clientCmd[0]){
            case "/join": 
                if(servCmd.equals(servAck)){
                    channel = clientCmd[1];
                    String okMsg ="Joining room @"+channel+".";
                    sendMessage(okMsg, 1);
                }
                else if(servCmd.equals(servNack)){
                    if(nickName == null){
                        String nullNick = "Please define your nickname before joining a room.";
                        sendMessage(nullNick, 1);
                    }
                    else{
                        String joinError = "Error joining room, please try again.";
                        sendMessage(joinError, 1);
                    }
                }
                break;
            case "/leave":
                if(servCmd.equals(servAck)){
                    String leavRoom = "You left the @"+channel+" room.";
                    sendMessage(leavRoom, 1);
                    channel = "";
                }
                else if(servCmd.equals(servNack)){
                    if(channel == null){
                        String noRoom = "Please join a room before leaving it";
                        sendMessage(noRoom, 1);
                    }
                    else{
                        String leavError = "Error leaving room, please try again.";
                        sendMessage(leavError, 1);
                    }
                }
                break;
            case "/nick":
                if(servCmd.equals(servAck)){
                    String nickSet = "Nickname set to '"+clientCmd[1]+"'.";
                    sendMessage(nickSet, 1);
                }
                else if(servCmd.equals(servNack)){
                    if(clientCmd[1] == null || clientCmd[1] == "" || clientCmd[1] == " "){
                        String noNick = "Please input a valid nickname.";
                        sendMessage(noNick, 1);
                    }
                    else{
                        String badNick = "Nickname already taken, please choose another.";
                        sendMessage(badNick, 1);
                    }
                }
                break;
            case "/priv":
                if(servCmd.equals(servAck)){
                    String receipt = clientCmd[1];
                    String prvtMsg="";
                    for(int i=2; i<clientCmd.length; i++)
                        prvtMsg += " "+clientCmd[i];
                    sendChatMessage(receipt, prvtMsg, "sender");
                }
                else if(servCmd.equals(servNack)){
                    String noRcvr = "Unable to send private message, please try again later.";
                    sendMessage(noRcvr, 1);
                }
                break;
            default:
                if(servCmd.equals(servNack)){
                    String failCmd = clientCmd[0];
                    if(failCmd.charAt(0) =='/' && failCmd.charAt(1) != '/'){
                        String cmdError = "Invalid or mistyped command, please try again.";
                        sendMessage(cmdError, 1);
                        break;
                    }
                    else if(nickName == null){
                        String nullNick = "A nickname must be set before having premission to do that.";
                        sendMessage(nullNick, 1);
                    }
                    else if(channel == null){
                        String nullRoom = "You need to be inside a chatroom before sending any messages";
                        sendMessage(nullRoom, 1);
                    }
                }
                break;
        }
    }
    // Método principal do objecto
    public void run() throws IOException{
        while(socket.isClosed() == false){
            String servInfo = input.readLine();


            if(servInfo.equals("") == false){
                String servMessg[] = servInfo.split(" ");
                String userMessg ="";

                switch(servMessg[0]){
                    case joinRoom:
                        String enterRoom = "User '"+servMessg[1]+"' has joined.";
                        sendMessage(enterRoom, 1);
                        break;
                    case leavRoom:
                        String lftRoom = "User '"+servMessg[1]+"' has left.";
                        sendMessage(lftRoom, 1);
                        break;
                    case privMessage:
                        String receiver = servMessg[1];
                        for(int i=2; i<servMessg.length;i++)
                            userMessg += " "+servMessg[i];
                        sendChatMessage(receiver, userMessg, "receiver");
                        break;
                    case nickChange:
                        String diffNick = "User'"+servMessg[1]+"' changed nickname to: '"+servMessg[2]+"'.";
                        sendMessage(diffNick, 1);
                        break;
                    case servAck:
                        servStateMessage(servAck);
                        break;
                    case servNack:
                        servStateMessage(servNack);
                        break;
                    case userMessage:
                        String sender = servMessg[1];
                        for(int i=2; i<servMessg.length;i++)
                            userMessg += " "+servMessg[i];
                        sendChatMessage(sender,userMessg, "normal");
                        break;
                    case servLeave:
                        String servOFF = "You have been disconnected.";
                        sendMessage(servOFF, 1);
                        socket.close();
                        frame.dispose();
                        break;
                }
            }
            System.out.println("Client received: "+servInfo);
        }
    }

    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException{
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }
    
}