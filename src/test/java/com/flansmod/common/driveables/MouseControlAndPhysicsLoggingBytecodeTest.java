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
import java.lang.reflect.AccessFlag;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.flansmod.client.handlers.MouseInputHandler;
import com.flansmod.common.driveables.mechas.EntityMecha;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bytecode regression tests for the plane mouse-control fix and the physics
 * logging. Like the other tests in this project, these must never trigger
 * registry bootstrap, so they assert on the compiled bytecode only.
 *
 * The mixin itself cannot be verified via JUnit bytecode; it is verified
 * in-game.
 */
class MouseControlAndPhysicsLoggingBytecodeTest
{
	@Test
	void entitySeatOnMouseMovedTakesTwoDoubles() throws IOException
	{
		MethodModel method = findMethod(EntitySeat.class, "onMouseMoved");
		assertEquals("(DD)V", method.methodType().stringValue(),
				"EntitySeat.onMouseMoved must take double deltas so sub-pixel "
						+ "mouse movement survives the capture");
	}

	@Test
	void entityDriveableOnMouseMovedTakesTwoDoubles() throws IOException
	{
		MethodModel method = findMethod(EntityDriveable.class, "onMouseMoved");
		assertEquals("(DD)V", method.methodType().stringValue(),
				"EntityDriveable.onMouseMoved must declare the double signature");
	}

	@Test
	void planeTickInvokesPhysicsLogging() throws IOException
	{
		assertInvokesLogPhysicsTick(EntityPlane.class);
	}

	@Test
	void vehicleTickInvokesPhysicsLogging() throws IOException
	{
		assertInvokesLogPhysicsTick(EntityVehicle.class);
	}

	@Test
	void mechaTickInvokesPhysicsLogging() throws IOException
	{
		assertInvokesLogPhysicsTick(EntityMecha.class);
	}

	@Test
	void mouseInputHandlerCaptureAndFlushAreStatic() throws IOException
	{
		ClassModel model = readClassModel(MouseInputHandler.class);
		boolean captureStatic = false;
		boolean flushStatic = false;
		for(MethodModel method : model.methods())
		{
			if("captureMouse".equals(method.methodName().stringValue()))
			{
				captureStatic = method.flags().has(AccessFlag.STATIC);
				assertEquals("(DDZ)V", method.methodType().stringValue(),
						"captureMouse must take (double dx, double dy, boolean screenOpen)");
			}
			if("flushMouse".equals(method.methodName().stringValue()))
			{
				flushStatic = method.flags().has(AccessFlag.STATIC);
			}
		}
		assertTrue(captureStatic,
				"MouseInputHandler.captureMouse must be static (Minecraft-free API)");
		assertTrue(flushStatic,
				"MouseInputHandler.flushMouse must be static (Minecraft-free API)");
	}

	private static void assertInvokesLogPhysicsTick(Class<?> owner) throws IOException
	{
		MethodModel tick = findMethod(owner, "tick", "()V");
		Optional<CodeModel> code = tick.code();
		assertTrue(code.isPresent(), owner.getSimpleName() + ".tick must have a body");
		boolean invokesLogPhysicsTick = false;
		for(CodeElement element : code.get().elementList())
		{
			if(element instanceof InvokeInstruction inv
					&& inv.opcode() == Opcode.INVOKEVIRTUAL
					&& "logPhysicsTick".equals(inv.name().stringValue()))
			{
				invokesLogPhysicsTick = true;
			}
		}
		assertTrue(invokesLogPhysicsTick,
				owner.getSimpleName() + ".tick must call logPhysicsTick so every ticking "
						+ "driveable emits telemetry when LogDriveablePhysics is enabled");
	}

	private static MethodModel findMethod(Class<?> owner, String name) throws IOException
	{
		ClassModel model = readClassModel(owner);
		for(MethodModel method : model.methods())
		{
			if(method.methodName().equalsString(name))
				return method;
		}
		fail("No method " + name + " in compiled " + owner.getName());
		return null; // unreachable
	}

	private static MethodModel findMethod(Class<?> owner, String name, String descriptor)
			throws IOException
	{
		ClassModel model = readClassModel(owner);
		for(MethodModel method : model.methods())
		{
			if(method.methodName().equalsString(name)
					&& method.methodType().equalsString(descriptor))
			{
				return method;
			}
		}
		fail("No method " + name + " " + descriptor + " in compiled " + owner.getName());
		return null; // unreachable
	}

	private static ClassModel readClassModel(Class<?> owner) throws IOException
	{
		String resourceName = owner.getSimpleName() + ".class";
		try(InputStream in = owner.getResourceAsStream(resourceName))
		{
			assertNotNull(in,
					"Compiled class resource not found: " + resourceName
							+ " (compile main classes first)");
			return ClassFile.of().parse(in.readAllBytes());
		}
	}
}
