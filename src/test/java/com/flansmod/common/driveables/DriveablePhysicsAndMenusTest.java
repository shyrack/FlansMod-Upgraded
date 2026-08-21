package com.flansmod.common.driveables;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.Instruction;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.flansmod.common.ModMenus;
import com.flansmod.common.driveables.mechas.ContainerMechaInventory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the driveable physics and menu fixes:
 *
 * <ul>
 *   <li>EntityVehicle.tick() must write the integrated wheel velocity back via
 *       wheel.setDeltaMovement(...) before wheel.move(...), otherwise no
 *       inertia accumulates and steering has no speed-proportional term.</li>
 *   <li>EntityDriveable.hasEnoughFuel() must null-check data.engine for
 *       driveables without an engine part.</li>
 *   <li>The driveable containers must call the AbstractContainerMenu
 *       super(MenuType, int) constructor instead of super(null, 0), so clicks
 *       are applied to a real server-side container in 26.1.</li>
 * </ul>
 *
 * Like EntryPointTest, these tests must never trigger registry bootstrap, so
 * they assert on the compiled bytecode instead of instantiating entities or
 * menus.
 */
class DriveablePhysicsAndMenusTest {

    private static final String ABSTRACT_CONTAINER_MENU = "net/minecraft/world/inventory/AbstractContainerMenu";

    private static final String SET_DELTA_MOVEMENT_DESC = "(DDD)V";

    private static final String MOVE_DESC =
            "(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V";

    private static final Set<String> ENTITY_OWNERS = Set.of(
            "net/minecraft/world/entity/Entity",
            "net/minecraft/world/entity/EntityWheel",
            "net/minecraft/world/entity/LivingEntity",
            "com/flansmod/common/driveables/EntityWheel");

    @Test
    void vehicleTickWritesWheelVelocityBackBeforeMove() throws IOException {
        CodeModel code = methodCode(EntityVehicle.class, "tick", "()V");

        Integer setDeltaMovementAt = null;
        Integer moveAt = null;
        boolean foundWheelWriteBack = false;
        int index = 0;
        for (CodeElement element : code.elementList()) {
            if (element instanceof InvokeInstruction inv) {
                String name = inv.name().stringValue();
                if (inv.opcode() == Opcode.INVOKEVIRTUAL
                        && "setDeltaMovement".equals(name)
                        && SET_DELTA_MOVEMENT_DESC.equals(inv.type().stringValue())) {
                    assertTrue(ENTITY_OWNERS.contains(inv.owner().asInternalName()),
                            "setDeltaMovement in tick must target a wheel/entity, found owner "
                                    + inv.owner().asInternalName());
                    foundWheelWriteBack = true;
                    if (setDeltaMovementAt == null)
                        setDeltaMovementAt = index;
                }
                if (inv.opcode() == Opcode.INVOKEVIRTUAL
                        && "move".equals(name)
                        && MOVE_DESC.equals(inv.type().stringValue())
                        && moveAt == null) {
                    moveAt = index;
                }
            }
            index++;
        }

        assertTrue(foundWheelWriteBack,
                "EntityVehicle.tick() must write the integrated velocity back via "
                        + "wheel.setDeltaMovement(x, y, z)");
        assertNotNull(moveAt, "EntityVehicle.tick() must move wheels");
        assertNotNull(setDeltaMovementAt, "EntityVehicle.tick() must call setDeltaMovement");
        assertTrue(setDeltaMovementAt < moveAt,
                "The velocity write-back must happen before wheel.move(...), "
                        + "like upstream 1.12.2 mutated wheel.motionX/Y/Z in place");
    }

