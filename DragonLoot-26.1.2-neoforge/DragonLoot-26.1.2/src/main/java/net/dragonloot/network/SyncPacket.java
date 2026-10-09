package net.dragonloot.network;

import net.dragonloot.DragonLootMain;
import net.dragonloot.access.DragonAnvilInterface;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncPacket(boolean isDragonAnvil) implements CustomPacketPayload {

    public static final Type<SyncPacket> TYPE = new Type<>(DragonLootMain.id("dragon_anvil_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPacket> STREAM_CODEC = StreamCodec.of(
        (buffer, packet) -> buffer.writeBoolean(packet.isDragonAnvil()),
        buffer -> new SyncPacket(buffer.readBoolean())
    );

    @Override
    public Type<SyncPacket> type() {
        return TYPE;
    }

    public static void handle(SyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) {
                return;
            }
            if (player.containerMenu instanceof AnvilMenu menu) {
                ((DragonAnvilInterface) menu).setDragonAnvil(packet.isDragonAnvil());
            }
        });
    }
}
