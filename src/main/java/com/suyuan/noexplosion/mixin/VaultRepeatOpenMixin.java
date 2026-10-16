package com.suyuan.noexplosion.mixin;

import java.util.Set;
import java.util.UUID;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.vault.VaultServerData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 解锁宝库"每个玩家只能开一次"的限制，让同一个玩家可以反复开启同一个宝库。
 *
 * <p>限制来自 {@code rewarded_players} 这份记录，而它在<b>两个地方各自生效</b>，
 * 只堵一处是不够的：</p>
 *
 * <ol>
 *   <li>{@code VaultBlockEntity.Server#tryInsertKey} 里查 {@code hasRewardedPlayer}：
 *       为真就播放"已领取"音效并直接 return，钥匙被无视。</li>
 *   <li>{@code VaultSharedData#updateConnectedPlayersWithinRange} 里则是
 *       <b>绕过前者直接读集合</b>：{@code !serverData.getRewardedPlayers().contains(uuid)}，
 *       把已领取的玩家从"附近玩家"里剔除。一旦附近只剩下已领取的玩家，
 *       宝库就会被判定成 {@code INACTIVE}，而 {@code VaultBlock#useItemOn}
 *       对非 {@code ACTIVE} 的状态根本不理会钥匙 —— 表现就是"怎么点都没反应"。</li>
 * </ol>
 *
 * <p>所以这里两处都覆盖：前者恒返回 false，后者恒返回空集合。这样即使方块实体里
 * 已经存着玩家 UUID（或是从被挖下来的宝库 NBT 里带过来的），也不再构成限制。</p>
 *
 * <p>钥匙依旧照常消耗，这是原版行为。</p>
 */
@Mixin(VaultServerData.class)
public class VaultRepeatOpenMixin {

	/** 第一处：不再认为任何玩家"已经领过奖励"。 */
	@Inject(method = "hasRewardedPlayer", at = @At("HEAD"), cancellable = true)
	private void noexplosionmod$alwaysAllowReopen(Player player, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(false);
	}

	/**
	 * 第二处：让"已领取玩家"集合对外恒为空，附近玩家不会再被剔除。
	 *
	 * <p>实测该方法只被上述 lambda 用于 {@code contains} 判断，返回空集合是安全的。</p>
	 */
	@Inject(method = "getRewardedPlayers", at = @At("HEAD"), cancellable = true)
	private void noexplosionmod$emptyRewardedPlayers(CallbackInfoReturnable<Set<UUID>> cir) {
		cir.setReturnValue(Set.of());
	}
}
