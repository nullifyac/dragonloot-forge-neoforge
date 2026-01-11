package net.dragonloot.network;

import java.util.function.Supplier;
import net.dragonloot.access.DragonAnvilInterface;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraftforge.network.NetworkEvent;

public record SyncPacket(boolean isDragonAnvil) {

    public static void encode(SyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.isDragonAnvil());
    }

    public static SyncPacket decode(FriendlyByteBuf buffer) {
        return new SyncPacket(buffer.readBoolean());
    }

    public static void handle(SyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null) {
                return;
            }
            if (minecraft.player.containerMenu instanceof AnvilMenu menu) {
                ((DragonAnvilInterface) menu).setDragonAnvil(packet.isDragonAnvil());
            }
        });
        context.setPacketHandled(true);
    }
}
