import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPClient {
    static int port = 9786;
    private static int FPS = 90;
    private static int B = 10000000;
    private static final  int Ld = 1448 ;
    private static final int numberOfPackets = (int) Math.ceil((double) (B/FPS) / (Ld*8));
    private static int num_frames = 0;
    
    public static int extractPacketNumber(DatagramPacket packet) {
        byte[] data = packet.getData();
        int length = packet.getLength();
        int higherByte = data[length - 2] & 0xFF; // higher byte
        int lowerByte = data[length - 1] & 0xFF;  // lower byte
        return (higherByte << 8) | lowerByte; // Combine the bytes to form the frame number
    }

    public static void main(String[] args) throws IOException {

        DatagramSocket clientSocket = new DatagramSocket(port);

        InetAddress serverAddress = InetAddress.getByName("localhost");
        boolean a = true;
        System.out.println("Client initiated");

        while(a) {
            byte[] receiveData = new byte[Ld];
            DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivedPacket); 
            
            String receivedMessage = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
            int packet_num = extractPacketNumber(receivedPacket);
            
            if (receivedMessage.equalsIgnoreCase("exit")){ 
                String breakMessage = "exit";
                byte[] finalData = breakMessage.getBytes();
                DatagramPacket finalPacket = new DatagramPacket(finalData, finalData.length, serverAddress, 9786);
                clientSocket.send(finalPacket);
                System.out.println("Shutting down client!!");
                clientSocket.close();
                break;
            }

            if(packet_num == numberOfPackets - 1){
                clientSocket.send(receivedPacket);
            }
        }
    }
}