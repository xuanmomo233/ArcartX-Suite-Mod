package xuanmo.arcartx_suite_mod.client;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.item.GunTooltipPart;
import com.tacz.guns.item.ModernKineticGunItem;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AllowAttachmentTagMatcher;
import com.tacz.guns.util.AttachmentDataUtils;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * TACZ 枪属性 tooltip 文本采集器。
 * <p>
 * 精确复刻 {@code ClientGunTooltip.getText()} 的计算逻辑，将枪属性转为
 * {@code Component} 文本行，供 {@code ItemTooltipEvent} 注入。
 * <p>
 * 直接调用 TACZ public API（{@code IGun}、{@code TimelessAPI}、
 * {@code AttachmentDataUtils}），不使用反射。
 */
public final class TaczTooltipCollector {

    private static final DecimalFormat FORMAT = new DecimalFormat("#.##%");
    private static final DecimalFormat FORMAT_P_D1 = new DecimalFormat("#.#%");
    private static final DecimalFormat DAMAGE_FORMAT = new DecimalFormat("#.##");
    private static final DecimalFormat CURRENT_AMMO_FORMAT_PERCENT = new DecimalFormat("0%");

    private TaczTooltipCollector() {}

    /**
     * 采集 TACZ 枪属性的 tooltip 文本行。
     *
     * @param stack 物品栈
     * @return 文本行列表，如果不是 TACZ 枪则返回空列表
     */
    public static List<Component> collectTooltipLines(ItemStack stack) {
        IGun iGun = IGun.getIGunOrNull(stack);
        if (iGun == null) {
            return List.of();
        }

        ResourceLocation gunId = iGun.getGunId(stack);
        Optional<CommonGunIndex> optIndex = TimelessAPI.getCommonGunIndex(gunId);
        if (optIndex.isEmpty()) {
            return List.of();
        }

        CommonGunIndex gunIndex = optIndex.get();
        GunData gunData = gunIndex.getGunData();
        BulletData bulletData = gunIndex.getBulletData();
        int hideFlags = GunTooltipPart.getHideFlags(stack);

        List<Component> lines = new ArrayList<>();

        // ── 弹药信息 ──
        if (shouldShow(GunTooltipPart.AMMO_INFO, hideFlags)) {
            ResourceLocation ammoId = gunData.getAmmoId();
            if (ammoId != null) {
                ItemStack ammo = AmmoItemBuilder.create().setId(ammoId).build();
                lines.add(ammo.getHoverName().copy().withStyle(ChatFormatting.GOLD));

                int barrelBullet = (iGun.hasBulletInBarrel(stack)
                    && gunData.getBolt() != Bolt.OPEN_BOLT) ? 1 : 0;
                int maxAmmo = AttachmentDataUtils.getAmmoCountWithAttachment(stack, gunData) + barrelBullet;
                int currentAmmo = iGun.getCurrentAmmoCount(stack) + barrelBullet;

                MutableComponent ammoCountText;
                if (iGun.useInventoryAmmo(stack)) {
                    ammoCountText = Component.translatable("tooltip.tacz.gun.inventory_mode")
                        .withStyle(ChatFormatting.YELLOW);
                } else if (!iGun.useDummyAmmo(stack)) {
                    ammoCountText = Component.literal("%d/%d".formatted(currentAmmo, maxAmmo))
                        .withStyle(ChatFormatting.GRAY);
                } else {
                    int dummyAmmo = iGun.getDummyAmmoAmount(stack);
                    ammoCountText = Component.literal("%d/%d (%d)".formatted(currentAmmo, maxAmmo, dummyAmmo))
                        .withStyle(ChatFormatting.GRAY);
                }
                lines.add(ammoCountText);
            }
        }

        // ── 基础信息 ──
        if (shouldShow(GunTooltipPart.BASE_INFO, hideFlags)) {
            // 等级
            int level = iGun.getLevel(stack);
            int maxLevel = iGun.getMaxLevel();
            if (level >= 0) {
                MutableComponent levelLine;
                if (level >= maxLevel) {
                    levelLine = Component.translatable("tooltip.tacz.gun.level")
                        .append(Component.literal("%d (MAX)".formatted(level))
                            .withStyle(ChatFormatting.DARK_PURPLE));
                } else {
                    int expCurrent = iGun.getExpCurrentLevel(stack);
                    int expToNext = iGun.getExpToNextLevel(stack);
                    String levelText = "%d (%.1f%%)".formatted(level,
                        expCurrent / (expToNext + expCurrent + 0.0) * 100f);
                    levelLine = Component.translatable("tooltip.tacz.gun.level")
                        .append(Component.literal(levelText).withStyle(ChatFormatting.YELLOW));
                }
                lines.add(levelLine);
            }

            // 枪械类型
            String typeKey = "tacz.type." + gunIndex.getType() + ".name";
            lines.add(Component.translatable("tooltip.tacz.gun.type")
                .append(Component.translatable(typeKey).withStyle(ChatFormatting.AQUA)));

            // 伤害
            double damage = AttachmentDataUtils.getDamageWithAttachment(stack, gunData);
            boolean hasSlugInstalled = AllowAttachmentTagMatcher.matchTag(
                ModernKineticGunItem.DefaultPropertyModification.SLUGS,
                iGun.getAttachmentId(stack, AttachmentType.EXTENDED_MAG));
            int bulletAmount = hasSlugInstalled ? 1 : bulletData.getBulletAmount();
            MutableComponent damageValue;
            if (bulletAmount > 1) {
                damageValue = Component.literal(
                    DAMAGE_FORMAT.format(damage / bulletAmount) + "x" + bulletAmount)
                    .withStyle(ChatFormatting.AQUA);
            } else {
                damageValue = Component.literal(DAMAGE_FORMAT.format(damage))
                    .withStyle(ChatFormatting.AQUA);
            }
            // 爆炸伤害
            if (bulletData.getExplosionData() != null
                && (AttachmentDataUtils.isExplodeEnabled(stack, gunData)
                    || bulletData.getExplosionData().isExplode())) {
                double explosionDmg = bulletData.getExplosionData().getDamage()
                    * SyncConfig.DAMAGE_BASE_MULTIPLIER.get();
                damageValue.append(" + ").append(DAMAGE_FORMAT.format(explosionDmg))
                    .append(Component.translatable("tooltip.tacz.gun.explosion"));
            }
            lines.add(Component.translatable("tooltip.tacz.gun.damage").append(damageValue));
        }

        // ── 额外伤害信息 ──
        if (shouldShow(GunTooltipPart.EXTRA_DAMAGE_INFO, hideFlags)) {
            ExtraDamage extraDamage = bulletData.getExtraDamage();
            double armorIgnore;
            double headshotMultiplier;
            if (extraDamage != null) {
                armorIgnore = AttachmentDataUtils.getArmorIgnoreWithAttachment(stack, gunData);
                headshotMultiplier = AttachmentDataUtils.getHeadshotMultiplier(stack, gunData);
            } else {
                armorIgnore = 0;
                headshotMultiplier = 1;
            }
            armorIgnore = Mth.clamp(armorIgnore, 0.0F, 1.0F);

            lines.add(Component.translatable("tooltip.tacz.gun.armor_ignore",
                FORMAT.format(armorIgnore)).withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("tooltip.tacz.gun.head_shot_multiplier",
                FORMAT.format(headshotMultiplier)).withStyle(ChatFormatting.GOLD));

            // 移速惩罚
            double weightFactor = SyncConfig.WEIGHT_SPEED_MULTIPLIER.get();
            double weight = AttachmentDataUtils.getWightWithAttachment(stack, gunData);
            lines.add(Component.translatable("tooltip.tacz.gun.movement_speed",
                FORMAT_P_D1.format(-weightFactor * weight)).withStyle(ChatFormatting.RED));
        }

        return lines;
    }

