package com.suyuan.noexplosion.mixin;

import com.suyuan.noexplosion.NoExplosionMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让带"精准采集"的下界合金镐可以把试炼刷怪笼和宝库挖下来。
 *
 * <p>这两个方块在原版里硬度是 50（能挖），但战利品表是空的，所以挖掉什么都不掉；
 * 这里在方块被破坏时补一次掉落。</p>
 *
 * <ul>
 *   <li><b>试炼刷怪笼</b>：掉落普通物品。曾尝试保留方块实体数据，但放回去仍然要拿刷怪蛋
 *       重新配置，所以已回退，不做数据保留。</li>
 *   <li><b>宝库</b>：保留 {@code ominous}（不祥变种）与 {@code facing} 方块状态，
 *       以及方块实体数据（战利品表、钥匙等），放回去还是原来那个宝库。</li>
 * </ul>
 */
@Mixin(Block.class)
public class SilkTouchHarvestMixin {

	@Inject(method = "playerDestroy", at = @At("HEAD"))
	private void noexplosionmod$dropSelfWithSilkTouch(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool, CallbackInfo ci) {
		Item drop;
		Block block = state.getBlock();
		if (block == Blocks.TRIAL_SPAWNER) {
			drop = Items.TRIAL_SPAWNER;
		} else if (block == Blocks.VAULT) {
			drop = Items.VAULT;
		} else {
			return;
		}

		if (tool.getItem() != Items.NETHERITE_PICKAXE) {
			return;
		}
		if (!hasSilkTouch(tool)) {
			return;
		}

		ItemStack dropped = new ItemStack(drop);
		if (block == Blocks.VAULT) {
			preserveVaultState(state, dropped);
			preserveBlockEntity(level, blockEntity, dropped);
		}
		Block.popResource(level, pos, dropped);
	}

	/**
	 * 保留宝库的方块状态属性，放置时由 {@code BlockItem} 从这里还原。
	 *
	 * <p>{@code ominous} 决定是不是不祥宝库；{@code facing} 保留朝向。
	 * 运行期的 {@code state} 属性故意不保留，交给方块实体自己重新推导。</p>
	 */
	private static void preserveVaultState(BlockState state, ItemStack stack) {
		BlockItemStateProperties properties = BlockItemStateProperties.EMPTY
			.with(VaultBlock.OMINOUS, state)
			.with(VaultBlock.FACING, state);
		if (!properties.isEmpty()) {
			stack.set(DataComponents.BLOCK_STATE, properties);
		}
	}

	/**
	 * 保留方块实体数据（宝库的战利品表与钥匙等）。
	 *
	 * <p>照抄原版中键选取方块时 {@code ServerGamePacketListenerImpl#addBlockDataToItem} 的做法。</p>
	 */
	private static void preserveBlockEntity(ServerLevel level, BlockEntity blockEntity, ItemStack stack) {
		if (blockEntity == null) {
			return;
		}
		try (ProblemReporter.ScopedCollector collector = new ProblemReporter.ScopedCollector(blockEntity.problemPath(), NoExplosionMod.LOGGER)) {
			TagValueOutput output = TagValueOutput.createWithContext(collector, level.registryAccess());
			blockEntity.saveCustomOnly(output);
			blockEntity.removeComponentsFromTag(output);
			BlockItem.setBlockEntityData(stack, blockEntity.getType(), output);
			stack.applyComponents(blockEntity.collectComponents());
		}
	}

	/** 判断工具是否带精准采集。{@code Enchantments.SILK_TOUCH} 是 ResourceKey，用 {@code Holder#is} 比对。 */
	private static boolean hasSilkTouch(ItemStack tool) {
		ItemEnchantments enchantments = tool.getEnchantments();
		for (Holder<Enchantment> holder : enchantments.keySet()) {
			if (holder.is(Enchantments.SILK_TOUCH) && enchantments.getLevel(holder) > 0) {
				return true;
			}
		}
		return false;
	}
}
