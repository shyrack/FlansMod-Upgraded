package com.shyrack.flansmodupgraded.client.model;

import java.util.HashMap;
import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;

import com.shyrack.flansmodupgraded.client.tmt.ModelRendererTurbo;
import com.shyrack.flansmodupgraded.common.driveables.DriveableType;
import com.shyrack.flansmodupgraded.common.driveables.EntityDriveable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class ModelDriveable extends Model {

    public static final float pi = 3.14159265F;
    public static final float tau = 2 * pi;

    public HashMap<String, ModelRendererTurbo[][]> gunModels = new HashMap<>();
    public ModelRendererTurbo[] bodyModel;
    public ModelRendererTurbo[] bodyDoorOpenModel;
    public ModelRendererTurbo[] bodyDoorCloseModel;

    /**
     * Set to true to use the old rotation order (ZYX) rather than (YZX)
     */
    public boolean oldRotateOrder;

    public ModelDriveable(Function<ResourceLocation, RenderType> p_103110_) {
        super(p_103110_);
        this.bodyModel = new ModelRendererTurbo[0];
        this.bodyDoorOpenModel = new ModelRendererTurbo[0];
        this.bodyDoorCloseModel = new ModelRendererTurbo[0];
        this.oldRotateOrder = false;
    }

    /**
     * For rendering a specific entity
     */
    public void render(EntityDriveable driveable, float f1) {
    }

    /**
     * For rendering from GUIs
     */
    public void render(DriveableType type) {
        renderPart(bodyModel);
        renderPart(bodyDoorCloseModel);
        for (ModelRendererTurbo[][] gun : gunModels.values())
            for (ModelRendererTurbo[] gunPart : gun)
                renderPart(gunPart);
    }

    /**
     * Renders the specified parts
     */
    public void renderPart(ModelRendererTurbo[] part) {
        for (ModelRendererTurbo bit : part) {
            bit.render(0.0625F, oldRotateOrder);
        }
    }

    public void registerGunModel(String name, ModelRendererTurbo[][] gunModel) {
        gunModels.put(name, gunModel);
    }

    protected void flip(ModelRendererTurbo[] model) {
        for (ModelRendererTurbo part : model) {
            part.doMirror(false, true, true);
            part.setRotationPoint(part.rotationPointX, -part.rotationPointY, -part.rotationPointZ);
        }
    }

    public void flipAll() {
        flip(bodyModel);
        flip(bodyDoorOpenModel);
        flip(bodyDoorCloseModel);
        for (ModelRendererTurbo[][] modsOfMods : gunModels.values()) {
            for (ModelRendererTurbo[] mods : modsOfMods) {
                flip(mods);
            }
        }
    }

    protected void translate(ModelRendererTurbo[] model, float x, float y, float z) {
        for (ModelRendererTurbo mod : model) {
            mod.rotationPointX += x;
            mod.rotationPointY += y;
            mod.rotationPointZ += z;
        }
    }

    public void translateAll(float x, float y, float z) {
        translate(bodyModel, x, y, z);
        translate(bodyDoorOpenModel, x, y, z);
        translate(bodyDoorCloseModel, x, y, z);
        for (ModelRendererTurbo[][] modsOfMods : gunModels.values()) {
            for (ModelRendererTurbo[] mods : modsOfMods) {
                translate(mods, x, y, z);
            }
        }
    }

    public void translateAll(int x, int y, int z) {
        translateAll((float) x, (float) y, (float) z);
    }

    /**
     * Renders a box with the bounds of the AABB trasnlated by an offset.
     * Copied from Render.class, but without the forced white colour
     */
    public static void renderOffsetAABB(AxisAlignedBB boundingBox, double x, double y, double z) {
        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.setTranslation(x, y, z);
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_NORMAL);
        bufferbuilder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).normal(0.0F, 0.0F, -1.0F).endVertex();
        bufferbuilder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).normal(0.0F, 0.0F, -1.0F).endVertex();
        bufferbuilder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).normal(0.0F, 0.0F, -1.0F).endVertex();
        bufferbuilder.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).normal(0.0F, 0.0F, -1.0F).endVertex();
        bufferbuilder.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).normal(0.0F, 0.0F, 1.0F).endVertex();
        bufferbuilder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).normal(0.0F, 0.0F, 1.0F).endVertex();
        bufferbuilder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).normal(0.0F, 0.0F, 1.0F).endVertex();
        bufferbuilder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).normal(0.0F, 0.0F, 1.0F).endVertex();
        bufferbuilder.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).normal(0.0F, -1.0F, 0.0F).endVertex();
        bufferbuilder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).normal(0.0F, -1.0F, 0.0F).endVertex();
        bufferbuilder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).normal(0.0F, -1.0F, 0.0F).endVertex();
        bufferbuilder.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).normal(0.0F, -1.0F, 0.0F).endVertex();
        bufferbuilder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).normal(0.0F, 1.0F, 0.0F).endVertex();
        bufferbuilder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).normal(0.0F, 1.0F, 0.0F).endVertex();
        bufferbuilder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).normal(0.0F, 1.0F, 0.0F).endVertex();
        bufferbuilder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).normal(0.0F, 1.0F, 0.0F).endVertex();
        bufferbuilder.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).normal(-1.0F, 0.0F, 0.0F).endVertex();
        bufferbuilder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).normal(-1.0F, 0.0F, 0.0F).endVertex();
        bufferbuilder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).normal(-1.0F, 0.0F, 0.0F).endVertex();
        bufferbuilder.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).normal(-1.0F, 0.0F, 0.0F).endVertex();
        bufferbuilder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).normal(1.0F, 0.0F, 0.0F).endVertex();
        bufferbuilder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).normal(1.0F, 0.0F, 0.0F).endVertex();
        bufferbuilder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).normal(1.0F, 0.0F, 0.0F).endVertex();
        bufferbuilder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).normal(1.0F, 0.0F, 0.0F).endVertex();
        tessellator.draw();
        bufferbuilder.setTranslation(0.0D, 0.0D, 0.0D);
        GlStateManager.enableTexture2D();
    }

    // TODO
    @Override
    public void renderToBuffer(PoseStack p_103111_, VertexConsumer p_103112_, int p_103113_, int p_103114_, float p_103115_, float p_103116_, float p_103117_, float p_103118_) {

    }

}
