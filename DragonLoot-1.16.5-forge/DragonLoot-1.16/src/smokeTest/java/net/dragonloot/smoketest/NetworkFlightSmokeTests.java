package net.dragonloot.smoketest;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.util.UUID;
import net.dragonloot.init.ItemInit;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDirection;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameType;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayer;

/** Real movement acceptance and floating ticks; no predicate or counter is reimplemented. */
final class NetworkFlightSmokeTests {
    private NetworkFlightSmokeTests() {}

    static void run(ServerWorld world) {
        longDescent(world);
        illegalHover(world);
        verticalThreshold(world);
        rejectedAndPendingTeleport(world);
        nativeGlideExemption(world, new ItemStack(Items.ELYTRA));
        nativeGlideExemption(world, new ItemStack(ItemInit.UPGRADED_DRAGON_CHESTPLATE.get()));
    }

    private static void longDescent(ServerWorld world) {
        try (Fixture fixture = fixture(world)) {
            double startY = fixture.player.getY();
            for (int tick = 0; tick < 121; tick++) {
                double destinationY = fixture.player.getY() - 1.0D;
                fixture.move(destinationY);
                check(fixture.player.getY() == destinationY, "Descending packet must actually be accepted");
                check(!fixture.floating(), "Accepted descent must clear floating");
                fixture.listener.tick();
                check(fixture.floatingTicks() == 0, "Native floating counter must remain zero throughout accepted descent");
                check(fixture.listener.disconnectReason == null, "Accepted descent must remain connected beyond 120 listener ticks");
            }
            check(fixture.player.getY() == startY - 121.0D, "Descent control must include 121 actual accepted moves");
        }
    }

    private static void illegalHover(ServerWorld world) {
        try (Fixture fixture = fixture(world)) {
            for (int tick = 1; tick <= 81; tick++) {
                fixture.move(fixture.player.getY());
                check(fixture.floating(), "Unsupported zero-Y packets must still set floating");
                fixture.listener.tick();
                check(fixture.floatingTicks() == tick, "Native hover counter must advance exactly once per listener tick");
                if (tick <= 80) {
                    check(fixture.listener.disconnectReason == null, "Vanilla permits exactly 80 floating ticks");
                }
            }
            check(fixture.listener.disconnectReason != null, "The native listener must reject hover at tick 81");
            check(fixture.listener.disconnectReason.getString().equals(
                new TranslationTextComponent("multiplayer.disconnect.flying").getString()),
                "Hover must trigger the flying disconnect, not a transport error");
        }
    }

    private static void verticalThreshold(ServerWorld world) {
        try (Fixture fixture = fixture(world)) {
            double startY = fixture.player.getY();
            double destinationY = startY - 0.03125D;
            check(destinationY - startY == -0.03125D, "Boundary fixture must represent the exact accepted delta");
            fixture.move(destinationY);
            check(fixture.player.getY() == destinationY, "Boundary packet must be accepted");
            check(fixture.floating(), "Exactly -0.03125 must remain within the floating threshold");
            fixture.listener.tick();
            check(fixture.floatingTicks() == 1, "Native boundary hover counter must become one");
        }
        try (Fixture fixture = fixture(world)) {
            double startY = fixture.player.getY();
            // Apply nextDown to the packet coordinate so adding a tiny delta to
            // a large Y coordinate cannot round back onto the exact boundary.
            double destinationY = Math.nextDown(startY - 0.03125D);
            check(destinationY - startY < -0.03125D, "Below-boundary fixture must survive coordinate rounding");
            fixture.move(destinationY);
            check(fixture.player.getY() == destinationY, "Below-boundary packet must be accepted");
            check(!fixture.floating(), "The next packet Y below the boundary must clear floating");
            fixture.listener.tick();
            check(fixture.floatingTicks() == 0, "Native below-boundary floating counter must stay zero");
        }
    }

    private static void rejectedAndPendingTeleport(ServerWorld world) {
        try (Fixture fixture = fixture(world)) {
            fixture.move(fixture.player.getY());
            fixture.listener.tick();
            check(fixture.floating() && fixture.floatingTicks() == 1, "Rejection control must begin with real unsupported hover");
            double originalY = fixture.player.getY();
            fixture.listener.handleMovePlayer(new ControlledMovePacket(
                fixture.player.getX() + 1000.0D, originalY, fixture.player.getZ(), false));
            check(fixture.player.getX() == fixture.startX && fixture.player.getY() == originalY,
                "Too-quick movement must be rejected by the real speed check");
            check(fixture.awaitingTeleport(), "Rejected movement must create a real pending correction teleport");
            check(fixture.floating() && fixture.floatingTicks() == 1, "Rejected movement must not clear previous floating state or its counter");
            fixture.listener.tick();
            check(fixture.floatingTicks() == 2, "Native counter must continue after rejected movement");
            fixture.move(originalY - 1.0D);
            check(fixture.player.getY() == originalY, "Pending-teleport movement must not be accepted");
            check(fixture.floating() && fixture.floatingTicks() == 2, "Ignored pending-teleport packet must not clear floating state or its counter");
            fixture.listener.tick();
            check(fixture.floatingTicks() == 3, "Native counter must continue while awaiting teleport acknowledgement");
            check(fixture.listener.disconnectReason == null, "Rejection control must not have reached the timeout");
        }
    }

