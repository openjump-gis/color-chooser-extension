package org.openjump.core.ui.plugin.extension;

import org.openjump.core.ui.plugin.colorchooser.FeatureColorChooserPlugIn;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.plugin.Extension;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

public class FeatureColorChooserExtension extends Extension {

    private static final String NAME = "Feature Color Chooser PlugIn (Giuseppe Aruta - adapted from SkyJUMP sourceforge.net/projects/skyjump/)";
    private static final String VERSION = I18N.getInstance("color_chooser").get("color_chooser.version");

    public String getName() {
        return NAME;
    }

    public String getVersion() {
        return VERSION;
    }

    public void configure(PlugInContext context) {

        new FeatureColorChooserPlugIn().initialize(context);

    }
}