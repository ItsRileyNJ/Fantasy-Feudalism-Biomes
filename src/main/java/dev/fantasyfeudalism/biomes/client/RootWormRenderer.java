package dev.fantasyfeudalism.biomes.client;

import dev.fantasyfeudalism.biomes.FantasyBiomes;
import dev.fantasyfeudalism.biomes.entity.RootWorm;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class RootWormRenderer extends MobRenderer<RootWorm, RootWormModel> {
    private static final ResourceLocation TEXTURE = FantasyBiomes.id("textures/entity/root_worm.png");
    public RootWormRenderer(EntityRendererProvider.Context context) {
        super(context, new RootWormModel(context.bakeLayer(RootWormModel.LAYER)), 0.35F);
    }
    @Override public ResourceLocation getTextureLocation(RootWorm entity) { return TEXTURE; }
}
