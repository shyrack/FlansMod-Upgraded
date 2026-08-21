package com.flansmod.common.driveables;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.reflect.Modifier;
import java.util.Optional;

import net.minecraft.world.entity.Entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the passenger-snap fix: the vanilla rideTick
 * (getVehicle().positionRider(this)) must never teleport wheels or seat
 * passengers, since both are positioned manually by the driveable's spring
 * physics and updatePosition().
 *
 * Like EntryPointTest, these tests must never trigger registry bootstrap, so
 * they assert on the compiled bytecode instead of instantiating entities:
 * EntityWheel.rideTick() must exist, must not call super.rideTick() or
 * positionRider, and must tick the wheel; EntitySeat.positionRider(...) must
 * be a no-op that does not delegate to the vanilla attachment point.
 */
class DriveableRidingFixTest {

    @Test
    void wheelOverridesRideTick() throws NoSuchMethodException {
        var method = EntityWheel.class.getMethod("rideTick");
        assertEquals(EntityWheel.class, method.getDeclaringClass(),
                "EntityWheel must override rideTick() itself");
        assertTrue(Modifier.isPublic(method.getModifiers()), "rideTick() must be public");
    }

    @Test
    void wheelRideTickDoesNotCallPositionRiderOrSuperRideTick() throws IOException {
        CodeModel code = methodCode(EntityWheel.class, "rideTick", "()V");

        boolean callsTick = false;
        for (CodeElement element : code.elementList()) {
            if (!(element instanceof InvokeInstruction inv))
                continue;
            String calledName = inv.name().stringValue();
            if (inv.opcode() == Opcode.INVOKESPECIAL && "rideTick".equals(calledName)) {
                fail("EntityWheel.rideTick() must not call super.rideTick(), " +
                        "which would re-add the vanilla positionRider snap");
            }
            if ("positionRider".equals(calledName)) {
                fail("EntityWheel.rideTick() must not call positionRider(...): " +
                        "wheels are positioned by the driveable's spring physics");
            }
            if (inv.opcode() == Opcode.INVOKEVIRTUAL && "tick".equals(calledName)) {
                callsTick = true;
            }
        }
        assertTrue(callsTick,
                "EntityWheel.rideTick() must tick the wheel, like EntitySeat.rideTick()");
    }

    @Test
    void seatOverridesPositionRider() throws NoSuchMethodException {
        var method = EntitySeat.class.getDeclaredMethod(
                "positionRider", Entity.class, Entity.MoveFunction.class);
        assertEquals(EntitySeat.class, method.getDeclaringClass(),
                "EntitySeat must override positionRider itself");
        assertTrue(Modifier.isProtected(method.getModifiers()),
                "positionRider must be protected");
    }

    @Test
    void seatPositionRiderIsNoOp() throws IOException {
        CodeModel code = methodCode(EntitySeat.class, "positionRider",
                "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V");
        for (CodeElement element : code.elementList()) {
            if (element instanceof InvokeInstruction inv) {
                fail("EntitySeat.positionRider must be a no-op so updatePosition() " +
                        "stays authoritative, found call to " + inv.name().stringValue());
            }
        }
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
