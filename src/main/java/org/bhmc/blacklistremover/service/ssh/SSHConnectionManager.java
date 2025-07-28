package org.bhmc.blacklistremover.service.ssh;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Shell;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.bhmc.blacklistremover.config.WLCConfig;
import org.bhmc.blacklistremover.config.WLCConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
@Slf4j
public class SSHConnectionManager {

    private final Map<Integer, SSHClient> connections = new ConcurrentHashMap<>();
    private final Map<Integer, WLCConfig> configs = new ConcurrentHashMap<>();
    private final Map<Integer, Shell> enabledShells = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Autowired
    private WLCConfiguration wlcConfiguration;

    @PostConstruct
    public void init() {
        log.info("Initializing SSH Connection Manager...");

        // Initialize WLC1 configuration
        WLCConfiguration.WLCConnection wlc1Config = wlcConfiguration.getSsh().getWlc1();
        if (wlc1Config.getHost() != null && !wlc1Config.getHost().isEmpty()) {
            configs.put(1, new WLCConfig(1, wlc1Config.getHost(), wlc1Config.getPort(),
                    wlc1Config.getUsername(), wlc1Config.getPassword(), wlc1Config.getEnapassword()));
            log.debug("Initialized WLC1 config: {}:{}", wlc1Config.getHost(), wlc1Config.getPort());
        } else {
            log.warn("WLC1 configuration is missing or incomplete");
        }

        // Initialize WLC2 configuration
        WLCConfiguration.WLCConnection wlc2Config = wlcConfiguration.getSsh().getWlc2();
        if (wlc2Config.getHost() != null && !wlc2Config.getHost().isEmpty()) {
            configs.put(2, new WLCConfig(2, wlc2Config.getHost(), wlc2Config.getPort(),
                    wlc2Config.getUsername(), wlc2Config.getPassword(), wlc2Config.getEnapassword()));
            log.debug("Initialized WLC2 config: {}:{}", wlc2Config.getHost(), wlc2Config.getPort());
        } else {
            log.warn("WLC2 configuration is missing or incomplete");
        }

        log.info("SSH Connection Manager initialized with {} WLC configurations", configs.size());
    }

    // Helper method to get WLC config for a given SSH client
    private WLCConfig getWLCConfigForSSH(SSHClient ssh) {
        for (Map.Entry<Integer, SSHClient> entry : connections.entrySet()) {
            if (entry.getValue() == ssh) {
                return configs.get(entry.getKey());
            }
        }
        return null;
    }

    private void createConnection(int wlcId, WLCConfig config) throws IOException {
        log.debug("Creating connection to WLC {} at {}:{}", wlcId, config.getHost(), config.getPort());
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier()); // Configure properly for production

