package myau.ui.components;

import myau.Myau;
import myau.module.modules.HUD;
import myau.ui.Component;
import myau.ui.dataset.Slider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;

public class SliderComponent implements Component {

    private static final int HEIGHT = 16;
    private static final int PADDING_X = 4;
    private static final int BAR_HEIGHT = 4;
    private static final int BAR_Y_OFFSET = 11;
    private static final int BACKGROUND_COLOR = new Color(30, 30, 30, 180).getRGB(); // -12302777 ≈ questo
    private static final double TEXT_SCALE = 0.5;

    private final Slider slider;
    private final ModuleComponent parent;
    private int offsetY;

    private boolean dragging = false;
    private double renderWidth = 0; // Larghezza renderizzata della barra (per animazioni future se vuoi)

    public SliderComponent(Slider slider, ModuleComponent parent, int offsetY) {
        this.slider = slider;
        this.parent = parent;
        this.offsetY = offsetY;
    }

    @Override
    public void draw(AtomicInteger offset) {
        if (!isVisible()) return;

        int x = parent.category.getX() + PADDING_X;
        int y = parent.category.getY() + offsetY + BAR_Y_OFFSET;
        int fullWidth = parent.category.getWidth() - 2 * PADDING_X;

        // Sfondo barra
        Gui.drawRect(x, y, x + fullWidth, y + BAR_HEIGHT, BACKGROUND_COLOR);

        // Barra colorata (HUD color)
        int hudColor = ((HUD) Myau.moduleManager.getModule(HUD.class))
                .getColor(System.currentTimeMillis(), offset.get()).getRGB();

        double ratio = (slider.getInput() - slider.getMin()) / (slider.getMax() - slider.getMin());
        int filledWidth = (int) (fullWidth * ratio);

        Gui.drawRect(x, y, x + filledWidth, y + BAR_HEIGHT, hudColor);

        // Testo scalato (nome + valore)
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        String text = slider.getName() + ": " + slider.getValueString();

        fr.drawStringWithShadowScaled(
                text,
                (x) * (1 / TEXT_SCALE), // Posizione scalata inversa
                (parent.category.getY() + offsetY + 3) * (1 / TEXT_SCALE),
                -1,
                (float) TEXT_SCALE
        );
    }

    @Override
    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void update(int mouseX, int mouseY) {
        if (!parent.isExpanded() || !isVisible()) return;

        int barX = parent.category.getX() + PADDING_X;
        int barY = parent.category.getY() + offsetY + BAR_Y_OFFSET;
        int barWidth = parent.category.getWidth() - 2 * PADDING_X;

        // Calcola larghezza renderizzata (puoi animarla in futuro con lerp)
        double ratio = (slider.getInput() - slider.getMin()) / (slider.getMax() - slider.getMin());
        renderWidth = barWidth * ratio;

        if (dragging) {
            double mouseProgress = Math.clamp((mouseX - barX) / (double) barWidth, 0.0, 1.0);
            double newValue = slider.getMin() + mouseProgress * (slider.getMax() - slider.getMin());

            // Applica incremento se presente
            double increment = slider.getIncrement();
            if (increment > 0) {
                newValue = Math.round(newValue / increment) * increment;
            }

            // Arrotonda a 2 decimali per float puliti (opzionale, puoi rimuovere)
            newValue = roundToDecimalPlaces(newValue, 2);

            slider.setValue(Math.clamp(newValue, slider.getMin(), slider.getMax()));
        }
    }

    private static double roundToDecimalPlaces(double value, int places) {
        if (places < 0) return value;
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        if (!parent.isExpanded() || !isVisible() || button != 0) return;

        if (isHovered(mouseX, mouseY)) {
            dragging = true;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        dragging = false;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        // Puoi aggiungere supporto tastiera qui (es. frecce per cambiare valore)
    }

    private boolean isHovered(int mouseX, int mouseY) {
        int x = parent.category.getX();
        int y = parent.category.getY() + offsetY;
        int width = parent.category.getWidth();
        int height = HEIGHT;

        return mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public boolean isVisible() {
        return slider.isVisible();
    }

    // Helper per clamp (Java 8 non ha Math.clamp, lo aggiungiamo)
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

// Estensione utile per FontRenderer (aggiungila in una classe GuiUtils o simile)
extension FontRenderer {
    public void drawStringWithShadowScaled(String text, float x, float y, int color, float scale) {
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, scale);
        this.drawStringWithShadow(text, (int) (x / scale), (int) (y / scale), color);
        GL11.glPopMatrix();
    }
}
