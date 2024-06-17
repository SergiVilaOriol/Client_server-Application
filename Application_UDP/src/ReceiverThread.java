import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ReceiverThread extends Thread {
    private DatagramSocket serverSocket;
    private int FPS;
    private int B; // 10Mbps Bitrate in bits per seconds
    private static final  int Ld = 1448 ;
    private final int numberOfPackets;
    private InetAddress clientAddress;
    private volatile boolean running = true;
    private static int count_frames;

    public ReceiverThread(DatagramSocket server_Socket, int frames_second, int bitrate, InetAddress client_Address) {
        this.serverSocket = server_Socket;
        FPS = frames_second;
        B = bitrate;
        numberOfPackets = (int) Math.ceil((double) (B/FPS) / (Ld*8));
        this.clientAddress = client_Address;

    }

    public void stopThread() {
        running = false; 
        interrupt();
    }

    public static int extractPacketNumber(DatagramPacket packet) {
        byte[] data = packet.getData();
        int length = packet.getLength();
        int higherByte = data[length - 2] & 0xFF; // higher byte
        int lowerByte = data[length - 1] & 0xFF;  // lower byte
        return (higherByte << 8) | lowerByte; // Combine the bytes to form the frame number
    }

    public static int frames_received(){
        return count_frames;
    }

    @Override
    public void run() {
        VideoFramePacket videoFrameThread = new VideoFramePacket(serverSocket, FPS, B, clientAddress, 0);
        while (running  && !Thread.currentThread().isInterrupted()) {
            try {
                byte[] receiveData = new byte[Ld];
                DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivedPacket);
                byte[] receivedData = receivedPacket.getData();
                String receivedMessage = new String(receivedData, 0, receivedPacket.getLength());
                
                if (receivedMessage.equalsIgnoreCase("exit")){
                    System.out.println(receivedMessage + " receiver"); 
                    stopThread();
                    serverSocket.close();
                    System.out.println("Server socket closed");
                    break;
                }
                int packet_num = extractPacketNumber(receivedPacket);
                int packet_type = receiveData[receivedPacket.getLength() - 3];// 0 is for video frame and 1 for audio frame
                int[] frame = new int[1];
                frame[0] = VideoFramePacket.extractFrameNumber(receivedPacket);

                if (packet_num == (numberOfPackets - 1) && packet_type == 0) {
                    count_frames++;
                    long[] endTime = new long[1];
                    endTime[0] = System.nanoTime();
                    videoFrameThread.enter_RTT_end(endTime, frame);
                }

            } catch (IOException e) {
                if (!running) { // Check if the thread was stopped intentionally
                    System.out.println("ReceiverThread stopped.");
                } else {
                    e.printStackTrace();}      
            }
        }
    }
}