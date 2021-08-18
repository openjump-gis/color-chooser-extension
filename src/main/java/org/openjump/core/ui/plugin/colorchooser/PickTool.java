package org.openjump.core.ui.plugin.colorchooser;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import images.ColorChooserIconLoader;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.geom.Point2D;
import java.util.Map;
import java.util.Set;

import javax.swing.*;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.openjump.core.ui.plugin.colorchooser.gui.ComboButton;
import org.openjump.core.ui.plugin.colorchooser.utils.ColorUtils;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.geom.EnvelopeUtil;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.cursortool.NClickTool;
import com.vividsolutions.jump.workbench.ui.cursortool.SpecifyFeaturesTool;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;

/*
 * [Giuseppe Aruta 2016_12_4] Pick tool allows to get RGB value (if exist) 
 * by clicking on a feature of a selected layer
 * This tool is distributed under GNU license
 */
public class PickTool extends NClickTool {

    I18N i18n = I18N.getInstance("color_chooser");

    private final ComboButton colorMenu;

    public PickTool(ComboButton colorMenu) {
        super(JUMPWorkbench.getInstance().getContext(), 1);
        this.colorMenu = colorMenu;
    }

    public static final String R_G_B = BasicStyle.RGB_ATTRIBUTE_NAME;

    protected Point getPoint() {
        return new GeometryFactory().createPoint((Coordinate) getCoordinates().get(0));
    }

    @Override
    protected void gestureFinished() {
        reportNothingToUndoYet();
        try {
            final Coordinate coord = (Coordinate) getCoordinates().get(0);
            final Point2D point = getPanel().getViewport().toViewPoint(coord);
            final int PIXEL_BUFFER = 2;
            // if (schema.hasAttribute(R_G_B)) {
            final Map<Layer, Set<Feature>> map =
                SpecifyFeaturesTool.layerToSpecifiedFeaturesMap(
                    panel.getLayerManager().getLayers().iterator(), EnvelopeUtil.expand(
                            new Envelope(panel.getViewport().toModelCoordinate(
                                    point)), PIXEL_BUFFER
                                    / panel.getViewport().getScale()));

            final String hex = findValue(R_G_B, map);
            final Color color = ColorUtils.hexToColorRGB(hex);
            if (colorMenu != null) colorMenu.setColor(color);
            //FeatureColorChooserPlugIn.colorSetButton.setColor(color);

            final String acad = ColorUtils.getColorIndexRegistry(hex);
            getWorkbench()
                    .getContext()
                    .getWorkbench()
                    .getFrame()
                    .setStatusMessage(
                            i18n.get("color") + " - " + "Index color: "
                                    + acad + "   Hex: " + hex + "   RGB: "
                                    + color.getRed() + "," + color.getGreen()
                                    + "," + color.getBlue(), 5000);

        } catch (final Exception e) {
            getWorkbench().getContext().getWorkbench().getFrame()
                    .setStatusMessage(i18n.get("msg1"), 5000);
        }
    }

    @Override
    public String getName() {
        return i18n.get("picker-color");
    }

    @Override
    public Icon getIcon() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Cursor getCursor() {
        return createCursor(ColorChooserIconLoader.image("pipette-cursor.gif")); //$NON-NLS-1$
    }

    private String findValue(String attributeName,
            Map<Layer, Set<Feature>> layerToSpecifiedFeaturesMap) {

        for (final Layer layer : layerToSpecifiedFeaturesMap.keySet()) {
            for (int j = 0; j < layer.getFeatureCollectionWrapper()
                    .getFeatureSchema().getAttributeCount(); j++) {
                if ("fid".equalsIgnoreCase(attributeName)) {
                    return "" + layerToSpecifiedFeaturesMap
                                    .get(layer).iterator().next().getID();
                }
                if (layer.getFeatureCollectionWrapper().getFeatureSchema()
                        .getAttributeName(j).equalsIgnoreCase(attributeName)) {
                    return "" + layerToSpecifiedFeaturesMap
                                    .get(layer).iterator().next().getAttribute(j);
                }
            }
        }
        return "";
    }

}
