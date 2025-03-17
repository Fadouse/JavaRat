# RemoteRat

**RemoteRat** is a Java-based remote administration tool (RAT) that allows administrators to securely monitor and control client machines. This project leverages a client-server architecture and offers features such as screen sharing, camera access, keyboard control, and more. It is designed for legitimate system administration, research, and educational purposes only.

> **Important Disclaimer**  
> This project is provided for educational and administrative use cases only. Unauthorized or malicious use is strictly prohibited. The user bears full responsibility for any legal consequences arising from misuse of this software, and the author disclaims all liability.

---

## Project Features

- **Remote Screen Sharing**  
  View and interact with the client’s screen in real-time.

- **Camera Access**  
  Stream and capture webcam feeds from the client machine.

- **Keyboard Control**  
  Send keyboard input to the client machine for remote operations.

- **Command Execution**  
  - Command Prompt interaction  
  - Shell command execution  
  - Shellcode execution

- **Notification System**  
  Send customized notifications or messages to client machines.

- **Data Retrieval**  
  - Extract browser cookies  
  - Retrieve QQ data

- **File Management**  
  Browse and manipulate files on the remote system.

- **Client Management**  
  Add or remove client connections through a centralized interface.

---

## Project Structure

```plaintext
RemoteRat/
├── Server/              # Core server application handling client connections
├── Client/              # Client-side application running on remote machines
├── libs/                # Dependency libraries (if applicable)
├── pom.xml              # Maven project configuration (Java 17)
├── LICENSE              # License information
└── README.md            # Project documentation (this file)
```

---

## How to Use

### System Requirements

- **Operating System:** Primarily Windows  
- **Java Development Kit (JDK):** Version 17 or higher  
- **Build Tool:** Maven

### Build Instructions

1. **Clone or Download the Repository**  
   Obtain the source code for both the `Server` and `Client` components.

2. **Open with Maven**  
   Navigate to the project directory where `pom.xml` is located.  
   ```bash
   cd RemoteRat
   mvn clean install
   ```
   This will download required dependencies and build both modules.

3. **Run the Server**  
   ```bash
   cd Server
   mvn exec:java
   ```
   The server will start listening for client connections on the configured port.

4. **Deploy and Run the Client**  
   - Copy the `Client` module JAR to the target machine.  
   - Execute the client JAR, ensuring it can reach the server’s IP address and port.

### Usage Overview

- **Start the Server**  
  Launch the server on the administrator’s machine. A management interface should become available for monitoring and controlling connected clients.

- **Run the Client**  
  On each target machine, run the client application. Upon successful connection, the server interface will display all active clients.

- **Perform Remote Operations**  
  From the server UI, select a connected client to:
  - Initiate remote screen sharing
  - Toggle camera access
  - Enable keyboard control
  - Execute system or shell commands
  - Send notifications
  - Browse or manage files
  - Extract browser cookies and QQ data

---

## Analysis Report

This tool is capable of potentially invasive operations (e.g., camera streaming, file access). Below are general guidelines and references for analyzing such software:

- **Security Tools & Sandbox Testing**  
  Run in a controlled environment to prevent accidental or unauthorized access.

- **Antivirus/EDR Considerations**  
  Certain antivirus or endpoint detection systems may flag or block the JARs due to remote administration functionalities.

---

## Notes

- **Educational Use Only**  
  This project is intended solely for learning and administrative oversight of systems you own or have explicit permission to manage.

- **Legal Compliance**  
  Ensure you comply with all relevant laws and regulations when deploying this software. The author disclaims responsibility for any misuse.

- **Potential False Positives**  
  Because of its nature, some security products may detect or quarantine the binaries. Operate in a secure, isolated test environment whenever possible.

---

## Contact

For any questions or discussions, please reach out to the author at: [fadouse@turings.org](mailto:fadouse@turings.org)

---

> **Disclaimer:** This project is provided only for legitimate research and system administration. The author assumes no responsibility for illegal or malicious use.

---

**Additional Note:**  
This README was adapted with a style similar to our other research-oriented security tools documentation.
