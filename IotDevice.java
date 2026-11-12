
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.CRC32;

class IoTDevice {

    public static void main(String[] args) {

        String serverAddress = "localhost";
        int port = 5000;

        try (Socket socket = new Socket(serverAddress, port)) {

            System.out.println("================================");
            System.out.println("       IOT DEVICE CLIENT");
            System.out.println("================================");

            System.out.println("Connected to IoT Gateway!");

            // Sensor configuration
            String deviceID = "IOT001";

            // Change this value to 38.0 to test WARNING command
            double temperature = 38.0;

            double humidity = 65.0;
            String status = "NORMAL";

            int telemetryInterval = 5000;

            PrintWriter output = new PrintWriter(
                    socket.getOutputStream(), true
            );

            BufferedReader input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            // Send 5 sensor packets
            for (int i = 1; i <= 5; i++) {

                String timestamp = LocalDateTime.now().format(
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME
                );

                // Generate checksum
                String checksumData = deviceID + "|" + timestamp + "|" +
                        temperature + "|" + humidity + "|" + status;

                CRC32 crc = new CRC32();

                crc.update(checksumData.getBytes("UTF-8"));

                long checksum = crc.getValue();

                // Create JSON packet
                String jsonData = "{"
                        + "\"device_id\":\"" + deviceID + "\","
                        + "\"timestamp\":\"" + timestamp + "\","
                        + "\"temperature\":" + temperature + ","
                        + "\"humidity\":" + humidity + ","
                        + "\"status\":\"" + status + "\","
                        + "\"checksum\":" + checksum
                        + "}";

                // Send data to Gateway
                System.out.println("\nSending Sensor Packet " + i);

                output.println(jsonData);

                System.out.println(jsonData);

                // Receive Gateway control command
                String response = input.readLine();

                if (response == null) {
                    System.out.println("Gateway disconnected.");
                    break;
                }

                System.out.println("\nGateway Response: " + response);

                String[] commandParts = response.split("\\|");

                if (commandParts.length != 3 ||
                        !commandParts[0].equals("VALID")) {

                    System.out.println("Packet rejected or invalid command.");
                    break;
                }

                String command = commandParts[1];

                telemetryInterval = Integer.parseInt(commandParts[2]);

                System.out.println("Control Command: " + command);

                if (command.equals("WARNING")) {

                    System.out.println("ALERT! High temperature detected!");

                } else {

                    System.out.println("Temperature is NORMAL.");
                }

                System.out.println("Next transmission interval: "
                        + telemetryInterval + " ms");

                // Adjust transmission rate based on Gateway command
                if (i < 5) {
                    Thread.sleep(telemetryInterval);
                }
            }

            System.out.println("\nIoT Device Transmission Completed.");

        } catch (IOException e) {

            System.out.println("Client Error: " + e.getMessage());

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            System.out.println("Device interrupted.");
        }
    }
}
