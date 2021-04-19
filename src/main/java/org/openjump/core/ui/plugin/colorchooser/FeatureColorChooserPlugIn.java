package org.openjump.core.ui.plugin.colorchooser;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.vividsolutions.jump.workbench.Logger;
import language.I18NPlug;
import org.openjump.core.ui.plugin.colorchooser.gui.ColorMenu;
import org.openjump.core.ui.plugin.colorchooser.gui.ComboButton;
import org.openjump.core.ui.plugin.colorchooser.utils.ColorUtils;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.FeatureEventType;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;

import images.ColorChooserIconLoader;

public class FeatureColorChooserPlugIn extends AbstractPlugIn {

    public static final String COLOR = "COLOR";
    public static final String R_G_B = BasicStyle.RGB_ATTRIBUTE_NAME;
    private static final int buttonWidth = 25;

    public ComboButton colorSetButton;
    public ComboButton colorChooserButton;
    private JPopupMenu colorPickerPopup = null;

    private PlugInContext context;

    private int customIndex = 1;

    @Override
    public void initialize(final PlugInContext context) {

      this.context = context;

      colorSetButton = new ComboButton(ComboButton.RECTANGLE) {
        private static final long serialVersionUID = 1L;
  
        @Override
        public void setBounds(int x, int y, int width, int height) {
          super.setBounds(x, y, buttonWidth, height);
        }
      };

      colorChooserButton = new ComboButton(ComboButton.TRIANGLE) {
        private static final long serialVersionUID = 1L;
  
        @Override
        public void setBounds(int x, int y, int width, int height) {
          super.setBounds(colorSetButton.getX() + buttonWidth, y, buttonWidth, height);
        }
      };

      colorSetButton.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          setFeatureColor(colorSetButton.getColor());
        }
      });
  
      // init popup takes a long time, defer it after workbench is shown
      SwingUtilities.invokeLater(() -> colorPickerPopup = initPopupLazily());

      // Add the popup containing various color selectors to the colorChooserButton
      colorChooserButton.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          final int x = colorSetButton.getLocation().x;
          final int y = colorSetButton.getLocation().y + colorSetButton.getHeight();
          if (colorPickerPopup == null) {
            colorPickerPopup = initPopupLazily();
          }
          colorPickerPopup.show(colorSetButton.getParent(), x, y);
        }
      });

      colorSetButton.setToolTipText(I18NPlug.getI18N("set-color-tool"));
      colorChooserButton.setToolTipText(I18NPlug.getI18N("pick-color-tools"));
  
      context.getWorkbenchContext().getWorkbench().getFrame().getToolBar().addSeparator();
      context.getWorkbenchContext().getWorkbench().getFrame().getToolBar().add(colorSetButton);
      context.getWorkbenchContext().getWorkbench().getFrame().getToolBar().add(colorChooserButton);
      context.getWorkbenchContext().getWorkbench().getFrame().getToolBar().addSeparator();
    }

    // Initialize the popu associated to colorChooserButton and containing
    // - useLayerStyleMenuItem
    // - colorMenu
    // - otherColorMenuItem
    // - pickerColorMenuItem
    // - recent
    private JPopupMenu initPopupLazily() {

      final JPopupMenu popup = new JPopupMenu();
      popup.setLayout(new GridLayout(0, 1));

      // Defining menuItems and submenus
      JMenuItem useLayerStyleMenuItem =
          new JMenuItem(I18NPlug.getI18N("use-layer-style-color"),
          new ColorIcon(null));

      useLayerStyleMenuItem.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            Layer[] selectedLayers = context.getWorkbenchContext().getLayerableNamePanel().getSelectedLayers();
            if (selectedLayers == null || selectedLayers.length == 0) {
              Logger.warn("No layer selected");
              return;
            }
            Color color = selectedLayers[0].getBasicStyle().getFillColor();
            colorSetButton.setColor(color);
            final String hex = ColorUtils.colorRGBToHex(color);
            final String acad = ColorUtils.getColorIndexRegistry(hex);
            colorSetButton.setToolTipText("Index color: " + acad + "  Hex:"
                + hex + "   RGB: " + color.getRed() + ","
                + color.getGreen() + "," + color.getBlue());
            setFeatureColor(color);
        }
      });

      final JMenu recent = new JMenu(I18NPlug.getI18N("recent-color") + "...");

      final ColorMenu colorMenu = new ColorMenu(
          I18NPlug.getI18N("choose-color"), getColorIcon());

      colorMenu.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
              final Color color = colorMenu.getColor();
              if (color != null) {
                  colorSetButton.setColor(color);
                  setFeatureColor(color);
                  colorMenu.addActionListener(new ColorPickerActionListener(color));

                  colorSetButton.setColor(color);
                  setFeatureColor(color);
                  final String hex = ColorUtils.colorRGBToHex(color);
                  final String acad = ColorUtils.getColorIndexRegistry(hex);
                  final String msg = "Index color: " + acad;

                  final String text = "Hex: " + hex + "   RGB: "
                          + color.getRed() + "," + color.getGreen() + ","
                          + color.getBlue();
                  final JMenuItem mis = new JMenuItem(text,
                      new ColorIcon(color));
                  mis.setToolTipText(msg);
                  mis.addActionListener(new FeatureColorChooserPlugIn.ColorPickerActionListener(
                          color));
                  recent.add(mis);
                  colorPickerPopup.insert(recent, customIndex++);
                  popup.revalidate();
                  popup.repaint();
              }
          }
      });


      JMenuItem otherColorMenuItem = new JMenuItem(
          I18NPlug.getI18N("other-color"), getColorIcon_2());
      otherColorMenuItem.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent paramAnonymousActionEvent) {
              new JColorChooser();
              final Color color = JColorChooser.showDialog(context
                      .getWorkbenchContext().getWorkbench().getFrame(),
                      I18NPlug.getI18N("choose-color"), new Color(0, 0, 0));
              if (color != null) {
                  colorSetButton.setColor(color);
                  setFeatureColor(color);
                  colorChooserButton.setColor(color);
                  setFeatureColor(color);
                  final String hex = ColorUtils.colorRGBToHex(color);
                  final String acad = ColorUtils.getColorIndexRegistry(hex);

                  final String msg = "Index color: " + acad;

                  final String text = "Hex: " + hex + "   RGB: "
                          + color.getRed() + "," + color.getGreen() + ","
                          + color.getBlue();
                  final JMenuItem mis = new JMenuItem(text, new ColorIcon(color));
                  mis.setToolTipText(msg);
                  mis.addActionListener(new ColorPickerActionListener(color));
                  recent.add(mis);
                  colorPickerPopup.insert(recent, customIndex++);
                  popup.revalidate();
                  popup.repaint();
              }
          }
      });

      // popup.addSeparator();
      JMenuItem pickerColorMenuItem = new JMenuItem(I18NPlug.getI18N("picker-color"), getPickColorIcon());
      pickerColorMenuItem.setToolTipText(I18NPlug.getI18N("msg2"));
      final PickTool pickTool = new PickTool(colorSetButton);
      pickerColorMenuItem.addActionListener(e ->
          context.getWorkbenchContext().getLayerViewPanel().setCurrentCursorTool(pickTool));

      popup.add(useLayerStyleMenuItem);
      popup.add(colorMenu);
      popup.add(otherColorMenuItem);
      popup.add(pickerColorMenuItem);
      popup.add(recent);
      
      return popup;
    }

    public Icon getColorIcon() {
      final ImageIcon icon = ColorChooserIconLoader.icon("color-swatch.png");
      return GUIUtil.toSmallIcon(icon);
    }
  
    public Icon getColorIcon_2() {
      final ImageIcon icon = IconLoader.icon("color_wheel.png");
      return GUIUtil.toSmallIcon(icon);
    }
  
    public Icon getPickColorIcon() {
      final ImageIcon icon2 = ColorChooserIconLoader.icon("pipette.png");
      return GUIUtil.toSmallIcon(icon2);
    }

    private void setFeatureColor(Color color) {
        final LayerViewPanel layerViewPanel = context.getWorkbenchContext()
                .getLayerViewPanel();
        if (layerViewPanel == null) {
            return;
        }
        final Collection<Layer> layers = layerViewPanel.getSelectionManager()
                .getLayersWithSelectedItems();

        for (final Layer layer : layers) {
            if (layer.isEditable()) {
                continue;
            }
            layerViewPanel.getContext().warnUser(
                    I18NPlug.getI18N("selected-items-layers-must-be-editable")
                            + " (" + layer.getName() + ")");
            return;
        }
        String colorS = "";
        for (final Layer layer : layers) {
            layer.setFeatureCollectionModified(true);
            final FeatureCollectionWrapper fcw = layer
                    .getFeatureCollectionWrapper();
            final FeatureSchema schema = fcw.getFeatureSchema();

            if (!schema.hasAttribute(R_G_B)) {
                schema.addAttribute(R_G_B, AttributeType.STRING);

                for (final Iterator<Feature> j = fcw.iterator(); j.hasNext();) {
                    final Feature feature = j.next();
                    final Object[] attributes = new Object[schema
                            .getAttributeCount()];

                    for (int k = 0; k < attributes.length - 1; k++) {
                        attributes[k] = feature.getAttribute(k);
                    }
                    feature.setAttributes(attributes);
                }
            }
            if (!schema.hasAttribute(COLOR)) {
                schema.addAttribute(COLOR, AttributeType.STRING); // .INTEGER);
                for (final Iterator<Feature> j = fcw.iterator(); j.hasNext();) {
                    final Feature feature = j.next();
                    final Object[] attributes = new Object[schema
                            .getAttributeCount()];

                    for (int k = 0; k < attributes.length - 1; k++) {
                        attributes[k] = feature.getAttribute(k);
                    }
                    feature.setAttributes(attributes);
                }
            }
            colorS = ColorUtils.colorRGBToHex(color);
        }
        final Collection<Feature> features = layerViewPanel
                .getSelectionManager().getFeaturesWithSelectedItems();
        setRGB(layers, features, colorS);
    }


    protected void setRGB(final Collection<Layer> layers,
            final Collection<Feature> features, String RGB) {
        if (layers.isEmpty()) {
            return;
        }
        final String newRGB = RGB;
        final ArrayList<String> RGBs = new ArrayList<>();
        final ArrayList<String> Colors = new ArrayList<>();

        for (final Feature feature : features) {
            RGBs.add(feature.getString(R_G_B));
            Colors.add(feature.getString(COLOR));
        }

        final LayerManager layerManager = context.getLayerManager();//layers.iterator().next().getLayerManager();
        layerManager.getUndoableEditReceiver().startReceiving();

        try {
            final UndoableCommand command = new UndoableCommand("Edit R_G_B") {
                @Override
                public void execute() {
                    for (final Feature feature : features) {
                        feature.setAttribute(R_G_B, newRGB);
                        feature.setAttribute(COLOR,
                                ColorUtils.getColorIndexRegistry(newRGB));

                    }

                    for (final Layer layer : layers) {
                        layer.fireAppearanceChanged();
                        layerManager.fireFeaturesChanged(features,
                                FeatureEventType.ATTRIBUTES_MODIFIED, layer);
                    }

                }

                @Override
                public void unexecute() {
                    int i = 0;
                    for (final Feature feature : features) {
                        final Object ob = RGBs.get(i++);
                        feature.setAttribute(R_G_B, ob);
                        final String oldRGB = ob.toString();
                        feature.setAttribute(COLOR,

                        ColorUtils.getColorIndexRegistry(oldRGB));
                    }

                    for (final Layer layer : layers) {
                        layer.fireAppearanceChanged();
                        layerManager.fireFeaturesChanged(features,
                                FeatureEventType.ATTRIBUTES_MODIFIED, layer);
                    }
                }
            };
            command.execute();
            layerManager.getUndoableEditReceiver().receive(
                    command.toUndoableEdit());
        } finally {
            layerManager.getUndoableEditReceiver().stopReceiving();
        }
    }

    @Override
    public boolean execute(PlugInContext context) {
        return true;
    }

    public static EnableCheck createEnableCheck(
            WorkbenchContext workbenchContext, boolean b) {
        final EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);

        return new MultiEnableCheck().add(
                checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayersMustBeEditableCheck(1));
    }

    private static class ColorIcon implements Icon {
        private final Color color;

        public ColorIcon(Color color) {
            this.color = color;
        }

        @Override
        public int getIconHeight() {
            return 10;
        }

        @Override
        public int getIconWidth() {
            return 10;
        }

        @Override
        public void paintIcon(Component comp, Graphics g, int x, int y) {
            final Color oldColor = g.getColor();

            int j;
            final int size = Math.max(getIconHeight(), 2);
            g.translate(x, y);

            if (color == null) {
                g.setColor(new Color(0, 0, 0));
            } else {
                g.setColor(color);
            }
            j = 0;

            if (color == null) {
                g.drawLine(0, 8, 5, 8);
                g.drawLine(5, 8, 5, 7);
                g.drawLine(1, 8, 1, 1);
                g.drawLine(0, 1, 2, 1);
            } else {
                for (int i = size - 1; i >= 0; i--) {
                    g.drawLine(0, j, 7, j);
                    j++;
                }
            }
            g.translate(-x, -y);
            g.setColor(oldColor);
        }
    }

    public class ColorPickerActionListener implements ActionListener {
        Color color;

        ColorPickerActionListener(Color color) {
            this.color = color;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            colorSetButton.setColor(color);
            final String hex = ColorUtils.colorRGBToHex(color);
            final String acad = ColorUtils.getColorIndexRegistry(hex);
            colorSetButton.setToolTipText("Index color: " + acad + "  Hex:"
                    + hex + "   RGB: " + color.getRed() + ","
                    + color.getGreen() + "," + color.getBlue());
            setFeatureColor(color);
        }
    }

}