package net.dragonloot.network;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.util.UUID;
import net.dragonloot.DragonLootMain;
import net.dragonloot.init.ItemInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Drives the real packet handler and floating timer; only transport and client motion are simulated. */
@GameTestHolder(DragonLootMain.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ServerMovementGameTests {
    private ServerMovementGameTests() {}

    @GameTest(template = "empty", batch = "dragonloot.network")
    public static void acceptedLongDescentDoesNotKick(GameTestHelper helper) {
        try (Fixture fixture = fixture(helper)) {
            double startY = fixture.player.getY();
            for (int tick = 0; tick < 121; tick++) {
                double destinationY = fixture.player.getY() - 1.0D;
                fixture.move(destinationY);
                check(helper, fixture.player.getY() == destinationY, "Descending packet must actually be accepted");
                check(helper, !fixture.floating(), "Accepted descent must clear floating");
                fixture.listener.tick();
                equal(helper, fixture.floatingTicks(), 0, "floating ticks during accepted descent");
                check(helper, fixture.listener.disconnectReason == null, "A legitimate descent must remain connected beyond 120 listener ticks");
            }
            check(helper, fixture.player.getY() == startY - 121.0D, "The test must include 121 actual accepted moves");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "dragonloot.network")
    public static void illegalHoverStillKicksAtEightyOneTicks(GameTestHelper helper) {
        try (Fixture fixture = fixture(helper)) {
            for (int tick = 1; tick <= 81; tick++) {
                fixture.move(fixture.player.getY());
                check(helper, fixture.floating(), "Unsupported zero-Y packets must still set floating");
                fixture.listener.tick();
                equal(helper, fixture.floatingTicks(), tick, "native floating counter");
                if (tick <= 80) {
                    check(helper, fixture.listener.disconnectReason == null, "Vanilla permits exactly 80 floating ticks");
                }
            }
            check(helper, fixture.listener.disconnectReason != null, "The native listener must reject hover at tick 81");
            check(helper, fixture.listener.disconnectReason.getString().equals(Component.translatable("multiplayer.disconnect.flying").getString()),
                "The native disconnect reason must be flying, not an unrelated transport error");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "dragonloot.network")
    public static void exactVerticalThresholdMatchesVanilla(GameTestHelper helper) {
        try (Fixture fixture = fixture(helper)) {
            double startY = fixture.player.getY();
            double destinationY = startY - 0.03125D;
            check(helper, destinationY - startY == -0.03125D, "Boundary fixture must represent the exact accepted delta");
            fixture.move(destinationY);
            check(helper, fixture.player.getY() == destinationY, "Boundary packet must be accepted");
            check(helper, fixture.floating(), "Exactly -0.03125 must remain within the floating threshold");
            fixture.listener.tick();
            equal(helper, fixture.floatingTicks(), 1, "floating ticks at the exact boundary");
        }
        try (Fixture fixture = fixture(helper)) {
            double startY = fixture.player.getY();
            // nextDown is applied to the packet coordinate: adding nextDown(delta)
            // to a large Y coordinate would round back to the exact boundary.
            double destinationY = Math.nextDown(startY - 0.03125D);
            check(helper, destinationY - startY < -0.03125D, "Below-boundary fixture must survive coordinate rounding");
            fixture.move(destinationY);
            check(helper, fixture.player.getY() == destinationY, "Below-boundary packet must be accepted");
            check(helper, !fixture.floating(), "The next representable packet Y below the boundary must clear floating");
            fixture.listener.tick();
            equal(helper, fixture.floatingTicks(), 0, "floating ticks below the boundary");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "dragonloot.network")
    public static void rejectedAndPendingTeleportPacketsDoNotClearHover(GameTestHelper helper) {
        try (Fixture fixture = fixture(helper)) {
            fixture.move(fixture.player.getY());
            fixture.listener.tick();
            check(helper, fixture.floating(), "Rejection control must start with real unsupported hover");
            equal(helper, fixture.floatingTicks(), 1, "initial hover counter");

            double originalY = fixture.player.getY();
            fixture.listener.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(
                fixture.player.getX() + 1000.0D, originalY, fixture.player.getZ(), false));
            check(helper, fixture.player.getX() == fixture.startX && fixture.player.getY() == originalY,
                "Too-quick packet must be rejected by the real speed check");
            check(helper, fixture.awaitingTeleport(), "Rejected movement must create a real pending correction teleport");
            check(helper, fixture.floating(), "Rejected movement must not clear the previous floating state");
            equal(helper, fixture.floatingTicks(), 1, "rejected movement must not reset the counter");
            fixture.listener.tick();
            equal(helper, fixture.floatingTicks(), 2, "hover counter after rejected movement");

            fixture.move(originalY - 1.0D);
            check(helper, fixture.player.getY() == originalY, "A packet awaiting teleport acknowledgement must not be accepted");
            check(helper, fixture.floating(), "Ignored pending-teleport movement must not clear floating");
            equal(helper, fixture.floatingTicks(), 2, "pending teleport must not reset the counter");
            fixture.listener.tick();
            equal(helper, fixture.floatingTicks(), 3, "native hover counter while awaiting acknowledgement");
            check(helper, fixture.listener.disconnectReason == null, "The rejection control must not have reached the timeout");
        }
        helper.succeed();
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }

    private static void equal(GameTestHelper helper, int actual, int expected, String message) {
        if (actual != expected) helper.fail(message + ": expected " + expected + ", got " + actual);
    }

    @GameTest(template = "empty", batch = "dragonloot.network")
    public static void nativeElytraStartStillExemptsFloating(GameTestHelper helper) {
        assertNativeGlideExemption(helper, new ItemStack(Items.ELYTRA));
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "dragonloot.network")
    public static void nativeDragonWingsStartStillExemptsFloating(GameTestHelper helper) {
        assertNativeGlideExemption(helper, new ItemStack(ItemInit.UPGRADED_DRAGON_CHESTPLATE.get()));
        helper.succeed();
    }

    private static void assertNativeGlideExemption(GameTestHelper helper, ItemStack chest) {
        try (Fixture fixture = fixture(helper)) {
            fixture.player.setItemSlot(EquipmentSlot.CHEST, chest);
            check(helper, fixture.player.tryToStartFallFlying(),
                "The actual equipped item's native eligibility must start fall-flying");
            // Motion is simulated here. This checks native start eligibility and
            // the real packet-listener exemption, not a full gliding trajectory.
            for (int tick = 0; tick < 121; tick++) {
                check(helper, fixture.player.isFallFlying(), "The exemption control must remain actually fall-flying");
                fixture.move(fixture.player.getY());
                check(helper, !fixture.floating(), "Actual fall-flying must exempt unsupported zero-Y packets");
                fixture.listener.tick();
                equal(helper, fixture.floatingTicks(), 0, "floating ticks during native fall-flying");
                check(helper, fixture.listener.disconnectReason == null, "Legitimate native flight must remain exempt beyond 120 listener ticks");
            }
        }
    }

    private static Fixture fixture(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        check(helper, !level.getServer().isFlightAllowed(), "Network fixtures require allow-flight=false");
        ControlledPlayer player = new ControlledPlayer(level);
        RecordingListener listener = new RecordingListener(player);
        player.setGameMode(GameType.SURVIVAL);
        BlockPos column = helper.absolutePos(new BlockPos(1, 1, 1));
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
        player.setPos(column.getX() + 0.5D, surface + 256.0D, column.getZ() + 0.5D);
        player.setOnGround(false);
        player.setLastSectionPos(SectionPos.of(player));
        check(helper, player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && !player.getAbilities().mayfly,
            "Network fixtures require survival without flight permission");
        check(helper, !player.isNoGravity() && !player.isFallFlying() && !player.hasEffect(MobEffects.LEVITATION)
                && !player.hasEffect(MobEffects.SLOW_FALLING),
            "Network fixtures require ordinary gravity and no flight or potion exemption");
        check(helper, level.getBlockStates(player.getBoundingBox().inflate(0.0625D).expandTowards(0, -0.55D, 0))
            .allMatch(state -> state.isAir()), "Network fixtures require an unsupported all-air position");
        level.addNewPlayer(player);
        listener.resetPosition();
        return new Fixture(player, listener);
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
            listener.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(player.getX(), y, player.getZ(), false));
        }

        boolean floating() {
            return (Boolean) read("clientIsFloating");
        }

        int floatingTicks() {
            return (Integer) read("aboveGroundTickCount");
        }

        boolean awaitingTeleport() {
            return read("awaitingPositionFromClient") != null;
        }

        private Object read(String name) {
            try {
                Field field = ServerGamePacketListenerImpl.class.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(listener);
            } catch (ReflectiveOperationException error) {
                throw new AssertionError("Cannot observe the real listener field " + name, error);
            }
        }

        @Override
        public void close() {
            player.serverLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
    }

    private static final class ControlledPlayer extends FakePlayer {
        ControlledPlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "network-fixture"));
        }

        // Incoming packets supply the simulated client's motion; the real listener
        // still performs movement, collision acceptance and all floating ticks.
        @Override
        public void doTick() {}
    }

    private static final class RecordingListener extends ServerGamePacketListenerImpl {
        Component disconnectReason;

        RecordingListener(ControlledPlayer player) {
            super(player.serverLevel().getServer(), new Connection(PacketFlow.SERVERBOUND), player);
        }

        @Override
        public void send(Packet<?> packet) {}

        @Override
        public void disconnect(Component reason) {
            disconnectReason = reason;
        }
    }
}

