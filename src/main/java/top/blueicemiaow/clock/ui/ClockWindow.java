package top.blueicemiaow.clock.ui;

import top.blueicemiaow.clock.config.ConfigException;
import top.blueicemiaow.clock.config.ConfigLoader;
import top.blueicemiaow.clock.config.ConfigSource;
import top.blueicemiaow.clock.config.LoadedConfig;
import top.blueicemiaow.clock.render.ClockSchedule;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.time.ZonedDateTime;

/**
 * Clock window. It starts as a borderless full-screen window.
 * <p>
 * Escape exits, F5 reloads the configuration, F8 minimizes and F11 toggles
 * between borderless full screen and a normal decorated window.
 */
public final class ClockWindow {

    private final ConfigSource source;
    private LoadedConfig loaded;

    private JFrame frame;
    private ClockPanel panel;
    private Timer timer;
    private ClockSchedule schedule;
    private boolean perPixelTranslucent;
    private boolean fullscreen = true;
    private Rectangle windowedBounds;

    private ClockWindow(LoadedConfig loaded, ConfigSource source) {
        this.loaded = loaded;
        this.source = source;
    }

    public static void show(LoadedConfig loaded, ConfigSource source) {
        new ClockWindow(loaded, source).open();
    }

    private void open() {
        fullscreen = true;
        createFrame();
        updateSchedule();

        System.out.println("Clock started with " + source.label());
        System.out.println("Layout items: " + loaded.config().getLayout().size());
        System.out.println("Press Esc to exit, F5 to reload, F8 to minimize, F11 to toggle full screen.");
    }

    private void createFrame() {
        GraphicsDevice device = pickDevice();
        perPixelTranslucent = fullscreen && shouldUsePerPixelTranslucency(device);

        panel = new ClockPanel(loaded, perPixelTranslucent);
        frame = new JFrame(loaded.config().getTitle());
        frame.setUndecorated(fullscreen);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(!fullscreen);
        frame.setAlwaysOnTop(loaded.config().isAlwaysOnTop());
        frame.setContentPane(panel);

        applyFrameAppearance();

        if (fullscreen) {
            frame.setBounds(screenBounds(device));
        } else {
            Rectangle bounds = windowedBoundsFor(device);
            windowedBounds = bounds;
            frame.setMinimumSize(new Dimension(320, 200));
            frame.setBounds(bounds);
        }

        installKeyBindings();
        applyCursor();

        frame.setVisible(true);
        frame.toFront();
        frame.requestFocus();
    }

