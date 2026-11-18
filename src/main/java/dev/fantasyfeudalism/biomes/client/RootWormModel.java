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

/** Long root-bark worm: a plated snout and articulated bite, with most detail in its segmented body. */
public final class RootWormModel extends HierarchicalModel<RootWorm> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(FantasyBiomes.id("root_worm"), "main");
    private final ModelPart root;
    private final ModelPart[] segments = new ModelPart[9];
    private final ModelPart upperJaw;
    private final ModelPart lowerJaw;
    private final ModelPart leftJaw;
    private final ModelPart rightJaw;
    private final ModelPart eyes;

    public RootWormModel(ModelPart root) {
        this.root = root;
        for (int i = 0; i < segments.length; i++) segments[i] = root.getChild("segment" + i);
        upperJaw = segments[0].getChild("upper_jaw");
        lowerJaw = segments[0].getChild("lower_jaw");
        leftJaw = segments[0].getChild("left_jaw");
        rightJaw = segments[0].getChild("right_jaw");
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
                    CubeListBuilder.create().texOffs(0, i == 0 ? 0 : 16)
                            .addBox(-w / 2, -h, -lengths[i] / 2.0F, w, h, lengths[i]),
                    PartPose.offset(0, 24, z));
            if (i == 0) {
                // Recessed lateral eyes sit beneath layered bark plates, without cartoon cheeks or a smile.
                segment.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-4.2F, -5.2F, -2.9F, 2.0F, 0.9F, 2.4F)
                        .addBox(2.2F, -5.2F, -2.9F, 2.0F, 0.9F, 2.4F)
                        .texOffs(0, 40).addBox(-2.9F, -5.45F, -1.9F, 5.8F, 0.7F, 3.2F), PartPose.ZERO);
                segment.addOrReplaceChild("eyes", CubeListBuilder.create().texOffs(96, 0)
                        .addBox(-4.15F, -0.325F, -0.55F, 0.25F, 0.65F, 1.1F)
                        .addBox(3.9F, -0.325F, -0.55F, 0.25F, 0.65F, 1.1F), PartPose.offset(0, -3.775F, -2.25F));
                // The throat is behind the closed overlapping plates; it becomes visible as they spread.
                segment.addOrReplaceChild("throat", CubeListBuilder.create().texOffs(96, 16)
                        .addBox(-2.8F, -4.6F, -2.7F, 5.6F, 4.3F, 0.15F), PartPose.ZERO);
                var upper = segment.addOrReplaceChild("upper_jaw", CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-3.0F, -2.2F, -3.6F, 6.0F, 2.25F, 3.9F)
                        .texOffs(0, 40).addBox(-2.6F, -2.65F, -2.6F, 5.2F, 0.55F, 2.3F),
                        PartPose.offset(0, -2.45F, -1.7F));
                upper.addOrReplaceChild("toothlets", CubeListBuilder.create().texOffs(112, 0)
                        .addBox(-2.15F, 0, -3.0F, 0.55F, 0.75F, 0.7F)
                        .addBox(1.6F, 0, -3.0F, 0.55F, 0.75F, 0.7F), PartPose.ZERO);
                var lower = segment.addOrReplaceChild("lower_jaw", CubeListBuilder.create().texOffs(32, 16)
                        .addBox(-2.8F, 0, -3.5F, 5.6F, 2.05F, 3.8F), PartPose.offset(0, -2.45F, -1.7F));
                lower.addOrReplaceChild("toothlets", CubeListBuilder.create().texOffs(112, 0)
                        .addBox(-1.8F, -0.65F, -2.9F, 0.5F, 0.7F, 0.65F)
                        .addBox(1.3F, -0.65F, -2.9F, 0.5F, 0.7F, 0.65F), PartPose.ZERO);
                segment.addOrReplaceChild("left_jaw", CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-1.0F, -1.55F, -3.0F, 1.2F, 3.1F, 3.4F), PartPose.offset(-2.9F, -2.5F, -1.9F));
                segment.addOrReplaceChild("right_jaw", CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-0.2F, -1.55F, -3.0F, 1.2F, 3.1F, 3.4F), PartPose.offset(2.9F, -2.5F, -1.9F));
            } else {
                segment.addOrReplaceChild("bark_ring", CubeListBuilder.create().texOffs(0, 40)
                        .addBox(-w / 2 - 0.15F, -h - 0.15F, -0.55F, w + 0.3F, h + 0.15F, 1.1F), PartPose.ZERO);
                if (i < 8) {
                    segment.addOrReplaceChild("root_nubs", CubeListBuilder.create().texOffs(32, 16)
                            .addBox(-w / 2 - 0.65F, -1.5F, -0.5F, 0.9F, 0.6F, 1.3F)
                            .addBox(w / 2 - 0.25F, -1.5F, -0.5F, 0.9F, 0.6F, 1.3F), PartPose.ZERO);
                    segment.addOrReplaceChild("moss", CubeListBuilder.create().texOffs(96, 32)
                            .addBox(-1.3F, -h - 0.3F, -1.4F, 1.2F, 0.3F, 0.9F)
                            .addBox(0.3F, -h - 0.3F, 0.5F, 0.8F, 0.3F, 1.1F), PartPose.ZERO);
                }
            }
            z += lengths[i] - 0.5F;
        }
        return LayerDefinition.create(mesh, 128, 64);
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
        float mouth = worm.getMouthOpenAmount(ageInTicks - worm.tickCount);
        upperJaw.xRot = -mouth * 0.75F;
        lowerJaw.xRot = mouth * 0.72F;
        leftJaw.yRot = mouth * 0.58F;
        rightJaw.yRot = -mouth * 0.58F;
        if (worm.isFeeding()) {
            segments[0].xRot = 0.06F + Mth.sin(ageInTicks * 0.5F) * 0.05F;
            // Root nibbling remains a small head motion with the mouth closed.
        }
    }
}
