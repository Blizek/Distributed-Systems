import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class zad1Server {
    public static void main(String args[])
    {
        System.out.println("JAVA UDP SERVER");
        DatagramSocket socket = null;
        int serverPortNumber = 9008;
        int clientPortNumber = 9009;

        try{
            socket = new DatagramSocket(serverPortNumber);
            byte[] receiveBuffer = new byte[1024];

            while(true) {
                Arrays.fill(receiveBuffer, (byte)0);
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(receivePacket);
                String msg = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("received msg: " + msg);
                System.out.println("from: " + receivePacket.getAddress().getHostAddress());

                InetAddress address = InetAddress.getByName("localhost");
                byte[] sendBuffer = "Pong".getBytes();

                DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, clientPortNumber);
                socket.send(sendPacket);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