    @Test
    void driveableHasEnoughFuelNullChecksEngine() throws IOException {
        CodeModel code = methodCode(EntityDriveable.class, "hasEnoughFuel", "()Z");

        boolean readsEngine = false;
        boolean hasNullBranch = false;
        for (CodeElement element : code.elementList()) {
            if (element instanceof FieldInstruction field
                    && "engine".equals(field.name().stringValue())) {
                readsEngine = true;
            }
            if (element instanceof Instruction ins
                    && (ins.opcode() == Opcode.IFNULL || ins.opcode() == Opcode.IFNONNULL)) {
                hasNullBranch = true;
            }
        }
        assertTrue(readsEngine,
                "hasEnoughFuel() must read driveableData.engine so it can guard against null");
        assertTrue(hasNullBranch,
                "hasEnoughFuel() must null-check driveableData.engine: driveables without "
                        + "an engine part would NPE here");
    }

    @Test
    void driveableMenuContainerUsesMenuTypeConstructor() throws IOException {
        assertConstructorCallsSuperWithMenuType(ContainerDriveableMenu.class);
    }

    @Test
    void driveableInventoryContainerUsesMenuTypeConstructor() throws IOException {
        assertConstructorCallsSuperWithMenuType(ContainerDriveableInventory.class);
    }

    @Test
    void mechaInventoryContainerUsesMenuTypeConstructor() throws IOException {
        assertConstructorCallsSuperWithMenuType(ContainerMechaInventory.class);
    }

    private static void assertConstructorCallsSuperWithMenuType(Class<?> owner) throws IOException {
        ClassModel model = readClassModel(owner);
        boolean foundConstructor = false;
        for (MethodModel method : model.methods()) {
            if (!"<init>".equals(method.methodName().stringValue()))
                continue;
            foundConstructor = true;
            Optional<CodeModel> code = method.code();
            assertTrue(code.isPresent(), owner.getSimpleName() + " constructor must have a body");
            assertTrue(callsSuperMenuTypeConstructor(code.get()),
                    owner.getSimpleName() + " must call super(menuType, containerId), "
                            + "not super(null, 0), so clicks land in a real server-side container");
        }
        assertTrue(foundConstructor, owner.getSimpleName() + " must declare a constructor");
    }

    private static boolean callsSuperMenuTypeConstructor(CodeModel code) {
        List<CodeElement> elements = code.elementList();
        for (int i = 0; i < elements.size(); i++) {
            CodeElement element = elements.get(i);
            if (!(element instanceof InvokeInstruction inv))
                continue;
            if (inv.opcode() != Opcode.INVOKESPECIAL
                    || !"<init>".equals(inv.name().stringValue())
                    || !ABSTRACT_CONTAINER_MENU.equals(inv.owner().asInternalName())
                    || !"(Lnet/minecraft/world/inventory/MenuType;I)V"
                            .equals(inv.type().stringValue())) {
                continue;
            }
            //The super(menuType, containerId) call must not be fed a null MenuType,
            //as the old super(null, 0) constructors did
            for (int j = Math.max(0, i - 2); j < i; j++) {
                if (elements.get(j) instanceof Instruction ins
                        && ins.opcode() == Opcode.ACONST_NULL) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Test
    void modMenusRegistersExtendedMenuTypes() throws NoSuchFieldException {
        assertExtendedMenuTypeField(ModMenus.class, "DRIVEABLE_MENU");
        assertExtendedMenuTypeField(ModMenus.class, "DRIVEABLE_FUEL");
        assertExtendedMenuTypeField(ModMenus.class, "DRIVEABLE_INVENTORY");
        assertExtendedMenuTypeField(ModMenus.class, "MECHA_INVENTORY");
    }

    private static void assertExtendedMenuTypeField(Class<?> owner, String fieldName)
            throws NoSuchFieldException {
        Field field = owner.getDeclaredField(fieldName);
        assertEquals("net.fabricmc.fabric.api.menu.v1.ExtendedMenuType",
                field.getType().getName(),
                owner.getSimpleName() + "." + fieldName + " must be an ExtendedMenuType so the "
                        + "driveable id is synced with the open screen packet");
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
