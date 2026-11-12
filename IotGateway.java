
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.CRC32;

public class IotGateway {

    private static final ExecutorService clientPool =
            Executors.newFixedThreadPool(10);

    private static final double TEMPERATURE_THRESHOLD = 35.0;

    public static void handleClient(Socket clientSocket) {

        String threadName = Thread.currentThread().getName();

        try (Socket socket = clientSocket) {

            System.out.println("\n[" + threadName + "] IoT Device Connected!");

            BufferedReader input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            PrintWriter output = new PrintWriter(
                    socket.getOutputStream(), true
            );

            String receivedData;

            // Keep receiving sensor packets from the same device
            while ((receivedData = input.readLine()) != null) {

                System.out.println("\n[" + threadName + "] Received Packet:");
                System.out.println(receivedData);

                if (!validatePacket(receivedData)) {
                    output.println("MALFORMED|STOP");
                    continue;
                }

                try {

                    String deviceID = getString(receivedData, "device_id");
                    String timestamp = getString(receivedData, "timestamp");
                    double temperature = getNumber(receivedData, "temperature");
                    double humidity = getNumber(receivedData, "humidity");
                    String status = getString(receivedData, "status");

                    long receivedChecksum =
                            (long) getNumber(receivedData, "checksum");

                    long calculatedChecksum = calculateChecksum(
                            deviceID, timestamp, temperature, humidity, status
                    );

                    System.out.println("\n========== SENSOR DATA ==========");
                    System.out.println("Device ID   : " + deviceID);
                    System.out.println("Timestamp   : " + timestamp);
                    System.out.println("Temperature : " + temperature + " C");
                    System.out.println("Humidity    : " + humidity + "%");
                    System.out.println("Status      : " + status);
                    System.out.println("Received Checksum   : " + receivedChecksum);
                    System.out.println("Calculated Checksum : " + calculatedChecksum);

                    if (receivedChecksum != calculatedChecksum) {

                        System.out.println("Checksum Validation: FAILED");
                        System.out.println("Packet Status: INVALID");

                        output.println("INVALID_CHECKSUM|STOP");
                        continue;
                    }

                    System.out.println("Checksum Validation: PASSED");
                    System.out.println("Packet Status: VALID");

                    // Temperature threshold logic
                    String command;
                    int interval;

                    if (temperature >= TEMPERATURE_THRESHOLD) {

                        command = "WARNING";
                        interval = 1000;

                    } else {

                        command = "NORMAL";
                        interval = 5000;
                    }

                    System.out.println("----------------------------------");
                    System.out.println("Temperature Threshold: "
                            + TEMPERATURE_THRESHOLD + " C");

                    System.out.println("Gateway Command: " + command);
                    System.out.println("Telemetry Interval: "
                            + interval + " ms");

                    // Send control command back to IoT Device
                    output.println("VALID|" + command + "|" + interval);

                    System.out.println("Control command sent to device!");
                    System.out.println("==================================");

                } catch (Exception e) {

                    System.out.println("Packet Processing Error: "
                            + e.getMessage());

                    output.println("MALFORMED|STOP");
                }
            }

            System.out.println("[" + threadName + "] Device Disconnected.");

        } catch (IOException e) {

            System.out.println("[" + threadName +
                    "] Client Error: " + e.getMessage());
        }
    }

    private static String getString(String json, String key) {

        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);

        if (start == -1) {
            throw new IllegalArgumentException("Missing field: " + key);
        }

        start += pattern.length();

        int end = json.indexOf("\"", start);

        if (end == -1) {
            throw new IllegalArgumentException("Invalid field: " + key);
        }

        return json.substring(start, end);
    }

    private static double getNumber(String json, String key) {

        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);

        if (start == -1) {
            throw new IllegalArgumentException("Missing field: " + key);
        }

        start += pattern.length();

        int end = start;

        while (end < json.length() &&
                "0123456789.-".indexOf(json.charAt(end)) != -1) {
            end++;
        }

        return Double.parseDouble(json.substring(start, end));
    }

    private static long calculateChecksum(
            String deviceID,
            String timestamp,
            double temperature,
            double humidity,
            String status) {

        String checksumData = deviceID + "|" + timestamp + "|" +
                temperature + "|" + humidity + "|" + status;

        CRC32 crc = new CRC32();

        crc.update(checksumData.getBytes(
                java.nio.charset.StandardCharsets.UTF_8
        ));

        return crc.getValue();
    }

    private static boolean validatePacket(String json) {

        return json != null &&
                json.contains("\"device_id\"") &&
                json.contains("\"timestamp\"") &&
                json.contains("\"temperature\"") &&
                json.contains("\"humidity\"") &&
                json.contains("\"status\"") &&
                json.contains("\"checksum\"");
    }

    public static void main(String[] args) {

        int port = 5000;

        System.out.println("================================");
        System.out.println("   SMART IOT MONITORING SYSTEM");
        System.out.println("================================");

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server started on port: " + port);
            System.out.println("Waiting for IoT devices...");
            System.out.println("Concurrent server is ready!");
            System.out.println("Temperature Threshold: 35.0 C");

            while (true) {

                Socket clientSocket = serverSocket.accept();

                clientPool.execute(
                        () -> handleClient(clientSocket)
                );
            }

        } catch (IOException e) {

            System.out.println("Server Error: " + e.getMessage());

        } finally {

            clientPool.shutdown();
        }
    }
}
