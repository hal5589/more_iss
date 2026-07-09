package nekotori_haru.more_iss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import nekotori_haru.more_iss.entity.LittleMonolithEntity;
import org.joml.Matrix4f;

public class LittleMonolithRenderer extends EntityRenderer<LittleMonolithEntity> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("more_iss", "textures/entity/little_monolith.png");

    public LittleMonolithRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LittleMonolithEntity entity, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // 中心を基準に（高さの半分だけ上に移動）
        poseStack.translate(0, 1, 0);

        float width = 0.8f;
        float height = 2.0f;
        float hw = width / 2;
        float hh = height / 2;

        // 枠線を描画
        VertexConsumer consumer = buffer.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();
        float r = 1.0f, g = 1.0f, b = 1.0f, a = 1.0f;

        Vec3[] corners = {
                new Vec3(-hw, -hh, -hw), new Vec3( hw, -hh, -hw),
                new Vec3( hw, -hh,  hw), new Vec3(-hw, -hh,  hw),
                new Vec3(-hw,  hh, -hw), new Vec3( hw,  hh, -hw),
                new Vec3( hw,  hh,  hw), new Vec3(-hw,  hh,  hw)
        };
        int[][] edges = {
                {0,1},{1,2},{2,3},{3,0},
                {4,5},{5,6},{6,7},{7,4},
                {0,4},{1,5},{2,6},{3,7}
        };

        for (int[] e : edges) {
            Vec3 p1 = corners[e[0]];
            Vec3 p2 = corners[e[1]];
            consumer.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z)
                    .color(r, g, b, a)
                    .uv(0, 0)
                    .overlayCoords(0)
                    .uv2(packedLight)   // ★ lightmap → uv2 に変更
                    .normal(0, 0, 1)
                    .endVertex();
            consumer.vertex(matrix, (float)p2.x, (float)p2.y, (float)p2.z)
                    .color(r, g, b, a)
                    .uv(0, 0)
                    .overlayCoords(0)
                    .uv2(packedLight)   // ★ lightmap → uv2 に変更
                    .normal(0, 0, 1)
                    .endVertex();
        }

        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LittleMonolithEntity entity) {
        return TEXTURE;
    }
}