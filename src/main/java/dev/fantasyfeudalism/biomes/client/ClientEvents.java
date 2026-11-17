package dev.fantasyfeudalism.biomes.client;

import dev.fantasyfeudalism.biomes.FantasyBiomes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.FoliageColor;

@EventBusSubscriber(modid = FantasyBiomes.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FantasyBiomes.ROOT_WORM.get(), RootWormRenderer::new);
    }
    @SubscribeEvent public static void leavesColor(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, index) -> level != null && pos != null ? BiomeColors.getAverageFoliageColor(level, pos) : FoliageColor.getDefaultColor(), FantasyBiomes.ROOTLANDS_LEAVES.get());
    }
    @SubscribeEvent public static void leafItemColor(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 0 ? 0x263A28 : -1, FantasyBiomes.LEAVES_ITEM.get());
    }
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RootWormModel.LAYER, RootWormModel::createLayer);
    }
}
