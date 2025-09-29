package org.jarvis;

import org.pcap4j.core.*;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import java.net.Inet4Address;

public class Main {

    public static void main(String[] args) {
        PcapNetworkInterface nif = null;
        try {
            // Find the first non-loopback network interface
            nif = Pcaps.findAllDevs().stream()
                    .filter(dev -> !dev.isLoopBack())
                    .findFirst()
                    .orElse(null);

        } catch (PcapNativeException e) {
            System.err.println("Error finding devices. Make sure libpcap is installed.");
            e.printStackTrace();
            return;
        }

        if (nif == null) {
            System.out.println("No suitable network interface found.");
            return;
        }

        System.out.println("Listening on: " + nif.getName() + " (" + nif.getDescription() + ")");

        // Open Pcap handle
        int snapLen = 65536;
        PcapNetworkInterface.PromiscuousMode mode = PcapNetworkInterface.PromiscuousMode.PROMISCUOUS;
        int timeout = 10000; // Increased to 10 seconds (10000 ms)
        PcapHandle handle;

        try {
            handle = nif.openLive(snapLen, mode, timeout);
        } catch (PcapNativeException e) {
            System.err.println("Error opening live handle. Are you running with sudo?");
            e.printStackTrace();
            return;
        }

        try {
            System.out.println("\nWaiting to capture a packet... (Timeout: 10 seconds)");
            Packet packet = handle.getNextPacketEx();

            if (packet == null) {
                System.out.println("No packet captured within the timeout.");
                return;
            }

            // Check if the captured packet contains an IPv4 packet
            IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);

            if (ipV4Packet != null) {
                Inet4Address srcAddr = ipV4Packet.getHeader().getSrcAddr();
                Inet4Address dstAddr = ipV4Packet.getHeader().getDstAddr();
                System.out.println("SUCCESS: Captured an IPv4 Packet!");
                System.out.println("Source IP: " + srcAddr.getHostAddress());
                System.out.println("Destination IP: " + dstAddr.getHostAddress());
            } else {
                System.out.println("A packet was captured, but it was not an IPv4 packet.");
                System.out.println(packet); // Print the packet to see what it was
            }

        } catch (Exception e) {
            System.err.println("An error occurred while capturing packets.");
            e.printStackTrace();
        } finally {
            if (handle.isOpen()) {
                handle.close();
            }
            System.out.println("Handle closed.");
        }
    }
}