package telran.monitoring.pulse;

import java.net.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.*;

import telran.monitoring.pulse.dto.SensorData;

public class PulseSenderAppl {

    private static final int PORT = 5000;
    private static final String HOST = "127.0.0.1";
    private static final int N_PATIENTS = 1000;
    private static final int MIN_PULSE_VALUE = 40;
    private static final int MAX_PULSE_VALUE = 210;
    private static final int WARN_MAX_PULSE_VALUE = 180;
    private static final int WARN_MIN_PULSE_VALUE = 55;
    private static final int MAX_ERROR_PULSE_PROB = 5;
    private static final int MIN_ERROR_PULSE_PROB = 3;
    private static final int JUMP_PROB = 2; 
    private static final int INTERVAL_MILLIS = 1000;

    private static final Logger logger = Logger.getLogger(PulseSenderAppl.class.getName());

    private static DatagramSocket socket;
    private static InetAddress address;
    private static ThreadLocalRandom random = ThreadLocalRandom.current();
    private static Map<Long, Integer> patientIdPulseValue = new HashMap<>();

    public static void main(String[] args) throws Exception {
        configureLogging();
        socket = new DatagramSocket();
        address = InetAddress.getByName(HOST);
        
        int seqNumber = 0;  
        
        while (true) {
            long patientId = getRandomPatientId();
            int pulseValue = getRandomPulseValue(patientId);
            seqNumber++;  
            SensorData data = new SensorData(seqNumber, patientId, pulseValue, System.currentTimeMillis());
            sendSensorData(data);
            Thread.sleep(INTERVAL_MILLIS);
        }
    }


    private static void configureLogging() {
        LogManager.getLogManager().reset();
        logger.setLevel(Level.FINE);
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINE);
        logger.addHandler(consoleHandler);
        
        logger.config("Pulse Sender Application started.");
    }

    private static long getRandomPatientId() {
        return random.nextLong(1, N_PATIENTS + 1);
    }

    private static int getRandomPulseValue(long patientId) {
        int valueRes = patientIdPulseValue.computeIfAbsent(patientId,
                k -> random.nextInt(MIN_PULSE_VALUE, MAX_PULSE_VALUE + 1));

        if (chance(JUMP_PROB)) {
            valueRes = getValueWithJump(valueRes);
            patientIdPulseValue.put(patientId, valueRes);
        }
        
        if (chance(MAX_ERROR_PULSE_PROB)) {
            valueRes = MAX_PULSE_VALUE + 10;
        } else if (chance(MIN_ERROR_PULSE_PROB)) {
            valueRes = MIN_PULSE_VALUE - 10;
        }

        return valueRes;
    }

    private static boolean chance(int probability) {
        return random.nextInt(100) < probability;
    }

    private static int getValueWithJump(int value) {
        int delta = random.nextInt(5, 21); 
        return random.nextBoolean() ? value + delta : value - delta;
    }

    private static void sendSensorData(SensorData data) throws Exception {
        byte[] buffer = data.toString().getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, PORT);
        socket.send(packet);
        logger.fine("Sent SensorData: " + data);
    }
}
