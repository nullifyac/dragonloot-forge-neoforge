package net.dragonloot.init;

import net.dragonloot.DragonLootMain;
import net.dragonloot.network.SyncPacket;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public final class NetworkInit {

    private NetworkInit() {
    }

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(DragonLootMain.id("main"))
        .networkProtocolVersion(() -> PROTOCOL_VERSION)
        .clientAcceptedVersions(PROTOCOL_VERSION::equals)
        .serverAcceptedVersions(PROTOCOL_VERSION::equals)
        .simpleChannel();

    private static int packetId = 0;

    public static void init() {
        CHANNEL.registerMessage(packetId++, SyncPacket.class, SyncPacket::encode, SyncPacket::decode, SyncPacket::handle);
    }
}
