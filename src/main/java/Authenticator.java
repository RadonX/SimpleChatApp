import java.io.*;
import java.util.HashMap;

public class Authenticator {

    HashMap<String, String> userpass;//username->password

    Authenticator(String filename){
        // if there are many users, we should use database.

        String line;
        String[] info;
        BufferedReader in;
        userpass = new HashMap<String, String>();

        // initialize userpass with filename
        try {
            in = new BufferedReader(new FileReader(new File(filename)));
            while ((line = in.readLine()) != null) {
                info = line.split(" ");
                userpass.put(info[0], info[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String getpsw(String username){
        String psw = userpass.get(username);
        System.out.println("<autheticator> " + username + " - " + psw);
        return psw;
    }

    public boolean verify(String psw, String password){
        System.out.println("<autheticator> verify: " + password);

        if (psw.equals(password)){
            return true;
        }else return false;

    }


    public void sha1(){

    }

}
