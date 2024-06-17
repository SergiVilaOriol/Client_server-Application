import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class AudioFrame extends Thread {
    private static final int NUM_BATCHES_PER_SECOND = 40;
    private static final int BITRATE = 1500000; // 1.5 Mbps
    private static final int BATCH_SIZE = BITRATE / NUM_BATCHES_PER_SECOND;
    private static boolean send_audio = true;

    private static List<byte[]> audio_batches = new ArrayList<>();
    private static int audio_packets_Sent = 0;

    private DatagramSocket socket;
    private InetAddress clientAddress;
    
    public AudioFrame(DatagramSocket socket, InetAddress address){
        this.socket = socket;
        this.clientAddress = address;
    }

    public void generateAudioBatch() {
        for (int j = 0; j < NUM_BATCHES_PER_SECOND; j++){
            byte[] audioData= new byte[BATCH_SIZE];
            for (int i = 0; i < audioData.length; i++) {
                audioData[i] = (byte) (Math.random() * 256);
            }
            audioData[BATCH_SIZE - 1] = (byte) (audio_packets_Sent & 0xfF);
            audioData[BATCH_SIZE - 2] = (byte) ((audio_packets_Sent >> 8) & 0xFF);
            audioData[BATCH_SIZE - 3] = (byte) (1 & 0x3F);
            
            audio_batches.add(audioData);
        }
    }

    public List<byte[]> getPackets() {
        return audio_batches;
    }

    public boolean sendAudio() {
        // Check if all packets for the frame have been sent
        return send_audio;
    }

    public static void stop_audio(){
        send_audio = false;
    }

    @Override
    public void run() {
        boolean a = true;
        while(a){
            AudioFrame audioframe = new AudioFrame(socket, clientAddress);
            audioframe.generateAudioBatch();
            
                for (byte[] audioData : audioframe.getPackets()) {
                    DatagramPacket sendPacket = new DatagramPacket(audioData, BATCH_SIZE, clientAddress, 9786); 
                    if(audioframe.sendAudio() == false){
                        break;
                    }
                    try {
                        socket.send(sendPacket);
                        audio_packets_Sent++;
                        
                    try {Thread.sleep(25);} catch (InterruptedException e) {e.printStackTrace();}
                } catch (IOException e) {e.printStackTrace();}
            }
        }
    }
}