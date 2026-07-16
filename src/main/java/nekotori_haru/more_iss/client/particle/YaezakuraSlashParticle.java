package nekotori_haru.more_iss.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class YaezakuraSlashParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float startSize;
    private final float endSize = 0.1f;
    private final int maxAge = 8;

    protected YaezakuraSlashParticle(ClientLevel level, double x, double y, double z,
                                     SpriteSet spriteSet, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = spriteSet;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.quadSize = 0.8f;
        this.startSize = 0.8f;
        this.lifetime = maxAge;
        this.hasPhysics = false;
        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();
        float progress = (float) this.age / (float) this.lifetime;
        this.quadSize = Mth.lerp(progress, this.startSize, this.endSize);
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new YaezakuraSlashParticle(level, x, y, z, this.sprites, xSpeed, ySpeed, zSpeed);
        }
    }
}