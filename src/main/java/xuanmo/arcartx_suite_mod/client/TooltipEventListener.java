package xuanmo.arcartx_suite_mod.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xuanmo.arcartx_suite_mod.ArcartXSuiteMod;

/**
 * ItemTooltipEvent 监听器，将 TACZ 枪属性和 Apotheosis affix 的文本行
 * 注入到 {@code event.getToolTip()} 中。
 * <p>
 * 使用 LOW 优先级确保在 Apotheosis 的 HIGH 优先级监听器之后执行，
 * 这样 Apotheosis 已经插入了 affix 描述行，我们只需补充 TACZ 枪属性行。
 * <p>
 * 注入的文本行会被 ArcartX 的 {@code onTooltipRender} 从
 * {@code ClientTextTooltip} 组件中提取，显示在 ArcartX 自定义 UI 中。
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = ArcartXSuiteMod.MOD_ID)
public class TooltipEventListener {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        List<Component> tooltip = event.getToolTip();

        // 1. TACZ 枪属性注入
        // TACZ 的枪属性走 TooltipComponent 机制，不在 getToolTip() 中
        // 需要调用 TACZ API 复刻计算，将文本行添加到 tooltip 列表
        List<Component> taczLines = TaczTooltipCollector.collectTooltipLines(stack);
        if (!taczLines.isEmpty()) {
            // 找到合适的插入位置：在物品名之后、原版属性之前
            // ItemTooltipEvent 的 tooltip 列表第 0 个是物品名
            // 我们在第 1 个位置插入（和 Apotheosis 一样的策略）
            int insertIndex = Math.min(1, tooltip.size());
            for (int i = taczLines.size() - 1; i >= 0; i--) {
                tooltip.add(insertIndex, taczLines.get(i));
            }
        }

        // 2. Apotheosis affix 行已在 HIGH 优先级被插入，无需额外处理
        // 但需要确认它们确实在 tooltip 列表中（Apotheosis 的 affixTooltips 方法已处理）
    }
}
