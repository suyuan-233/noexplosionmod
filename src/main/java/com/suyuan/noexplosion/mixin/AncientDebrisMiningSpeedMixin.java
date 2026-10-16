package com.suyuan.noexplosion.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让试炼刷怪笼和宝库的挖掘速度与远古残骸完全一致。
 *
 * <p>原版这两个方块是 {@code strength(50.0F)}，而且<b>不在 {@code minecraft:mineable/pickaxe}
 * 标签里</b>，所以镐子的速度加成本来就不生效——用下界合金镐挖也按"空手 1.0"计算，
 * 需要 50 × 30 = 1500 tick（约 75 秒）。</p>
 *
 * <p>这里不复刻公式，而是直接拿<b>远古残骸的方块状态</b>去算挖掘进度。这样一来硬度、
 * 工具速度加成、"工具不对时按 100 而不是 30 计"这些细节全部自动与原版远古残骸一致，
 * 也不依赖任何标签或 {@code requiresCorrectToolForDrops} 标记。</p>
 */
@Mixin(BlockBehaviour.class)
public abstract class AncientDebrisMiningSpeedMixin {

	@Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
	private void noexplosionmod$mineLikeAncientDebris(BlockState state, Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
		Block block = state.getBlock();
		if (block != Blocks.TRIAL_SPAWNER && block != Blocks.VAULT) {
			return;
		}

		BlockState ancientDebris = Blocks.ANCIENT_DEBRIS.defaultBlockState();
		float hardness = ancientDebris.getDestroySpeed(level, pos);
		if (hardness == -1.0F) {
			cir.setReturnValue(0.0F);
			return;
		}

		int divisor = player.hasCorrectToolForDrops(ancientDebris) ? 30 : 100;
		cir.setReturnValue(player.getDestroySpeed(ancientDebris) / hardness / (float) divisor);
	}
}
