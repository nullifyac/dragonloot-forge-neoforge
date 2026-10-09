package net.dragonloot.init;

import net.dragonloot.network.SyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkInit {

    private NetworkInit() {
    }

    private static final String PROTOCOL_VERSION = "1";

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NetworkInit::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(SyncPacket.TYPE, SyncPacket.STREAM_CODEC, SyncPacket::handle);
    }

    public static void sendDragonAnvilSync(ServerPlayer player, boolean isDragonAnvil) {
        PacketDistributor.sendToPlayer(player, new SyncPacket(isDragonAnvil));
    }
}
