package myau.ui.components;

import myau.Myau;
import myau.module.Module;
import myau.module.modules.HUD;
import myau.property.Property;
import myau.property.properties.*;
import myau.ui.Component;
import myau.ui.dataset.impl.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ModuleComponent implements Component {

    private static final int MODULE_HEIGHT = 16;
    private static final int PADDING_Y = 4;

    public final Module module;
    public final CategoryComponent category;
    private int offsetY;

    private final List<Component> settingComponents = new ArrayList<>();
    private boolean expanded = false;

    public ModuleComponent(Module module, CategoryComponent category, int offsetY) {
        this.module = module;
        this.category = category;
        this.offsetY = offsetY;

        int settingY = offsetY + MODULE_HEIGHT;

        // Popola dinamicamente i componenti delle impostazioni
        for (Property<?> prop : Myau.propertyManager.getPropertiesFor(module.getClass())) {
            Component settingComponent = createSettingComponent(prop, settingY);
            if (settingComponent != null) {
                settingComponents.add(settingComponent);
                settingY += settingComponent.getHeight();
            }
        }

        // Aggiungi sempre il bind in fondo
        settingComponents.add(new BindComponent(this, settingY));
    }

    private Component createSettingComponent(Property<?> property, int y) {
        return switch (property) {
            case BooleanProperty boolProp -> new CheckBoxComponent(boolProp, this, y);
            case FloatProperty floatProp -> new SliderComponent(new FloatSlider(floatProp), this, y);
            case IntProperty intProp -> new SliderComponent(new IntSlider(intProp), this, y);
            case PercentProperty percentProp -> new SliderComponent(new PercentageSlider(percentProp), this, y);
            case ModeProperty modeProp -> new ModeComponent(modeProp, this, y);
            case ColorProperty colorProp -> new ColorSliderComponent(colorProp, this, y);
            case TextProperty textProp -> new TextComponent(textProp, this, y);
            default -> null;
        };
    }

    @Override
    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
        int currentY = newOffsetY + MODULE_HEIGHT;

        for (Component setting : settingComponents) {
            setting.setComponentStartAt(currentY);
            if (setting.isVisible()) {
                currentY += setting.getHeight();
            }
        }
    }

    @Override
    public void draw(AtomicInteger offset) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        int x = category.getX();
        int y = category.getY() + offsetY;
        int width = category.getWidth();

        // Colore del testo in base allo stato del modulo
        int textColor = module.isEnabled()
                ? ((HUD) Myau.moduleManager.getModule(HUD.class)).getColor(System.currentTimeMillis(), offset.get()).getRGB()
                : new Color(102, 102, 102).getRGB();

        // Centra il nome del modulo
        String name = module.getName();
        int textX = x + width / 2 - fr.getStringWidth(name) / 2;
        int textY = y + PADDING_Y;

        fr.drawStringWithShadow(name, textX, textY, textColor);

        // Disegna le impostazioni se espanso
        if (expanded && !settingComponents.isEmpty()) {
            for (Component setting : settingComponents) {
                if (setting.isVisible()) {
                    setting.draw(offset);
                    offset.incrementAndGet();
                }
            }
        }
    }

    @Override
    public int getHeight() {
        if (!expanded) {
            return MODULE_HEIGHT;
        }

        return MODULE_HEIGHT + settingComponents.stream()
                .filter(Component::isVisible)
                .mapToInt(Component::getHeight)
                .sum();
    }

    @Override
    public void update(int mouseX, int mouseY) {
        if (!expanded) return;

        settingComponents.stream()
                .filter(Component::isVisible)
                .forEach(c -> c.update(mouseX, mouseY));
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        if (isHovered(mouseX, mouseY)) {
            if (button == 0) { // Click sinistro
                module.toggle();
            } else if (button == 1) { // Click destro
                expanded = !expanded;
            }
        }

        if (!expanded) return;

        settingComponents.stream()
                .filter(Component::isVisible)
                .forEach(c -> c.mouseDown(mouseX, mouseY, button));
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        if (!expanded) return;

        settingComponents.stream()
                .filter(Component::isVisible)
                .forEach(c -> c.mouseReleased(mouseX, mouseY, button));
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (!expanded) return;

        settingComponents.stream()
                .filter(Component::isVisible)
                .forEach(c -> c.keyTyped(typedChar, keyCode));
    }

    @Override
    public boolean isHovered(int mouseX, int mouseY) {
        int x = category.getX();
        int y = category.getY() + offsetY;
        int width = category.getWidth();

        return mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + MODULE_HEIGHT;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    // Getter utile per animazioni o hover effects futuri
    public boolean isExpanded() {
        return expanded;
    }

    public int getOffsetY() {
        return offsetY;
    }
}
