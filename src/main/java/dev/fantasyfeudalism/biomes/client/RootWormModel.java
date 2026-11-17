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

/** Original low, segmented root-bark model; animated independently from vanilla silverfish. */
public final class RootWormModel extends HierarchicalModel<RootWorm> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(FantasyBiomes.id("root_worm"), "main");
    private final ModelPart root;
    private final ModelPart[] segments = new ModelPart[9];
    private final ModelPart jaw;

    public RootWormModel(ModelPart root) {
        this.root = root;
        for (int i = 0; i < segments.length; i++) segments[i] = root.getChild("segment" + i);
        jaw = segments[0].getChild("jaw");
    }

    public static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        int[] widths = {8, 8, 8, 7, 7, 6, 5, 4, 2};
        int[] heights = {5, 6, 6, 5, 5, 4, 4, 3, 2};
        int[] lengths = {5, 5, 5, 5, 5, 4, 4, 3, 3};
        float z = -16;
        for (int i = 0; i < widths.length; i++) {
            var segment = root.addOrReplaceChild("segment" + i,
                    CubeListBuilder.create().texOffs(0, i == 0 ? 0 : 12)
                            .addBox(-widths[i] / 2.0F, -heights[i], -lengths[i] / 2.0F, widths[i], heights[i], lengths[i]),
                    PartPose.offset(0, 24, z));
            if (i == 0) {
                // Broad root-bark brow and cheek plates frame a recessed, non-human face.
                segment.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.4F, -5.1F, -3.2F, 8.8F, 1.2F, 2.0F)
                        .addBox(-4.2F, -4.0F, -3.1F, 1.4F, 3.6F, 1.8F)
                        .addBox(2.8F, -4.0F, -3.1F, 1.4F, 3.6F, 1.8F), PartPose.ZERO);
                segment.addOrReplaceChild("eyes", CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-3.1F, -3.9F, -3.25F, 1.8F, 0.55F, 0.6F)
                        .texOffs(48, 0).addBox(1.3F, -3.9F, -3.25F, 1.8F, 0.55F, 0.6F), PartPose.ZERO);
                segment.addOrReplaceChild("maw", CubeListBuilder.create().texOffs(40, 16)
                        .addBox(-2.5F, -3.1F, -3.1F, 5.0F, 2.6F, 0.8F), PartPose.ZERO);
                segment.addOrReplaceChild("upper_teeth", CubeListBuilder.create().texOffs(48, 24)
                        .addBox(-1.8F, -3.1F, -3.7F, 0.7F, 1.0F, 0.6F)
                        .addBox(-0.35F, -3.1F, -3.7F, 0.7F, 0.75F, 0.6F)
                        .addBox(1.1F, -3.1F, -3.7F, 0.7F, 1.0F, 0.6F), PartPose.ZERO);
                segment.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(48, 8)
                        .addBox(-2.7F, -0.6F, -1.4F, 5.4F, 0.9F, 2.0F)
                        .texOffs(48, 24).addBox(-1.6F, -1.5F, -1.4F, 0.7F, 0.9F, 0.6F)
                        .addBox(0.9F, -1.5F, -1.4F, 0.7F, 0.9F, 0.6F), PartPose.offset(0, -0.25F, -2.7F));
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
        }
        segments[0].yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.3F;
        if (worm.isFeeding()) {
            segments[0].xRot = 0.12F + Mth.sin(ageInTicks * 0.5F) * 0.1F;
            jaw.xRot = 0.25F + Mth.sin(ageInTicks * 0.9F) * 0.25F;
        }
    }
}
