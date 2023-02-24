package com.shyrack.flansmodupgraded.yeolde.client.model;

import net.minecraft.client.model.Model;
import net.minecraft.world.entity.Entity;

import com.shyrack.flansmodupgraded.client.tmt.ModelRendererTurbo;

public class ModelRock extends Model {

    public ModelRendererTurbo rockModel;

    public ModelRock() {
        rockModel = new ModelRendererTurbo(this, 0, 0, 8, 8);
        rockModel.addBox(-1F, -1F, -1F, 2, 2, 2);
    }

    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        rockModel.render(f5);
    }

    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5) {
    }

}
