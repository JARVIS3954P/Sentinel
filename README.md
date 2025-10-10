# Sentinel
_A Java-based Intrusion Detection and Prevention System (IDPS)_

Sentinel is a host-based firewall application built entirely in Java. It actively monitors network traffic on a Linux host, detects packets from specified malicious sources, and dynamically creates firewall rules in the system's `iptables` to block them in real-time. It features a clean, user-friendly JavaFX interface for managing blocking rules and viewing live security events.

---

## Key Features

*   **Live Packet Monitoring:** Utilizes the **Pcap4j** library to capture raw network packets from a specified network interface in promiscuous mode.
*   **Dynamic Firewall Control:** Programmatically interacts with the Linux kernel's **`iptables`** firewall to insert and delete `DROP` rules without needing to restart the application or the system.
*   **GUI-Based Rule Management:** A simple and intuitive **JavaFX** user interface allows users to add and delete IP-based blocking rules on the fly.
*   **Persistent Rules & Logs:** All firewall rules and block events are stored in a local **SQLite** database, ensuring that your configuration is saved between sessions.
*   **Real-time Event Logging:** The UI provides a live-updating log that immediately displays when a packet from a blocked IP is detected and action is taken.
*   **Multi-threaded Architecture:** The packet sniffing service runs on a dedicated background thread, ensuring the user interface remains responsive at all times.

---

## Architecture Overview

Sentinel operates on a simple but powerful reactive architecture. The application is composed of four main components that work in concert:

1.  **UI (JavaFX):** The command center. This is the user's entry point for viewing the system's state, adding new rules, and removing old ones.
2.  **Core Engine (Pcap4j):** The detector. Running in a background thread, this service continuously listens for network packets. When a packet arrives, it inspects the source IP and checks it against the active ruleset.
3.  **Enforcer (`iptables`):** The muscle. When the Core Engine detects a packet that violates a rule, it instructs the Enforcer to execute a `sudo iptables` command via a `ProcessBuilder`, adding a `DROP` rule to the kernel's firewall.
4.  **Persistence (SQLite):** The memory. A local SQLite database stores all user-defined rules and a historical log of all block events. The Core Engine loads its ruleset from this database on startup.

```
+----------------+      (Manages)      +--------------------+
|  UI (JavaFX)   | <-----------------> |  Core Engine       |
+----------------+                     |  (Pcap4j Listener) |
      ^                                +--------------------+
      | (Updates)                            |         | (Checks Rules)
      |                                      |         V
      | (Logs Events)                        |   +-------------------+
      |                                      |   | Persistence       |
      |                                      |   | (SQLite Database) |
      |                                      |   +-------------------+
      |                                      V
      | (Issues Block/Unblock Commands)  +-------------------+
      +--------------------------------> |  Enforcer          |
                                         |  (iptables via CLI)|
                                         +-------------------+
```

---

## Technology Stack

*   **Programming Language:** Java 21
*   **Packet Capture:** Pcap4j
*   **GUI Framework:** JavaFX
*   **Database:** SQLite (with `sqlite-jdbc` driver)
*   **Build System:** Apache Maven
*   **Packaging:** `maven-shade-plugin` for creating an executable uber-JAR.

---

## Prerequisites

Before running Sentinel, you must have the following installed on your **Linux** system:

1.  **Java Development Kit (JDK) 21 or higher.**
2.  **Apache Maven.**
3.  **Native Libraries:** `libpcap` and `iptables`.

You can install the native libraries on an Arch-based distribution (like CachyOS) with:
```bash
sudo pacman -Syu libpcap iptables
```
On a Debian-based distribution (like Ubuntu), use:
```bash
sudo apt update && sudo apt install libpcap-dev iptables
```

---

## Build Instructions

1.  Clone this repository or download the source code.
2.  Navigate to the root directory of the project in your terminal.
3.  Execute the Maven package command:
    ```bash
    mvn clean package
    ```
This will compile the source code, run tests, and create a single, executable JAR file in the `target/` directory named `Sentinel-1.0-SNAPSHOT.jar`.

---

## How to Run

The application **must** be run with `sudo` because it requires root privileges for two critical operations:
1.  To open the network interface in promiscuous mode for packet capture.
2.  To execute `iptables` commands to modify the system firewall.

Execute the following command from the project's root directory:

```bash
sudo java -jar target/Sentinel-1.0-SNAPSHOT.jar
```

The graphical user interface should launch, displaying any rules currently stored in your `firewall.db`.

### How to Test the Full Workflow

1.  **Run the application** using the `sudo` command above.
2.  Click the **"Add Rule..."** button.
3.  In the dialog box, enter an IP address you want to block (e.g., `8.8.8.8`) and click **"Save"**. The new rule will appear in the table.
4.  Open a **second terminal window** and try to `ping` the blocked IP:
    ```bash
    ping 8.8.8.8
    ```
5.  **Observe the results:**
    *   The `ping` command will show **100% packet loss**.
    *   A new entry will instantly appear in the **"Live Event Logs"** in the Sentinel UI.
    *   Running `sudo iptables -L INPUT` will show a new `DROP` rule for `8.8.8.8`.
6.  In the Sentinel UI, **select the rule** you just added and click **"Delete Selected Rule"**.
7.  The rule will disappear from the table.
8.  **Try the `ping` command again.** It should now work successfully, proving the rule has been removed from the live firewall.

---

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
