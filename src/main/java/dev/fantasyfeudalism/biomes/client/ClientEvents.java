package dev.fantasyfeudalism.biomes.client;

import dev.fantasyfeudalism.biomes.FantasyBiomes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = FantasyBiomes.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FantasyBiomes.ROOT_WORM.get(), RootWormRenderer::new);
    }
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RootWormModel.LAYER, RootWormModel::createLayer);
    }
}
