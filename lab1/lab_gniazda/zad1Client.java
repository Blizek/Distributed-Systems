import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class zad1Client {
    public static void main(String args[]) throws Exception
    {
        System.out.println("JAVA UDP CLIENT");
        DatagramSocket socket = null;
        int serverPortNumber = 9008;
        int clientPortNumber = 9009;

        try {
            socket = new DatagramSocket(clientPortNumber);
            InetAddress address = InetAddress.getByName("localhost");
            byte[] sendBuffer = "Ping".getBytes();

            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, serverPortNumber);
            socket.send(sendPacket);

            byte[] receiveBuffer = new byte[1024];
            Arrays.fill(receiveBuffer, (byte)0);
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket);
            String msg = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("received msg: " + msg);
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
