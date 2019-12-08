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

    DataOutputStream output;
    BufferedReader input;
    Socket socket;
    String serverMessage;
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
            chatArea.append("["+time+"] "+message+"\n");
        }

    }

    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message){
        chatArea.append(message);
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
            System.out.println("counldn t connect");
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

    // Método principal do objecto
    public void run() throws IOException{
        // PREENCHER AQUI
    }

    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException{
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }
    
}