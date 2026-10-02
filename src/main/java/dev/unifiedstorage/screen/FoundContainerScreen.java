package dev.unifiedstorage.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class FoundContainerScreen extends AbstractContainerScreen<FoundContainerScreenHandler> {
    private static final Identifier TEXTURE =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int PANEL_WIDTH = 176;

    private Button previous;
    private Button next;

    public FoundContainerScreen(
            FoundContainerScreenHandler menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title, PANEL_WIDTH, 114 + menu.getRows() * 18);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        int x = this.leftPos + PANEL_WIDTH + 6;
        int y1 = this.topPos + 134;
        int y2 = this.topPos + 150;

        previous = addRenderableWidget(Button.builder(
                        Component.literal("<"),
                        button -> press(FoundContainerScreenHandler.BUTTON_PREVIOUS))
                .bounds(x, y1, 18, 14)
                .build());

        next = addRenderableWidget(Button.builder(
                        Component.literal(">"),
                        button -> press(FoundContainerScreenHandler.BUTTON_NEXT))
                .bounds(x + 20, y1, 18, 14)
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("Sort"),
                        button -> press(FoundContainerScreenHandler.BUTTON_SORT))
                .bounds(x + 40, y1, 34, 14)
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("Deposit"),
                        button -> press(FoundContainerScreenHandler.BUTTON_DEPOSIT_ALL))
                .bounds(x + 76, y1, 48, 14)
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("Loot"),
                        button -> press(FoundContainerScreenHandler.BUTTON_LOOT_ALL))
                .bounds(x + 126, y1, 42, 14)
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("Store Chest"),
                        button -> press(FoundContainerScreenHandler.BUTTON_STORE_CHEST))
                .bounds(x, y2, 168, 14)
                .build());

        refreshButtons();
    }

    private void press(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshButtons();
    }

    private void refreshButtons() {
        if (previous != null) previous.active = menu.getPage() > 0;
        if (next != null) next.active = menu.getPage() + 1 < menu.getPageCount();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);

        int foundRows = menu.getRows();

        // Left: vanilla found chest/barrel and player inventory.
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                this.leftPos,
                this.topPos,
                0.0F,
                0.0F,
                PANEL_WIDTH,
                foundRows * 18 + 17,
                256,
                256
        );
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                this.leftPos,
                this.topPos + foundRows * 18 + 17,
                0.0F,
                126.0F,
                PANEL_WIDTH,
                96,
                256,
                256
        );

        // Right: Universal Storage.
        int storageX = this.leftPos + PANEL_WIDTH;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                storageX,
                this.topPos,
                0.0F,
                0.0F,
                PANEL_WIDTH,
                132,
                256,
                256
        );
        graphics.fill(
                storageX,
                this.topPos + 132,
                storageX + PANEL_WIDTH,
                this.topPos + Math.max(this.imageHeight, 166),
                0xFFC6C6C6
        );
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);

        graphics.text(
                this.font,
                Component.literal("Universal Storage"),
                PANEL_WIDTH + 8,
                6,
                0x404040,
                false
        );
        graphics.text(
                this.font,
                Component.literal("Page " + (menu.getPage() + 1) + "/" + menu.getPageCount()),
                PANEL_WIDTH + 130,
                6,
                0x404040,
                false
        );
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        boolean insideStorage =
                mouseX >= this.leftPos + PANEL_WIDTH
                        && mouseX < this.leftPos + PANEL_WIDTH * 2
                        && mouseY >= this.topPos
                        && mouseY < this.topPos + Math.max(this.imageHeight, 166);

        if (insideStorage) return false;
        return super.hasClickedOutside(mouseX, mouseY, left, top);
    }
}
