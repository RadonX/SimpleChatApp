import java.net.*;
import java.util.*;

public class ShowInterfaces
{
    public static void main(String[] args) throws Exception
    {
        // returns your local address
        System.out.println("Host addr: " + InetAddress.getLocalHost().getHostAddress());

        // Make sure the local host that you get back isn't 127.0.0.1 ~~~~~~

        Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
        for (; n.hasMoreElements();)
        {
            NetworkInterface e = n.nextElement();
            System.out.println("Interface: " + e.getName());
            Enumeration<InetAddress> a = e.getInetAddresses();
            for (; a.hasMoreElements();)
            {
                InetAddress addr = a.nextElement();
                System.out.println(" " + addr.getHostAddress());
            }
        }

    }
}