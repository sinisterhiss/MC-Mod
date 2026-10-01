package dev.unifiedstorage.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class UniversalStorageScreen extends HandledScreen<UniversalStorageScreenHandler> {
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/generic_54.png");

    private ButtonWidget previous;
    private ButtonWidget next;

    public UniversalStorageScreen(UniversalStorageScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 222;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        int bx = this.x + this.backgroundWidth + 5;
        int by = this.y + 18;

        previous = addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> click(UniversalStorageScreenHandler.BUTTON_PREVIOUS))
                .dimensions(bx, by, 24, 20).build());
        next = addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> click(UniversalStorageScreenHandler.BUTTON_NEXT))
                .dimensions(bx + 28, by, 24, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Sort"), button -> click(UniversalStorageScreenHandler.BUTTON_SORT))
                .dimensions(bx, by + 28, 72, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Deposit All"), button -> click(UniversalStorageScreenHandler.BUTTON_DEPOSIT_ALL))
                .dimensions(bx, by + 52, 72, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Loot All"), button -> click(UniversalStorageScreenHandler.BUTTON_LOOT_ALL))
                .dimensions(bx, by + 76, 72, 20).build());
    }

    private void click(int id) {
        if (client != null && client.interactionManager != null) {
            client.interactionManager.clickButton(handler.syncId, id);
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);
        context.drawText(textRenderer,
                Text.literal("Page " + (handler.getPage() + 1) + "/" + handler.getPageCount()),
                backgroundWidth + 6, 6, 0x404040, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (previous != null) previous.active = handler.getPage() > 0;
        if (next != null) next.active = handler.getPage() + 1 < handler.getPageCount();
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
