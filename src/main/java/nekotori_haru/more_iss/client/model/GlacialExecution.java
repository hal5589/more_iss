package nekotori_haru.more_iss.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import nekotori_haru.more_iss.More_iss;

public class GlacialExecution<T extends Entity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(More_iss.MODID, "glacial_execution"), "main"
    );
    private final ModelPart bone;

    public GlacialExecution(ModelPart root) {
        this.bone = root.getChild("bone");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 11.0F, -7.0F, 0.0F, 1.5708F, -3.1416F));

        bone.addOrReplaceChild("cube_r1",  CubeListBuilder.create().texOffs(-1, 0).addBox(-2.0F, -2.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, -8.0F,  0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r2",  CubeListBuilder.create().texOffs(-1, 0).addBox(-2.0F, -2.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, -2.0F,  0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r3",  CubeListBuilder.create().texOffs(-1, 0).addBox(-2.0F, -2.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, -4.0F,  0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r4",  CubeListBuilder.create().texOffs(-1, 0).addBox(-2.0F, -2.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, -6.0F,  0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r5",  CubeListBuilder.create().texOffs(-1, 0).addBox(-2.0F, -2.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, -10.0F, 0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r6",  CubeListBuilder.create().texOffs(-1, 0).addBox(-2.0F, -2.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F,  0.0F,  0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r7",  CubeListBuilder.create().texOffs(-1, 0).addBox(-2.0F, -2.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F,  2.0F,  0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r8",  CubeListBuilder.create().texOffs(-1, 0).addBox(-2.0F, -2.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F,  4.0F,  0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r9",  CubeListBuilder.create().texOffs(-1, 0).addBox(-2.0F, -2.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -11.0F, 0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(-1, 0).addBox(-2.0F, -2.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F, -8.0F,  0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(-1, 0).addBox(-2.0F, -2.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F, -10.0F, 0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(-1, 0).addBox(-2.0F, -2.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F, -6.0F,  0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(-1, 0).addBox(-2.0F, -2.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F, -4.0F,  0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(-1, 0).addBox(-2.0F, -2.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F, -2.0F,  0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(-1, 0).addBox(-2.0F, -2.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F,  0.0F,  0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(-1, 0).addBox(-2.0F, -2.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F,  2.0F,  0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(-1, 0).addBox(-2.0F, -2.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F,  4.0F,  0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs( 0, 0).addBox(-1.0F, -3.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.0F, 8.0F, 0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs( 0, 0).addBox(-1.0F, -3.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F,  8.0F, 0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs( 0, 0).addBox(-1.0F, -3.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F,  8.0F, 0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs( 0, 0).addBox(-1.0F, -3.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F,  6.0F, 0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs( 0, 0).addBox(-1.0F, -3.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F,  8.0F, 0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs( 0, 0).addBox(-1.0F, -3.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.0F,  8.0F, 0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs( 0, 0).addBox(-1.0F, -3.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.0F,  8.0F, 0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r25", CubeListBuilder.create().texOffs( 0, 0).addBox(-1.0F, -3.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F,  8.0F, 0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r26", CubeListBuilder.create().texOffs( 0, 0).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, 10.0F, 0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r27", CubeListBuilder.create().texOffs( 0, 0).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F,  9.0F, 0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r28", CubeListBuilder.create().texOffs( 0, 0).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, 12.0F, 0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r29", CubeListBuilder.create().texOffs( 0, 0).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, 11.0F, 0.0F, 0.0F, 0.0F, -0.7854F));
        bone.addOrReplaceChild("cube_r30", CubeListBuilder.create().texOffs( 0, 0).addBox(-1.0F, -3.0F, -1.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, 14.0F, 0.0F, 0.0F, 0.0F, -0.7854F));

        return LayerDefinition.create(meshdefinition, 36, 36);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {}

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}