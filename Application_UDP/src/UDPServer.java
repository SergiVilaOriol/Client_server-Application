import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

public class UDPServer {
    private static int port = 9787;
    private static int FPS = 90;
    private static int B = 10000000; // 10Mbps Bitrate in bits per seconds
    private static float time_packets = (1.0f/FPS)*1000;

    static boolean a = true;
    static int i = 0;
    private static int simulationDurationSeconds = 1; // Change this value to your desired duration
    private static long seconds_sum = (long) Math.pow(10,9) * simulationDurationSeconds;

    public static void saveMatrixToFile(long[] matrix, int[]matrix_2, double[] matrix_3, int[] matrix_4,String fileName) {
        try {
            FileWriter fileWriter = new FileWriter(fileName);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            for (int i = 0; i < matrix.length; i++) {
                printWriter.print(matrix[i] + " ");
            }
            printWriter.print("\n");

            for (int i = 0; i < matrix.length; i++) {
                printWriter.print(matrix_2[i] + " ");
            }
            printWriter.print("\n");

            for (int i = 0; i < matrix.length; i++) {
                printWriter.print(String.format("%.5f", matrix_3[i]) + " ");
            }
            printWriter.print("\n");

            printWriter.print(String.format("%d", matrix_4[0]) + " " + matrix_4[1] + " ");
            printWriter.print("\n");

            printWriter.close();
            System.out.println("Matrix values saved to: " + fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static double[] mapToNewRange(long[] originalValues, int newMax) {
        double[] newValues = new double[originalValues.length];

        for (int i = 0; i < originalValues.length; i++) {
            newValues[i] = (double) ((originalValues[i] / (double) originalValues[originalValues.length - 1]) * newMax);
        }

        return newValues;
    }

    public static double[] set_time_frames(long start_time, List<long[]> RTT_end, int simulationDurationSeconds){ //this times according to the 
        long[] RTT_time = new long[RTT_end.size()];                            //correct duration of the simulation
        for(int i = 0; i < RTT_end.size(); i++){
            long[] currentvalue = RTT_end.get(i);
            RTT_time[i] = currentvalue[0] - start_time;
        }

        double [] RTT_time_according = mapToNewRange(RTT_time, simulationDurationSeconds);
        return RTT_time_according;
    }

    public static void main(String[] args) throws IOException {
        DatagramSocket serverSocket = new DatagramSocket(port);
        InetAddress clientAddress = InetAddress.getByName("localhost");
        
        ReceiverThread receive_packets = new ReceiverThread(serverSocket, FPS, B, clientAddress);
        receive_packets.start();

        // Calculate the end time of the simulation
        long simulation_start_time = System.nanoTime();
        long endTime = simulation_start_time + (seconds_sum); //nanoseconds 1s = 10^9 nanosegons
        System.out.println(System.nanoTime());
        System.out.println(endTime);

        AudioFrame audioFrameThread = new AudioFrame(serverSocket, clientAddress);
        audioFrameThread.start();

        while(a){
            VideoFramePacket videoFrameThread = new VideoFramePacket(serverSocket, FPS, B, clientAddress, i);
            videoFrameThread.start();
            i++;

            if(System.nanoTime() > endTime){
                System.out.println(i);
                System.out.println("break");
                break;
            }
            try {Thread.sleep((long) time_packets);} catch (InterruptedException e) {e.printStackTrace();}
        }
        System.out.println("break performed");
        String breakMessage = "exit";
        byte[] finalData = breakMessage.getBytes();
        DatagramPacket finalPacket = new DatagramPacket(finalData, finalData.length, InetAddress.getLocalHost(), 9787);
        serverSocket.send(finalPacket);
        System.out.println("exit sent");
        AudioFrame.stop_audio();

        long[] RTT_result = VideoFramePacket.return_RTT();
        int[] RTT_end_frame = VideoFramePacket.return_RTT_end_frame();
        double[] RTT_time_according = set_time_frames(simulation_start_time, VideoFramePacket.return_RTT_end(), simulationDurationSeconds);
        
        int[] frames_send_receive = new int[2];
        frames_send_receive[0] = i;
        frames_send_receive[1] = ReceiverThread.frames_received();

        try { Thread.sleep(1000);} catch (InterruptedException e){e.printStackTrace();}  
        //System.out.println(Arrays.toString(RTT_result));
        System.out.println(RTT_result.length);
        saveMatrixToFile(RTT_result, RTT_end_frame, RTT_time_according, frames_send_receive, "100Mb_90FPS_ethernet");
    }
}