    private static void nativeGlideExemption(ServerWorld world, ItemStack chest) {
        try (Fixture fixture = fixture(world)) {
            fixture.player.setItemSlot(EquipmentSlotType.CHEST, chest);
            check(fixture.player.tryToStartFallFlying(), "The actual item's native eligibility must start fall-flying");
            // This isolates the real packet-listener exemption using simulated
            // client motion. It does not claim a full gliding trajectory.
            for (int tick = 0; tick < 121; tick++) {
                check(fixture.player.isFallFlying(), "The exemption control must remain actually fall-flying");
                fixture.move(fixture.player.getY());
                check(!fixture.floating(), "Actual fall-flying must exempt unsupported zero-Y packets");
                fixture.listener.tick();
                check(fixture.floatingTicks() == 0, "Native flight must keep the floating timer at zero");
                check(fixture.listener.disconnectReason == null, "Native flight must remain exempt beyond 120 listener ticks");
            }
        }
    }

    private static Fixture fixture(ServerWorld world) {
        check(!world.getServer().isFlightAllowed(), "Network fixtures require allow-flight=false");
        ControlledPlayer player = new ControlledPlayer(world);
        RecordingListener listener = new RecordingListener(player);
        player.setGameMode(GameType.SURVIVAL);
        BlockPos column = world.getSharedSpawnPos();
        int surface = world.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
        player.setPos(column.getX() + 0.5D, surface + 256.0D, column.getZ() + 0.5D);
        player.setOnGround(false);
        player.setLastSectionPos(SectionPos.of(player));
        check(player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && !player.abilities.mayfly,
            "Network fixtures require survival without flight permission");
        check(!player.isNoGravity() && !player.isFallFlying() && !player.hasEffect(Effects.LEVITATION)
                && !player.hasEffect(Effects.SLOW_FALLING),
            "Network fixtures require ordinary gravity and no flight or potion exemption");
        check(world.getBlockStates(player.getBoundingBox().inflate(0.0625D).expandTowards(0, -0.55D, 0))
            .allMatch(state -> state.isAir()), "Network fixtures require an unsupported all-air position");
        world.addNewPlayer(player);
        listener.resetPosition();
        return new Fixture(player, listener);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Fixture implements AutoCloseable {
        final ControlledPlayer player;
        final RecordingListener listener;
        final double startX;

        Fixture(ControlledPlayer player, RecordingListener listener) {
            this.player = player;
            this.listener = listener;
            this.startX = player.getX();
        }

        void move(double y) {
            listener.handleMovePlayer(new ControlledMovePacket(player.getX(), y, player.getZ(), false));
        }

        boolean floating() { return (Boolean) read("clientIsFloating"); }
        int floatingTicks() { return (Integer) read("aboveGroundTickCount"); }
        boolean awaitingTeleport() { return read("awaitingPositionFromClient") != null; }

        private Object read(String name) {
            try {
                Field field = ServerPlayNetHandler.class.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(listener);
            } catch (ReflectiveOperationException error) {
                throw new AssertionError("Cannot observe real listener field " + name, error);
            }
        }

        @Override
        public void close() {
            player.getLevel().removePlayerImmediately(player);
        }
    }

    // The vanilla PositionPacket convenience constructor is client-only and is
    // stripped on a dedicated server. Populate its base protocol fields instead.
    private static final class ControlledMovePacket extends CPlayerPacket {
        ControlledMovePacket(double x, double y, double z, boolean onGround) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.onGround = onGround;
            this.hasPos = true;
        }
    }

    private static final class ControlledPlayer extends FakePlayer {
        ControlledPlayer(ServerWorld world) {
            super(world, new GameProfile(UUID.randomUUID(), "network-fixture"));
        }

        // Incoming packets supply the test client's motion. Movement acceptance
        // and the floating timer still run in the real packet listener.
        @Override
        public void doTick() {}
    }

    private static final class RecordingListener extends ServerPlayNetHandler {
        ITextComponent disconnectReason;

        RecordingListener(ControlledPlayer player) {
            super(player.getLevel().getServer(), new NetworkManager(PacketDirection.SERVERBOUND), player);
        }

        @Override
        public void send(IPacket<?> packet) {}

        @Override
        public void disconnect(ITextComponent reason) {
            disconnectReason = reason;
        }
    }
}
