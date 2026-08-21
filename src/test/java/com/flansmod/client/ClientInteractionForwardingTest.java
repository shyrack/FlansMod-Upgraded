package com.flansmod.client;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the seat-mount fix: right-clicking a block near a
 * driveable must forward an interact (which mounts the player through the
 * seat loop in EntityDriveable.interact), never an attack, because the
 * attack packet triggers the 1.12.2 empty-driveable pickup path in
 * EntityPlane.attackEntityFrom which destroys the plane and drops it.
 *
 * Like EntryPointTest, these tests must never trigger registry bootstrap,
 * so they assert on the compiled bytecode of ClientProxy.
 */
class ClientInteractionForwardingTest {

    private static final String PLAYER_CLICK_BLOCK_DESC =
            "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/InteractionHand;)V";

    private static final String PLAYER_CLICK_INTERACT_DESC =
            "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;)V";

    @Test
    void playerClickBlockTakesHandParameter() throws IOException {
        methodCode(ClientProxy.class, "playerClickBlock", PLAYER_CLICK_BLOCK_DESC);
    }

    @Test
    void playerClickBlockDoesNotAttackDriveables() throws IOException {
        CodeModel code = methodCode(ClientProxy.class, "playerClickBlock", PLAYER_CLICK_BLOCK_DESC);

        boolean forwardsInteract = false;
        for (CodeElement element : code.elementList()) {
            if (!(element instanceof InvokeInstruction inv))
                continue;
            String calledName = inv.name().stringValue();
            if (inv.opcode() == Opcode.INVOKEVIRTUAL && "attack".equals(calledName)) {
                fail("playerClickBlock must not call gameMode.attack(...): " +
                        "the attack packet triggers the empty-driveable pickup that destroys and drops the driveable");
            }
            if ("interact".equals(calledName)) {
                forwardsInteract = true;
            }
        }
        assertTrue(forwardsInteract,
                "playerClickBlock must forward gameMode.interact(...) so block clicks near a driveable mount the player");
    }

    @Test
    void playerClickInteractSkipsForwardingForDriveableParts() throws IOException {
        CodeModel code = methodCode(ClientProxy.class, "playerClickInteract", PLAYER_CLICK_INTERACT_DESC);

        boolean hasDriveableCheck = false;
        boolean hasSeatCheck = false;
        boolean hasWheelCheck = false;
        for (CodeElement element : code.elementList()) {
            if (!(element instanceof TypeCheckInstruction inst))
                continue;
            String type = inst.type().asInternalName();
            if ("com/flansmod/common/driveables/EntityDriveable".equals(type))
                hasDriveableCheck = true;
            if ("com/flansmod/common/driveables/EntitySeat".equals(type))
                hasSeatCheck = true;
            if ("com/flansmod/common/driveables/EntityWheel".equals(type))
                hasWheelCheck = true;
        }
        assertTrue(hasDriveableCheck && hasSeatCheck && hasWheelCheck,
                "playerClickInteract must return early for driveable parts " +
                        "(EntityDriveable, EntitySeat, EntityWheel) so the vanilla interact packet " +
                        "mounts the correct seat without a duplicate seat[0] forward");
    }

    private static CodeModel methodCode(Class<?> owner, String name, String descriptor)
            throws IOException {
        ClassModel model = readClassModel(owner);
        for (MethodModel method : model.methods()) {
            if (method.methodName().equalsString(name)
                    && method.methodType().equalsString(descriptor)) {
                Optional<CodeModel> code = method.code();
                assertTrue(code.isPresent(),
                        owner.getSimpleName() + "." + name + " must have a code body");
                return code.get();
            }
        }
        fail("No method " + name + " " + descriptor + " in compiled " + owner.getName());
        return null; // unreachable
    }

    private static ClassModel readClassModel(Class<?> owner) throws IOException {
        String resourceName = owner.getSimpleName() + ".class";
        try (InputStream in = owner.getResourceAsStream(resourceName)) {
            assertNotNull(in,
                    "Compiled class resource not found: " + resourceName +
                            " (compile main classes first)");
            return ClassFile.of().parse(in.readAllBytes());
        }
    }
}
