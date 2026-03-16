import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class zad3Server {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(9008);
        byte[] buffer = new byte[4];
        System.out.println("JAVA UDP SERVER");

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            int receivedNumber = ByteBuffer.wrap(packet.getData())
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .getInt();

            System.out.println("Otrzymana liczba: " + receivedNumber);
            int responseNumber = receivedNumber + 1;

            byte[] responseBytes = ByteBuffer.allocate(4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(responseNumber)
                    .array();

            DatagramPacket responsePacket = new DatagramPacket(
                    responseBytes, responseBytes.length, packet.getAddress(), packet.getPort()
            );
            socket.send(responsePacket);
        }
    }
}
