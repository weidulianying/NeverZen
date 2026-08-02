package shit.zen.gui.neverloseGUI.factory;

import java.util.Arrays;
import shit.zen.gui.neverloseGUI.framework.Component;
import shit.zen.gui.neverloseGUI.model.SettingViewModel;
import shit.zen.gui.neverloseGUI.widget.*;

public final class WidgetFactory {
    private WidgetFactory() {}

    public static Component create(SettingViewModel svm, float x, float y, float w) {
        if (svm.isBoolean()) return new Toggle(x + w - 34, y + 2, svm.getBoolean(), svm::setBoolean);
        if (svm.isNumber())  return new LineSlider(x + w * 0.54f, y, w * 0.43f, "", svm.getMin(), svm.getMax(), svm.getStep(), svm.getNumber(), svm::setNumber);
        if (svm.isMode())    return new TextModeSelector(x + w * 0.50f, y, w * 0.47f, svm.getModes(), java.util.Arrays.asList(svm.getModes()).indexOf(svm.getMode()), idx -> svm.setMode(svm.getModes()[idx]));
        if (svm.isMulti())   return new MultiSelectBox(x + w * 0.50f, y, w * 0.47f, svm.name(), svm.getMultiOptions(), svm::getMultiSelected, svm::toggleMulti);
        if (svm.isString())  return new TextInput(x + w * 0.50f, y + 2, w * 0.47f, svm::getText, svm::setText, false);
        if (svm.isPassword()) return new TextInput(x + w * 0.50f, y + 2, w * 0.47f,
                () -> "•".repeat(Math.min(svm.passwordLength(), 24)), svm::setPassword, true);
        if (svm.isAction())  return new Button(x + w * 0.55f, y + 2, w * 0.42f, 18, svm.name(), svm::invoke);
        return null;
    }
}