    private void reload() {
        try {
            LoadedConfig next = ConfigLoader.load(source);
            loaded = next;

            GraphicsDevice device = pickDevice();
            perPixelTranslucent = fullscreen && shouldUsePerPixelTranslucency(device);

            frame.setAlwaysOnTop(next.config().isAlwaysOnTop());
            frame.setTitle(next.config().getTitle());
            if (fullscreen) {
                frame.setBounds(screenBounds(device));
            } else {
                windowedBounds = frame.getBounds();
                Rectangle bounds = windowedBoundsFor(device);
                windowedBounds = bounds;
                frame.setBounds(bounds);
            }
            applyFrameAppearance();
            panel.setLoaded(next);
            applyCursor();
            updateSchedule();
            panel.revalidate();
            panel.repaint();
            frame.toFront();
            frame.requestFocus();
            System.out.println("Reloaded configuration: " + source.label());
        } catch (ConfigException e) {
            System.err.println(e.getMessage());
            JOptionPane.showMessageDialog(frame, e.getMessage(),
                    "Unable to reload configuration", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleFullscreen() {
        if (timer != null) {
            timer.stop();
        }

        if (fullscreen) {
            fullscreen = false;
            windowedBounds = windowedBoundsFor(pickDevice());
        } else {
            windowedBounds = frame.getBounds();
            fullscreen = true;
        }

        frame.dispose();
        createFrame();
        updateSchedule();
    }

    private void minimize() {
        frame.setExtendedState(JFrame.ICONIFIED);
    }

    private void updateSchedule() {
        schedule = ClockSchedule.from(loaded.config());
        if (schedule == null) {
            if (timer != null) {
                timer.stop();
                timer = null;
            }
            return;
        }

        if (timer == null) {
            timer = new Timer(1_000, event -> {
                panel.repaintTime();
                scheduleNextTick();
            });
            timer.setRepeats(false);
            timer.setCoalesce(true);
        }
        scheduleNextTick();
    }

    private void scheduleNextTick() {
        if (timer == null || schedule == null) {
            return;
        }
        long delay = schedule.millisUntilNext(ZonedDateTime.now(loaded.zone()));
        timer.setInitialDelay((int) Math.min(Integer.MAX_VALUE, Math.max(1L, delay)));
        timer.restart();
    }

    private void applyFrameAppearance() {
        boolean translucentApplied = false;
        if (perPixelTranslucent) {
            try {
                frame.setBackground(new Color(0, 0, 0, 0));
                frame.getRootPane().setOpaque(false);
                frame.getLayeredPane().setOpaque(false);
                translucentApplied = true;
            } catch (RuntimeException e) {
                // Some window managers refuse changing translucency on a
                // visible window. Fall back to compositing over black.
                perPixelTranslucent = false;
            }
        }
        if (!translucentApplied) {
            Color background = loaded.config().getBackground().toColor();
            frame.setBackground(background.getAlpha() == 255 ? background : Color.BLACK);
            frame.getRootPane().setOpaque(true);
            frame.getLayeredPane().setOpaque(true);
        }
        panel.setPerPixelTranslucent(perPixelTranslucent);
    }

    private void applyCursor() {
        if (fullscreen && loaded.config().isHideCursor()) {
            try {
                BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                Cursor blank = Toolkit.getDefaultToolkit()
                        .createCustomCursor(image, new Point(0, 0), "clock-hidden-cursor");
                panel.setCursor(blank);
            } catch (Exception e) {
                panel.setCursor(Cursor.getDefaultCursor());
            }
        } else {
            panel.setCursor(Cursor.getDefaultCursor());
        }
    }

    private void installKeyBindings() {
        InputMap inputMap = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = frame.getRootPane().getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "exit-clock");
        actionMap.put("exit-clock", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
                System.exit(0);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("F5"), "reload-clock");
        actionMap.put("reload-clock", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reload();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("F8"), "minimize-clock");
        actionMap.put("minimize-clock", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                minimize();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("F11"), "toggle-fullscreen");
        actionMap.put("toggle-fullscreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(ClockWindow.this::toggleFullscreen);
            }
        });
    }

    private GraphicsDevice pickDevice() {
        GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        if (devices.length == 0) {
            throw new IllegalStateException("No screen devices are available");
        }
        int index = loaded.config().getScreen();
        if (index < 0 || index >= devices.length) {
            System.err.println("Screen index " + index + " is not available; using screen 0.");
            index = 0;
        }
        return devices[index];
    }

    private static Rectangle screenBounds(GraphicsDevice device) {
        return device.getDefaultConfiguration().getBounds();
    }

    private Rectangle windowedBoundsFor(GraphicsDevice device) {
        Rectangle screen = screenBounds(device);
        if (windowedBounds == null) {
            int width = Math.min(screen.width,
                    Math.max(320, Math.min(1600, screen.width * 2 / 3)));
            int height = Math.min(screen.height,
                    Math.max(200, Math.min(900, screen.height * 2 / 3)));
            return new Rectangle(
                    screen.x + (screen.width - width) / 2,
                    screen.y + (screen.height - height) / 2,
                    width,
                    height);
        }

        Rectangle bounds = new Rectangle(windowedBounds);
        Rectangle virtualScreen = virtualScreenBounds();
        bounds.width = Math.min(bounds.width, virtualScreen.width);
        bounds.height = Math.min(bounds.height, virtualScreen.height);
        if (!virtualScreen.contains(bounds)) {
            bounds.x = screen.x + (screen.width - bounds.width) / 2;
            bounds.y = screen.y + (screen.height - bounds.height) / 2;
        }
        return bounds;
    }

    private static Rectangle virtualScreenBounds() {
        Rectangle union = null;
        for (GraphicsDevice device
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            Rectangle bounds = screenBounds(device);
            union = union == null ? bounds : union.union(bounds);
        }
        return union == null ? new Rectangle(0, 0, 1, 1) : union;
    }

    private boolean shouldUsePerPixelTranslucency(GraphicsDevice device) {
        if (loaded.config().getBackground().alpha() >= 255) {
            return false;
        }
        try {
            return device.isWindowTranslucencySupported(
                    GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
