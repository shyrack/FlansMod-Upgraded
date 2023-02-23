package com.shyrack.flansmodupgraded.client.model;

import net.minecraft.client.model.ModelBase;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;

public class ModelAttachment extends ModelBase
{
	public ModelRendererTurbo[] attachmentModel = new ModelRendererTurbo[0];
	
	public Vector3f muzzleFlashPoint = new Vector3f();
	/**
	 * For big scopes, so that the player actually looks through them properly
	 */
	public float renderOffset = 0F;
	
	public void renderAttachment(float f)
	{
		for(ModelRendererTurbo model : attachmentModel)
			if(model != null)
				model.render(f);
	}
	
	public void flipAll()
	{
		for(ModelRendererTurbo anAttachmentModel : attachmentModel)
		{
			anAttachmentModel.doMirror(false, true, true);
			anAttachmentModel.setRotationPoint(anAttachmentModel.rotationPointX, -anAttachmentModel.rotationPointY, -anAttachmentModel.rotationPointZ);
		}
	}
	
	protected void translate(ModelRendererTurbo[] model, float x, float y, float z)
	{
		for(ModelRendererTurbo anAttachmentModel : attachmentModel)
		{
			anAttachmentModel.rotationPointX += x;
			anAttachmentModel.rotationPointY += y;
			anAttachmentModel.rotationPointZ += z;
		}
	}
	
	public void translateAll(float x, float y, float z)
	{
		translate(attachmentModel, x, y, z);
	}
}
