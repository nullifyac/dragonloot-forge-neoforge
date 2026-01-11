package net.dragonloot.network;

import java.util.function.Supplier;
import net.dragonloot.access.DragonAnvilInterface;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.container.RepairContainer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class SyncPacket {

    private final boolean isDragonAnvil;

    public SyncPacket(boolean isDragonAnvil) {
        this.isDragonAnvil = isDragonAnvil;
    }

    public boolean isDragonAnvil() {
        return this.isDragonAnvil;
    }

    public static void encode(SyncPacket packet, PacketBuffer buffer) {
        buffer.writeBoolean(packet.isDragonAnvil);
    }

    public static SyncPacket decode(PacketBuffer buffer) {
        return new SyncPacket(buffer.readBoolean());
    }

    public static void handle(SyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null) {
                return;
            }
            if (minecraft.player.containerMenu instanceof RepairContainer) {
                ((DragonAnvilInterface) minecraft.player.containerMenu).setDragonAnvil(packet.isDragonAnvil);
            }
        });
        context.setPacketHandled(true);
    }
}
