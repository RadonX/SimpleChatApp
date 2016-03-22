import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientThread0 extends Thread{

    Socket socket;
    String name;
    DataInputStream in;
    DataOutputStream out;
    Authenticator authenticator;
    int maxattempt = 3;

    public ClientThread0(Socket client, Authenticator authenticator) throws IOException {
        socket = client;
        this.authenticator = authenticator;

        name = "Client<" + socket.getRemoteSocketAddress() + "> ";
        System.out.println("Just connected to "
                + socket.getRemoteSocketAddress());

        in = new DataInputStream(client.getInputStream());
        out = new DataOutputStream(client.getOutputStream());

    }

    public void run(){

        try{
            out.writeUTF("connected to server " + socket.getLocalSocketAddress() + "\n");

            /*
            String line;
            while ((line = in.readUTF()) != null) {
                System.out.println(line);
                out.writeUTF("go on: ");
                //if (outputLine.equals("Bye."))   break;
            }
            */
            if (authenticate()){

            }

            close();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    private boolean authenticate() throws IOException {
        String username, psw;

        // get the saved psw of username
        out.writeUTF("Username: ");
        while (true) {
            username = in.readUTF();
            psw = authenticator.getpsw(username);
            if (psw == null) {
                out.writeUTF("The user name does not exist.\nUsername: ");
            }else break;
        }

        // verify password
        int attemptLeft = maxattempt;
        out.writeUTF("Password: ");
        while (attemptLeft > 0){
            attemptLeft--;
            if (authenticator.verify(psw, in.readUTF()) ){
                out.writeUTF("Welcome to the simple chat server!");
                System.out.println(name + "succesfully login");
                return true;
            } else{
                if (attemptLeft == 0){
                    out.writeUTF(String.format(
                            "Wrong password. %d attempts failed. Bye bye~ ", maxattempt));
                    // please try after blocktime
                    break;
                }
                out.writeUTF("Wrong password. Please try again.\nPassword: ");
            }
        }

        return false;
    }

    private void close(){
        // try to close the connection
        try {
            if(out != null) {
                out.close();
                System.out.println("ClientThread closes");
            }
        }catch(Exception e) {}

        try{
            if(socket != null) socket.close();
        } catch (Exception e) {}
    }

}
