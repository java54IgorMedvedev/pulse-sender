package telran.monitoring.pulse;

import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import telran.monitoring.pulse.dto.SensorData;

public class PulseSenderAppl {
    private static final int N_PACKETS = 20;
    private static final long TIMEOUT = 500;
    private static final int N_PATIENTS = 5;
    private static final int MIN_PULSE_VALUE = 50;
    private static final int MAX_PULSE_VALUE = 200;
    private static final String HOST = "localhost";
    private static final int PORT = 5000;

    private static final double JUMP_PROBABILITY = 0.15;
    private static final double JUMP_POSITIVE_PROBABILITY = 0.70;
    private static final int MIN_JUMP_PERCENT = 10;
    private static final int MAX_JUMP_PERCENT = 100;

    private static Random random = new Random();
    private static DatagramSocket socket;
    private static Map<Long, Integer> previousPulseValues = new HashMap<>();

    public static void main(String[] args) throws Exception {
        socket = new DatagramSocket();
        IntStream.rangeClosed(1, N_PACKETS)
            .forEach(PulseSenderAppl::sendPulse);
    }

    static void sendPulse(int seqNumber) {
        SensorData data = getRandomSensorData(seqNumber);
        String jsonData = data.toString();
        sendDatagramPacket(jsonData);
        try {
            Thread.sleep(TIMEOUT);
        } catch (InterruptedException e) {
        }
    }

    private static void sendDatagramPacket(String jsonData) {
        byte[] buffer = jsonData.getBytes();
        try {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                InetAddress.getByName(HOST), PORT);
            socket.send(packet);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static SensorData getRandomSensorData(int seqNumber) {
        long patientId = random.nextInt(1, N_PATIENTS + 1);
        int value = getRandomPulseValue(patientId);
        return new SensorData(seqNumber, patientId, value, System.currentTimeMillis());
    }

    private static int getRandomPulseValue(long patientId) {
        int previousValue = previousPulseValues.getOrDefault(patientId, 0);
        int newValue = previousValue == 0 ? random.nextInt(MIN_PULSE_VALUE, MAX_PULSE_VALUE + 1) 
                                          : computeNewValue(previousValue);
        previousPulseValues.put(patientId, newValue);
        return newValue;
    }

    private static int computeNewValue(int previousValue) {
        int newValue = previousValue;
        int jumpEvent = random.nextInt(100);

        if (jumpEvent < JUMP_PROBABILITY * 100) {
            int sign = random.nextInt(100) < JUMP_POSITIVE_PROBABILITY * 100 ? 1 : -1;
            int jumpPercent = random.nextInt(MIN_JUMP_PERCENT, MAX_JUMP_PERCENT + 1);
            newValue = previousValue + sign * previousValue * jumpPercent / 100;

            if (newValue > MAX_PULSE_VALUE) {
                newValue = MAX_PULSE_VALUE;
            } else if (newValue < MIN_PULSE_VALUE) {
                newValue = MIN_PULSE_VALUE;
            }
        }

        return newValue;
    }
}
