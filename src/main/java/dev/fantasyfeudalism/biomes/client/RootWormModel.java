package dev.fantasyfeudalism.biomes.client;

import dev.fantasyfeudalism.biomes.FantasyBiomes;
import dev.fantasyfeudalism.biomes.entity.RootWorm;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;

/** Long root-bark worm: simple friendly face, with most detail in its segmented body. */
public final class RootWormModel extends HierarchicalModel<RootWorm> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(FantasyBiomes.id("root_worm"), "main");
    private final ModelPart root;
    private final ModelPart[] segments = new ModelPart[9];
    private final ModelPart jaw;
    private final ModelPart eyes;

    public RootWormModel(ModelPart root) {
        this.root = root;
        for (int i = 0; i < segments.length; i++) segments[i] = root.getChild("segment" + i);
        jaw = segments[0].getChild("jaw");
        eyes = segments[0].getChild("eyes");
    }

    public static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        int[] widths = {8, 8, 8, 7, 7, 6, 5, 4, 2};
        int[] heights = {5, 6, 6, 5, 5, 4, 4, 3, 2};
        int[] lengths = {5, 5, 5, 5, 5, 4, 4, 3, 3};
        float z = -16;
        for (int i = 0; i < widths.length; i++) {
            float w = widths[i], h = heights[i];
            var segment = root.addOrReplaceChild("segment" + i,
                    CubeListBuilder.create().texOffs(0, i == 0 ? 0 : 12)
                            .addBox(-w / 2, -h, -lengths[i] / 2.0F, w, h, lengths[i]),
                    PartPose.offset(0, 24, z));
            if (i == 0) {
                // Large simple eyes and a tiny closed smile replace the armored brow, maw and teeth.
                var eyes = segment.addOrReplaceChild("eyes", CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-3.1F, -0.85F, -0.15F, 1.7F, 1.7F, 0.6F)
                        .texOffs(48, 0).addBox(1.4F, -0.85F, -0.15F, 1.7F, 1.7F, 0.6F),
                        PartPose.offset(0, -3.1F, -2.75F));
                eyes.addOrReplaceChild("highlights", CubeListBuilder.create().texOffs(56, 0)
                        .addBox(-2.9F, -0.65F, -0.3F, 0.55F, 0.55F, 0.25F)
                        .texOffs(56, 0).addBox(1.6F, -0.65F, -0.3F, 0.55F, 0.55F, 0.25F), PartPose.ZERO);
                segment.addOrReplaceChild("cheeks", CubeListBuilder.create().texOffs(56, 8)
                        .addBox(-3.4F, -1.9F, -2.7F, 1.1F, 0.5F, 0.3F)
                        .texOffs(56, 8).addBox(2.3F, -1.9F, -2.7F, 1.1F, 0.5F, 0.3F), PartPose.ZERO);
                segment.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(48, 8)
                        .addBox(-0.9F, 0, -0.2F, 1.8F, 0.25F, 0.25F)
                        .addBox(-1.1F, -0.2F, -0.2F, 0.3F, 0.3F, 0.25F)
                        .addBox(0.8F, -0.2F, -0.2F, 0.3F, 0.3F, 0.25F), PartPose.offset(0, -1.2F, -2.8F));
                segment.addOrReplaceChild("bud", CubeListBuilder.create().texOffs(48, 16)
                        .addBox(-0.7F, -5.55F, -1.5F, 1.4F, 0.55F, 1.1F), PartPose.ZERO);
            } else {
                segment.addOrReplaceChild("bark_ring", CubeListBuilder.create().texOffs(0, 24)
                        .addBox(-w / 2 - 0.15F, -h - 0.15F, -0.55F, w + 0.3F, h + 0.15F, 1.1F), PartPose.ZERO);
                if (i < 8) {
                    segment.addOrReplaceChild("root_nubs", CubeListBuilder.create().texOffs(28, 12)
                            .addBox(-w / 2 - 0.65F, -1.5F, -0.5F, 0.9F, 0.6F, 1.3F)
                            .addBox(w / 2 - 0.25F, -1.5F, -0.5F, 0.9F, 0.6F, 1.3F), PartPose.ZERO);
                    segment.addOrReplaceChild("moss", CubeListBuilder.create().texOffs(48, 16)
                            .addBox(-1.3F, -h - 0.3F, -1.4F, 1.2F, 0.3F, 0.9F)
                            .addBox(0.3F, -h - 0.3F, 0.5F, 0.8F, 0.3F, 1.1F), PartPose.ZERO);
                }
            }
            z += lengths[i] - 0.5F;
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override public ModelPart root() { return root; }

    @Override public void setupAnim(RootWorm worm, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float movement = Math.min(1.0F, limbSwingAmount * 3);
        for (int i = 0; i < segments.length; i++) {
            float wave = limbSwing * 2.2F - i * 0.65F;
            segments[i].yRot = Mth.sin(wave) * 0.22F * movement;
            segments[i].x = Mth.sin(wave) * 1.2F * movement;
            segments[i].y -= Math.abs(Mth.cos(wave)) * 0.5F * movement;
            segments[i].y -= Mth.sin(ageInTicks * 0.08F - i * 0.6F) * 0.06F;
        }
        segments[0].yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.3F;
        eyes.yScale = ageInTicks % 140 > 136 ? 0.15F : 1.0F;
        if (worm.isFeeding()) {
            segments[0].xRot = 0.06F + Mth.sin(ageInTicks * 0.5F) * 0.05F;
            jaw.xRot = 0.04F + Mth.sin(ageInTicks * 0.9F) * 0.08F;
        }
    }
}
