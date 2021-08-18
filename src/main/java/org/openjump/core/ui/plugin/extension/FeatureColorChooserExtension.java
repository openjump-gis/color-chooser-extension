package org.openjump.core.ui.plugin.extension;

import org.openjump.core.ui.plugin.colorchooser.FeatureColorChooserPlugIn;

import com.vividsolutions.jump.workbench.plugin.Extension;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

public class FeatureColorChooserExtension extends Extension {

    private static final String NAME = "Feature Color Chooser PlugIn (Giuseppe Aruta - adapted from SkyJUMP sourceforge.net/projects/skyjump/)";
    private static final String VERSION = "2.1.0 (2021-08-18)";

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