        try {
            ssh.connect(config.getHost(), config.getPort());
            log.debug("Connected to WLC {}", wlcId);
            ssh.authPassword(config.getUsername(), config.getPassword());
            log.debug("Authenticated to WLC {}", wlcId);

            connections.put(wlcId, ssh);

            // Create persistent enabled shell session
            Shell enabledShell = createEnabledShell(ssh, config);
            enabledShells.put(wlcId, enabledShell);

            // Test the enabled shell with a simple command
            String initialTest = executeCommandOnShell(enabledShell, "show version", 2);
            log.debug("Connection test successful, output length: {}", initialTest.length());

            log.info("Successfully connected to WLC {} at {}:{}", wlcId, config.getHost(), config.getPort());
        } catch (IOException e) {
            connections.remove(wlcId);
            enabledShells.remove(wlcId);
            ssh.close();
            throw e;
        }
    }

    private Shell createEnabledShell(SSHClient ssh, WLCConfig config) throws IOException {
        log.debug("Creating persistent enabled shell session");

        Session session = ssh.startSession();
        session.allocateDefaultPTY();
        Shell shell = session.startShell();

        OutputStream outputStream = shell.getOutputStream();
        InputStream inputStream = shell.getInputStream();

        try {
            // Wait for initial prompt
            Thread.sleep(1000);

            // Clear any initial output
            byte[] clearBuffer = new byte[4096];
            while (inputStream.available() > 0) {
                inputStream.read(clearBuffer);
            }

            // Enter enable mode
            log.debug("Entering enable mode for persistent shell");
            outputStream.write("enable\n".getBytes());
            outputStream.flush();

            // Wait for password prompt
            Thread.sleep(1000);

            // Read and check for password prompt
            StringBuilder enableResponse = new StringBuilder();
            byte[] buffer = new byte[1024];
            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < 5000) { // 5 second timeout
                if (inputStream.available() > 0) {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead > 0) {
                        String chunk = new String(buffer, 0, bytesRead);
                        enableResponse.append(chunk);

                        if (chunk.toLowerCase().contains("password")) {
                            // Send enable password
                            outputStream.write((config.getEnapassword() + "\n").getBytes());
                            outputStream.flush();
                            log.debug("Sent enable password for persistent shell");
                            break;
                        }
                    }
                } else {
                    Thread.sleep(50);
                }
            }

            // Wait for enable mode to be established and check for # prompt
            Thread.sleep(1000);
            enableResponse.setLength(0);
            startTime = System.currentTimeMillis();
            boolean enableModeEstablished = false;

            while (System.currentTimeMillis() - startTime < 5000) {
                if (inputStream.available() > 0) {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead > 0) {
                        String chunk = new String(buffer, 0, bytesRead);
                        enableResponse.append(chunk);

                        if (chunk.contains("#") || enableResponse.toString().contains("#")) {
                            log.debug("Successfully entered enable mode - found # prompt");
                            enableModeEstablished = true;
                            break;
                        }
                    }
                } else {
                    Thread.sleep(50);
                }
            }

            if (!enableModeEstablished) {
                log.warn("Enable mode setup completed but # prompt not clearly detected. Response: {}",
                        enableResponse.toString().trim());
            }

            // **DISABLE PAGING TO PREVENT "Press any key to continue" PROMPTS**
            log.debug("Disabling paging for persistent shell");

            // Clear any remaining output before sending no paging command
            while (inputStream.available() > 0) {
                inputStream.read(clearBuffer);
            }

            // Send no paging command (works for most Aruba/HP devices)
            outputStream.write("no paging\n".getBytes());
            outputStream.flush();

            // Wait for command to complete
            Thread.sleep(1000);

            // Read and clear the no paging command output
            StringBuilder noPagingResponse = new StringBuilder();
            startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < 3000) { // 3 second timeout
                if (inputStream.available() > 0) {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead > 0) {
                        String chunk = new String(buffer, 0, bytesRead);
                        noPagingResponse.append(chunk);

                        // Check if command completed (back to prompt)
                        if (chunk.contains("#")) {
                            break;
                        }
                    }
                } else {
                    Thread.sleep(50);
                }
            }

            log.debug("No paging command completed. Response: {}", noPagingResponse.toString().trim());

            // Final clear of any remaining output
            while (inputStream.available() > 0) {
                inputStream.read(clearBuffer);
            }

            log.debug("Successfully created persistent enabled shell with paging disabled");
            return shell;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            shell.close();
            throw new IOException("Interrupted while setting up enabled shell", e);
        } catch (Exception e) {
            shell.close();
            throw new IOException("Failed to create enabled shell", e);
        }
    }

    public SSHClient getConnection(int wlcId) {
        return connections.get(wlcId);
    }

    public String executeCommand(int wlcId, String command, int timeoutSeconds) throws IOException {
        SSHClient ssh = getConnection(wlcId);
        if (ssh == null || !ssh.isConnected()) {
            log.warn("Connection to WLC {} is not available, attempting to reconnect", wlcId);
            reconnect(wlcId);
            ssh = getConnection(wlcId);
        }

        if (ssh == null) {
            throw new IOException("Unable to establish connection to WLC " + wlcId);
        }

        return executeCommand(ssh, command, timeoutSeconds);
    }

    private String executeCommand(SSHClient ssh, String command, int timeoutSeconds) throws IOException {
        // Find the WLC ID for this SSH client
        int wlcId = -1;
        for (Map.Entry<Integer, SSHClient> entry : connections.entrySet()) {
            if (entry.getValue() == ssh) {
                wlcId = entry.getKey();
                break;
            }
        }

        if (wlcId == -1) {
            throw new IOException("SSH client not found in connections");
        }

        Shell enabledShell = enabledShells.get(wlcId);
        if (enabledShell == null) {
            throw new IOException("Enabled shell not found for WLC " + wlcId);
        }

        return executeCommandOnShell(enabledShell, command, timeoutSeconds);
    }

    private String executeCommandOnShell(Shell shell, String command, int timeoutSeconds) throws IOException {
        log.debug("Executing command on enabled shell: {}", command);

        OutputStream outputStream = shell.getOutputStream();
        InputStream inputStream = shell.getInputStream();

        try {
            // Clear any previous output
            byte[] clearBuffer = new byte[4096];
            while (inputStream.available() > 0) {
                inputStream.read(clearBuffer);
            }

            // Send the command
            log.debug("Sending command: {}", command);
            outputStream.write((command + "\n").getBytes());
            outputStream.flush();

            // Wait for command to execute
            Thread.sleep(1000);

            // Read the output
            StringBuilder output = new StringBuilder();
            long startTime = System.currentTimeMillis();
            long timeoutMillis = timeoutSeconds * 1000L;
            byte[] buffer = new byte[16384];
            boolean foundPrompt = false;

            while (System.currentTimeMillis() - startTime < timeoutMillis && !foundPrompt) {
                if (inputStream.available() > 0) {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead > 0) {
                        String chunk = new String(buffer, 0, bytesRead);
                        output.append(chunk);

                        // Check for privileged prompt (we should always be in enabled mode)
                        if (chunk.contains("# ") || chunk.contains(")#")) {
                            foundPrompt = true;
                        }
                    }
                } else {
                    Thread.sleep(50);
                }
            }

            String result = output.toString();
            log.debug("Command '{}' raw output length: {}", command, result.length());
            log.debug("Command raw output: [{}]", result);

            // Clean up the output
            result = cleanCommandOutput(result, command);

            log.debug("Command '{}' cleaned output length: {}", command, result.length());
            log.debug("Command cleaned output: [{}]", result);

            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while executing command", e);
        }
    }

    private String cleanCommandOutput(String rawOutput, String command) {
        if (rawOutput == null || rawOutput.trim().isEmpty()) {
            return "";
        }

        String[] lines = rawOutput.split("\n");
        StringBuilder cleanOutput = new StringBuilder();
        boolean commandFound = false;

        for (String line : lines) {
            String trimmedLine = line.trim();

            // Skip the command echo line
            if (!commandFound && trimmedLine.equals(command)) {
                commandFound = true;
                continue;
            }

            // Skip empty lines at the beginning
            if (!commandFound && trimmedLine.isEmpty()) {
                continue;
            }

            // Stop at prompt
            if (trimmedLine.matches(".*\\) >.*") || trimmedLine.matches(".*# .*")) {
                break;
            }

            // Add the line to output
            if (commandFound || !trimmedLine.isEmpty()) {
                cleanOutput.append(line).append("\n");
                commandFound = true;
            }
        }

        return cleanOutput.toString().trim();
    }

    private void reconnect(int wlcId) {
        WLCConfig config = configs.get(wlcId);
        if (config == null) return;

        // Close existing connections
        Shell oldShell = enabledShells.remove(wlcId);
        if (oldShell != null) {
            try {
                oldShell.close();
            } catch (IOException e) {
                log.warn("Error closing old enabled shell for WLC {}: {}", wlcId, e.getMessage());
            }
        }

        SSHClient oldConnection = connections.remove(wlcId);
        if (oldConnection != null) {
            try {
                oldConnection.close();
            } catch (IOException e) {
                log.warn("Error closing old connection for WLC {}: {}", wlcId, e.getMessage());
            }
        }

        try {
            createConnection(wlcId, config);
            log.info("Successfully reconnected to WLC {} at {}:{}", wlcId, config.getHost(), config.getPort());
        } catch (IOException e) {
            log.error("Failed to reconnect to WLC {}: {}", wlcId, e.getMessage());
        }
    }

    private void healthCheck() {
        connections.forEach((wlcId, ssh) -> {
            try {
                if (!ssh.isConnected()) {
                    log.debug("WLC {} connection is down, attempting reconnect", wlcId);
                    reconnect(wlcId);
                }
            } catch (Exception e) {
                log.error("Health check failed for WLC {}: {}", wlcId, e.getMessage());
            }
        });
    }

    @PreDestroy
    public void cleanup() {
        scheduler.shutdown();

        // Close enabled shells first
        enabledShells.values().forEach(shell -> {
            try {
                shell.close();
            } catch (IOException e) {
                log.warn("Error closing enabled shell: {}", e.getMessage());
            }
        });
        enabledShells.clear();

        // Then close SSH connections
        connections.values().forEach(ssh -> {
            try {
                ssh.close();
            } catch (IOException e) {
                log.warn("Error closing SSH connection: {}", e.getMessage());
            }
        });
        connections.clear();
    }
}