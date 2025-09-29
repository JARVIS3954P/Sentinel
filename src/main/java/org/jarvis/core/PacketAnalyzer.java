package org.jarvis.core;

import org.pcap4j.core.PacketListener;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;

import java.net.Inet4Address;

public class PacketAnalyzer implements PacketListener {

    @Override
    public void gotPacket(Packet packet) {
        // This method is called every time a packet is captured.

        // Check if the captured packet contains an IPv4 packet
        IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);

        if (ipV4Packet != null) {
            Inet4Address srcAddr = ipV4Packet.getHeader().getSrcAddr();
            Inet4Address dstAddr = ipV4Packet.getHeader().getDstAddr();

            // For now, we just print the details.
            // Later, this is where we will check against the rules from the database.
            System.out.println("IPv4 Packet Detected: " + srcAddr.getHostAddress() + " -> " + dstAddr.getHostAddress());
        }
        // If it's not an IPv4 packet, we simply ignore it for now.
    }
}