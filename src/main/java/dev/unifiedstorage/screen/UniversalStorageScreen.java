package dev.unifiedstorage.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class UniversalStorageScreen extends AbstractRecipeBookScreen<UniversalStorageScreenHandler> {
    private static final Identifier CRAFTING_TEXTURE =
            Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");
    private static final Identifier STORAGE_TEXTURE =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int CRAFTING_WIDTH = 176;
    private static final int STORAGE_WIDTH = 176;
    private static final int STORAGE_BACKGROUND_HEIGHT = 132;

    private Button previous;
    private Button next;
    private Button sort;
    private Button deposit;
    private Button loot;

    public UniversalStorageScreen(
            UniversalStorageScreenHandler menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, new CraftingRecipeBookComponent(menu), inventory, title);
    }

    @Override
    protected void init() {
        super.init();

        previous = addRenderableWidget(Button.builder(
                        Component.literal("<"),
                        button -> press(UniversalStorageScreenHandler.BUTTON_PREVIOUS))
                .bounds(0, 0, 18, 20)
                .build());

        next = addRenderableWidget(Button.builder(
                        Component.literal(">"),
                        button -> press(UniversalStorageScreenHandler.BUTTON_NEXT))
                .bounds(0, 0, 18, 20)
                .build());

        sort = addRenderableWidget(Button.builder(
                        Component.literal("Sort"),
                        button -> press(UniversalStorageScreenHandler.BUTTON_SORT))
                .bounds(0, 0, 34, 20)
                .build());

        deposit = addRenderableWidget(Button.builder(
                        Component.literal("Deposit"),
                        button -> press(UniversalStorageScreenHandler.BUTTON_DEPOSIT_ALL))
                .bounds(0, 0, 48, 20)
                .build());

        loot = addRenderableWidget(Button.builder(
                        Component.literal("Loot"),
                        button -> press(UniversalStorageScreenHandler.BUTTON_LOOT_ALL))
                .bounds(0, 0, 42, 20)
                .build());

        positionStorageButtons();
        refreshStorageButtons();
    }

    @Override
    protected ScreenPosition getRecipeBookButtonPosition() {
        // Same location as the vanilla crafting-table screen.
        return new ScreenPosition(this.leftPos + 5, this.height / 2 - 49);
    }

    @Override
    protected void onRecipeBookButtonClick() {
        // Opening/closing the vanilla recipe book can move the centered crafting panel.
        // Keep the Universal Storage controls attached to its right edge.
        positionStorageButtons();
    }

    private void positionStorageButtons() {
        if (previous == null) return;

        int x = this.leftPos + CRAFTING_WIDTH + 6;
        int y = this.topPos + 138;

        previous.setPosition(x, y);
        next.setPosition(x + 20, y);
        sort.setPosition(x + 42, y);
        deposit.setPosition(x + 78, y);
        loot.setPosition(x + 128, y);
    }

    private void press(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        refreshStorageButtons();
    }

    private void refreshStorageButtons() {
        if (previous != null) previous.active = menu.getPage() > 0;
        if (next != null) next.active = menu.getPage() + 1 < menu.getPageCount();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);

        // Vanilla crafting-table panel stays centered exactly as normal.
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CRAFTING_TEXTURE,
                this.leftPos,
                this.topPos,
                0.0F,
                0.0F,
                this.imageWidth,
                this.imageHeight,
                256,
                256
        );

        // Universal Storage is an attached panel to the right.
        int storageX = this.leftPos + CRAFTING_WIDTH;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                STORAGE_TEXTURE,
                storageX,
                this.topPos,
                0.0F,
                0.0F,
                STORAGE_WIDTH,
                STORAGE_BACKGROUND_HEIGHT,
                256,
                256
        );

        // Plain vanilla-style backing for the storage controls below the six rows.
        graphics.fill(
                storageX,
                this.topPos + STORAGE_BACKGROUND_HEIGHT,
                storageX + STORAGE_WIDTH,
                this.topPos + this.imageHeight,
                0xFFC6C6C6
        );
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Keeps the normal "Crafting" and "Inventory" labels on the center panel.
        super.extractLabels(graphics, mouseX, mouseY);

        graphics.text(
                this.font,
                Component.literal("Universal Storage"),
                CRAFTING_WIDTH + 8,
                6,
                0x404040,
                false
        );

        graphics.text(
                this.font,
                Component.literal("Page " + (menu.getPage() + 1) + "/" + menu.getPageCount()),
                CRAFTING_WIDTH + 130,
                6,
                0x404040,
                false
        );
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        boolean insideStorage =
                mouseX >= this.leftPos + CRAFTING_WIDTH
                        && mouseX < this.leftPos + CRAFTING_WIDTH + STORAGE_WIDTH
                        && mouseY >= this.topPos
                        && mouseY < this.topPos + this.imageHeight;

        if (insideStorage) {
            return false;
        }

        return super.hasClickedOutside(mouseX, mouseY, left, top);
    }
}
