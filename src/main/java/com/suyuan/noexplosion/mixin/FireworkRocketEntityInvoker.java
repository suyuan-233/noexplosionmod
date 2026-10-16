package com.suyuan.noexplosion.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * {@code FireworkRocketEntity.explode(ServerLevel)} 是私有方法，
 * 通过 {@link Invoker} 生成一个访问器，好让烟花在指定位置立刻爆开。
 */
@Mixin(FireworkRocketEntity.class)
public interface FireworkRocketEntityInvoker {

	@Invoker("explode")
	void noexplosionmod$explodeNow(ServerLevel level);
}
