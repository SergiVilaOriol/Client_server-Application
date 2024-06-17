import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class VideoFramePacket extends Thread{
    private List<byte[]> packets; // List to store fragmented packets
    private final int FPS; // Frames per second
    private final int B; // Bitrate in bits per second
    private static final  int Ld = 1448; // Data size in Bytes
    private final int numberOfPackets;
    private final int frame_number;

    boolean running = true;

    private static List<long[]> RTT_start = new ArrayList<>();
    private static List<long[]> RTT_end = new ArrayList<>();
    private static List<int[]> RTT_end_frame = new ArrayList<>();
    private int packetsSent;

    private DatagramSocket socket;
    private InetAddress clientAddress;

    public VideoFramePacket(DatagramSocket socket, int FPS, int B, InetAddress clientAddress, int frame_num) {
        this.socket = socket;
        this.packets = new ArrayList<>();
        this.FPS = FPS;
        this.B = B;
        this.clientAddress = clientAddress;
        this.numberOfPackets = (int) Math.ceil((double) (B/FPS) / (Ld*8));
        this.frame_number = frame_num;
        packetsSent = 0;
    }

    public void generatePacketData() {
        for (int j = 0; j < numberOfPackets; j++) {
            byte[] packet = new byte[Ld];
            Random random = new Random();
            for (int i = 0; i < (Ld-3); i++) {
                packet[i] = (byte) random.nextInt(256);
            }
            packet[Ld - 1] = (byte) (j & 0xFF); // packet num inside frame
            packet[Ld - 2] = (byte) ((j >> 8) & 0xFF);
            packet[Ld - 3] = (byte) (0 & 0x3F); //frame type 0 = videopacketframe - 1 = audiopacketframe
            packet[Ld - 4] = (byte) (frame_number & 0xFF); // lower byte of frame number
            packet[Ld - 5] = (byte) ((frame_number >> 8) & 0xFF); // higher byte of frame number
            
            packets.add(packet);
        }
    }
    
    public static int extractFrameNumber(DatagramPacket packet) {
        byte[] data = packet.getData();
        int length = packet.getLength();
        int higherByte = data[length - 5] & 0xFF; // higher byte
        int lowerByte = data[length - 4] & 0xFF;  // lower byte
        return (higherByte << 8) | lowerByte; // Combine the bytes to form the frame number
    }

    public List<byte[]> getPackets() {
        return packets;
    }

    public void enter_RTT_start(long[] startTime) {
        RTT_start.add(startTime);
    }

    public static List<long[]> return_RTT_start() {
        return RTT_start;
    }

    public void enter_RTT_end(long[] RTT, int[] frame) {
        RTT_end.add(RTT);
        RTT_end_frame.add(frame);
    }

    public static List<long[]> return_RTT_end() {
        return RTT_end;
    }

    public static int[] return_RTT_end_frame(){
        int[] RTT_frame = new int[RTT_end_frame.size()];
        for(int i = 0; i < RTT_end_frame.size(); i++){
            int[] frame = RTT_end_frame.get(i);
            RTT_frame[i] = frame[0];
        }
        return RTT_frame;
    }

    public static long [] return_RTT(){
        int size = RTT_end_frame.size();
        long[]RTT_result = new long[size];

        int[] frame_number_selection;
        long[] start = new long[1];
        long[] end = new long[1];

        for(int j = 0; j < size; j++){
            frame_number_selection = RTT_end_frame.get(j); //Frame al arribar
            end = RTT_end.get(j); //Temps en que el frame_number selection ha arribat
            start = RTT_start.get(frame_number_selection[0]); //Agafar el temps d'inici que pertany al frame
            RTT_result[j] = end[0] - start[0]; // Calcular RTT
            RTT_result[j] = RTT_result[j] / (1000); //Result in microseconds
        }
        return RTT_result;
    }

    public void stopThread() {
        running = false;
    }

    @Override
    public void run() {
        VideoFramePacket videoFramePacket = new VideoFramePacket(socket, FPS, B, clientAddress, frame_number);
        videoFramePacket.generatePacketData();
        
        for (byte[] packetData : videoFramePacket.getPackets()) {
            DatagramPacket sendPacket = new DatagramPacket(packetData, Ld, clientAddress, 9786); 
            long[] StartTime = new long[1];
            StartTime[0] = System.nanoTime();

            if(packetsSent == (numberOfPackets - 1)){ 
                videoFramePacket.enter_RTT_start(StartTime);
            }

            try {
                socket.send(sendPacket);
                packetsSent++;
            } catch (IOException e) {e.printStackTrace();}
        }
        if(packetsSent == (numberOfPackets - 1)){videoFramePacket.stopThread();}
    }
}