    private static boolean shouldShow(GunTooltipPart part, int hideFlags) {
        return (hideFlags & part.getMask()) == 0;
    }

    /**
     * 采集 TACZ 枪属性的结构化数据（供后续网络包发送用）。
     *
     * @param stack 物品栈
     * @return 结构化数据的 JSON 字符串，如果不是 TACZ 枪则返回 null
     */
    public static String collectStructuredData(ItemStack stack) {
        IGun iGun = IGun.getIGunOrNull(stack);
        if (iGun == null) return null;

        ResourceLocation gunId = iGun.getGunId(stack);
        Optional<CommonGunIndex> optIndex = TimelessAPI.getCommonGunIndex(gunId);
        if (optIndex.isEmpty()) return null;

        CommonGunIndex gunIndex = optIndex.get();
        GunData gunData = gunIndex.getGunData();
        BulletData bulletData = gunIndex.getBulletData();

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"gunId\":\"").append(gunId).append("\",");

        double damage = AttachmentDataUtils.getDamageWithAttachment(stack, gunData);
        sb.append("\"damage\":").append(damage).append(",");

        int bulletAmount = bulletData.getBulletAmount();
        sb.append("\"bulletAmount\":").append(bulletAmount).append(",");

        ExtraDamage extraDamage = bulletData.getExtraDamage();
        double armorIgnore = extraDamage != null
            ? AttachmentDataUtils.getArmorIgnoreWithAttachment(stack, gunData) : 0;
        armorIgnore = Mth.clamp(armorIgnore, 0.0F, 1.0F);
        sb.append("\"armorIgnore\":").append(armorIgnore).append(",");

        double headshot = extraDamage != null
            ? AttachmentDataUtils.getHeadshotMultiplier(stack, gunData) : 1;
        sb.append("\"headshotMultiplier\":").append(headshot).append(",");

        double weight = AttachmentDataUtils.getWightWithAttachment(stack, gunData);
        sb.append("\"weight\":").append(weight).append(",");

        double weightFactor = SyncConfig.WEIGHT_SPEED_MULTIPLIER.get();
        sb.append("\"movementSpeedPenalty\":").append(-weightFactor * weight).append(",");

        int barrelBullet = (iGun.hasBulletInBarrel(stack)
            && gunData.getBolt() != Bolt.OPEN_BOLT) ? 1 : 0;
        int maxAmmo = AttachmentDataUtils.getAmmoCountWithAttachment(stack, gunData) + barrelBullet;
        sb.append("\"maxAmmo\":").append(maxAmmo).append(",");

        int currentAmmo = iGun.getCurrentAmmoCount(stack) + barrelBullet;
        sb.append("\"currentAmmo\":").append(currentAmmo).append(",");

        int level = iGun.getLevel(stack);
        sb.append("\"level\":").append(level).append(",");

        sb.append("\"type\":\"").append(gunIndex.getType()).append("\"");

        sb.append("}");
        return sb.toString();
    }
}
