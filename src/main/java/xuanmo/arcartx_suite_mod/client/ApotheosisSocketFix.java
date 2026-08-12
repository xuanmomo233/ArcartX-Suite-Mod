package xuanmo.arcartx_suite_mod.client;

import java.util.ArrayList;
import java.util.List;
import com.mojang.datafixers.util.Either;
import dev.shadowsoffire.apotheosis.adventure.client.SocketTooltipRenderer;
import dev.shadowsoffire.apotheosis.adventure.client.SocketTooltipRenderer.SocketComponent;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.client.event.RenderTooltipEvent;

/**
 * 将 Apotheosis 的 {@link SocketComponent}（图像类型 TooltipComponent）
 * 转换为文本行，以便 ArcartX 的 {@code onTooltipRender} 能提取到宝石槽描述。
 * <p>
 * Apotheosis 在 {@code GatherComponents} 事件中将 {@code APOTH_REMOVE_MARKER}
 * 替换为 {@link SocketComponent}，而 ArcartX 只从 {@link net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip}
 * 中提取文本，导致宝石槽描述在 ArcartX UI 中不显示。
 * <p>
 * 此类单独存放，仅在 Apotheosis 存在时才会被类加载，调用方用 try-catch 包裹。
 */
public final class ApotheosisSocketFix {

    private ApotheosisSocketFix() {}

    /**
     * 遍历 GatherComponents 的 elements 列表，将所有 {@link SocketComponent}
     * 替换为对应的文本行（每个宝石一行描述）。
     *
     * @param event GatherComponents 事件
     */
    public static void convert(RenderTooltipEvent.GatherComponents event) {
        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        for (int i = elements.size() - 1; i >= 0; i--) {
            Either<FormattedText, TooltipComponent> either = elements.get(i);
            TooltipComponent tc = either.right().orElse(null);
            if (tc instanceof SocketComponent sc) {
                List<Component> textLines = new ArrayList<>();
                for (GemInstance inst : sc.gems().gems()) {
                    textLines.add(SocketTooltipRenderer.getSocketDesc(inst));
                }
                // 用文本行替换 SocketComponent
                elements.remove(i);
                for (int j = textLines.size() - 1; j >= 0; j--) {
                    elements.add(i, Either.left(textLines.get(j)));
                }
            }
        }
    }
}
