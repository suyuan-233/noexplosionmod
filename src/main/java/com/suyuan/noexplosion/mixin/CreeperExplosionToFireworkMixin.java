package com.suyuan.noexplosion.mixin;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 把苦力怕的爆炸换成一次烟花爆开。
 *
 * <p>只拦截 {@code explodeCreeper()} 里那一次 {@code ServerLevel.explode(...)} 调用，
 * 方法里的其余原版流程（dead 标记、喷溅药水云、死亡效果、discard）全部原样执行，
 * 所以苦力怕的死亡语义和原版保持一致，唯一的差别是"不再产生爆炸"。</p>
 */
@Mixin(Creeper.class)
public class CreeperExplosionToFireworkMixin {

	/**
	 * 用来随机取色的染料色。
	 *
	 * <p>刻意排除白、淡灰、灰、黑、棕——这些颜色做出来的烟花不够"彩"。</p>
	 */
	private static final DyeColor[] VIVID_COLORS = {
		DyeColor.RED,
		DyeColor.ORANGE,
		DyeColor.YELLOW,
		DyeColor.LIME,
		DyeColor.GREEN,
		DyeColor.CYAN,
		DyeColor.LIGHT_BLUE,
		DyeColor.BLUE,
		DyeColor.PURPLE,
		DyeColor.MAGENTA,
		DyeColor.PINK
	};

	/** 一次爆开使用几种颜色。 */
	private static final int COLOR_COUNT = 3;

	@Redirect(
		method = "explodeCreeper",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)V"
		)
	)
	private void noexplosionmod$releaseFirework(ServerLevel level, Entity source, double x, double y, double z, float radius, Level.ExplosionInteraction interaction) {
		FireworkRocketEntity firework = new FireworkRocketEntity(level, x, y, z, createFireworkStack(level.getRandom()));

		// 必须先加入世界：客户端要先收到实体，才能正确渲染爆开的粒子。
		level.addFreshEntity(firework);

		// 立刻在原地引爆，而不是让火箭升空。
		((FireworkRocketEntityInvoker) firework).noexplosionmod$explodeNow(level);
	}

	/** 做一个普通的小球烟花：随机 3 种鲜艳颜色，白色收尾。 */
	private static ItemStack createFireworkStack(RandomSource random) {
		ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
		rocket.set(DataComponents.FIREWORKS, new Fireworks(
			1,
			List.of(new FireworkExplosion(
				FireworkExplosion.Shape.SMALL_BALL,
				pickDistinctColors(random, COLOR_COUNT),
				IntList.of(DyeColor.WHITE.getFireworkColor()),
				true,
				true))
		));
		return rocket;
	}

	/** 随机挑 count 种互不相同的颜色。 */
	private static IntList pickDistinctColors(RandomSource random, int count) {
		IntArrayList colors = new IntArrayList(count);
		while (colors.size() < count) {
			int color = VIVID_COLORS[random.nextInt(VIVID_COLORS.length)].getFireworkColor();
			if (!colors.contains(color)) {
				colors.add(color);
			}
		}
		return colors;
	}
}
