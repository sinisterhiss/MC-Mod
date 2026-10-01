package dev.unifiedstorage.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class UniversalStorageScreen extends AbstractContainerScreen<UniversalStorageScreenHandler> {
    private static final Identifier STORAGE_TEXTURE =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier CRAFTING_TEXTURE =
            Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");

    private static final int PANEL_WIDTH = 176;
    private static final int SCREEN_WIDTH = 352;
    private static final int SCREEN_HEIGHT = 222;

    private Button previous;
    private Button next;

    public UniversalStorageScreen(
            UniversalStorageScreenHandler menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title, SCREEN_WIDTH, SCREEN_HEIGHT);
        this.inventoryLabelY = 129;
    }

    @Override
    protected void init() {
        super.init();

        int right = this.leftPos + PANEL_WIDTH;
        int bx = right + 8;
        int by = this.topPos + 92;

        previous = Button.builder(Component.literal("<"),
                        button -> press(UniversalStorageScreenHandler.BUTTON_PREVIOUS))
                .bounds(bx, by, 32, 20)
                .build();

        next = Button.builder(Component.literal(">"),
                        button -> press(UniversalStorageScreenHandler.BUTTON_NEXT))
                .bounds(bx + 36, by, 32, 20)
                .build();

        addRenderableWidget(previous);
        addRenderableWidget(next);

        addRenderableWidget(Button.builder(Component.literal("Sort"),
                        button -> press(UniversalStorageScreenHandler.BUTTON_SORT))
                .bounds(bx, by + 28, 92, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Deposit All"),
                        button -> press(UniversalStorageScreenHandler.BUTTON_DEPOSIT_ALL))
                .bounds(bx, by + 52, 92, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Loot All"),
                        button -> press(UniversalStorageScreenHandler.BUTTON_LOOT_ALL))
                .bounds(bx, by + 76, 92, 20)
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

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                STORAGE_TEXTURE,
                this.leftPos,
                this.topPos,
                0.0F,
                0.0F,
                PANEL_WIDTH,
                SCREEN_HEIGHT,
                256,
                256
        );

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CRAFTING_TEXTURE,
                this.leftPos + PANEL_WIDTH,
                this.topPos,
                0.0F,
                0.0F,
                PANEL_WIDTH,
                83,
                256,
                256
        );
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, Component.literal("Universal Storage"), 8, 6, 0x404040, false);
        graphics.text(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
        graphics.text(this.font, Component.literal("Crafting"), PANEL_WIDTH + 29, 6, 0x404040, false);
        graphics.text(
                this.font,
                Component.literal("Page " + (menu.getPage() + 1) + "/" + menu.getPageCount()),
                118,
                6,
                0x404040,
                false
        );
    }
}
