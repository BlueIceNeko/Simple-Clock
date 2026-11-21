package top.blueicemiaow.clock.ui;

import top.blueicemiaow.clock.config.Anchor;
import top.blueicemiaow.clock.config.FontStyle;
import top.blueicemiaow.clock.config.LayoutItem;
import top.blueicemiaow.clock.config.LoadedConfig;
import top.blueicemiaow.clock.config.Rgba;
import top.blueicemiaow.clock.render.TimeText;

import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The actual clock canvas. Layout items are painted in array order; later
 * items are drawn on top of earlier ones.
 */
public final class ClockPanel extends JPanel {

    private static final Rgba DEFAULT_TEXT_COLOR = new Rgba(191, 191, 191, 255);

    private LoadedConfig loaded;
    private boolean perPixelTranslucent;
    private static final BufferedImage METRICS_IMAGE =
            new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

    private final Map<String, Font> fontCache = new HashMap<>();
    private final Map<Font, FontMetrics> metricsCache = new HashMap<>();
    private final Map<LayoutItem, Rectangle> dynamicBounds = new IdentityHashMap<>();
    private Supplier<ZonedDateTime> timeSource;
    private ZonedDateTime paintTime;

    public ClockPanel(LoadedConfig loaded, boolean perPixelTranslucent) {
        this.loaded = loaded;
        this.perPixelTranslucent = perPixelTranslucent;
        this.timeSource = () -> ZonedDateTime.now(loaded.zone());
        setOpaque(!perPixelTranslucent);
        setBackground(loaded.config().getBackground().toColor());
        setDoubleBuffered(true);
    }

    public void setLoaded(LoadedConfig loaded) {
        this.loaded = loaded;
        this.timeSource = () -> ZonedDateTime.now(loaded.zone());
        fontCache.clear();
        metricsCache.clear();
        dynamicBounds.clear();
        paintTime = null;
        setBackground(loaded.config().getBackground().toColor());
        repaint();
    }

    public void setPerPixelTranslucent(boolean perPixelTranslucent) {
        this.perPixelTranslucent = perPixelTranslucent;
        setOpaque(!perPixelTranslucent);
        dynamicBounds.clear();
        paintTime = null;
        repaint();
    }

    void setTimeSource(Supplier<ZonedDateTime> timeSource) {
        this.timeSource = timeSource;
    }

    /**
     * Repaints only the areas occupied by {@code {time}} / {@code {date}}
     * text. On a large translucent window a full-window repaint is very
     * expensive, while the changing text usually covers only a small part of
     * the screen.
     */
    public void repaintTime() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        ZonedDateTime now = timeSource.get();
        Rectangle dirty = null;
        boolean dynamic = false;

        for (LayoutItem item : loaded.config().getLayout()) {
            if (!isDynamic(item)) {
                continue;
            }
            dynamic = true;
            Rectangle current = textBounds(item, now, width, height);
            Rectangle previous = dynamicBounds.put(item, current);
            Rectangle region = previous == null ? current : current.union(previous);
            dirty = dirty == null ? region : dirty.union(region);
        }

        if (!dynamic) {
            paintTime = null;
            return;
        }

        if (dirty == null) {
            paintTime = null;
            return;
        }

        dirty.grow(6, 6);
        dirty = dirty.intersection(new Rectangle(0, 0, width, height));
        if (dirty.isEmpty()) {
            paintTime = null;
            return;
        }

        paintTime = now;
        repaint(dirty.x, dirty.y, dirty.width, dirty.height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        ZonedDateTime now = paintTime;
        paintTime = null;
        if (now == null) {
            now = timeSource.get();
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            paintClock(g2, getWidth(), getHeight(), now);
        } finally {
            g2.dispose();
        }
    }

    void paintClock(Graphics2D g2, int width, int height, ZonedDateTime now) {
        if (width <= 0 || height <= 0) {
            return;
        }
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        paintBackground(g2, width, height);

        for (LayoutItem item : loaded.config().getLayout()) {
            if (item == null) {
                continue;
            }
            if (item.isText()) {
                paintTextItem(g2, item, width, height, now);
            } else if (item.isImage()) {
                paintImageItem(g2, item, width, height);
            }
        }
    }

    private void paintBackground(Graphics2D g2, int width, int height) {
        Rectangle clip = g2.getClipBounds();
        int x = 0;
        int y = 0;
        int w = width;
        int h = height;
        if (clip != null) {
            x = clip.x;
            y = clip.y;
            w = clip.width;
            h = clip.height;
        }

        Color background = loaded.config().getBackground().toColor();
        if (perPixelTranslucent) {
            // AlphaComposite.Src is surprisingly slow on translucent window
            // surfaces (it falls back to a software path). Clearing first and
            // then painting with SrcOver replaces the pixels just as well and
            // stays on the fast path.
            g2.setComposite(AlphaComposite.Clear);
            g2.fillRect(x, y, w, h);
            g2.setComposite(AlphaComposite.SrcOver);
            g2.setColor(background);
            g2.fillRect(x, y, w, h);
        } else {
            // Alpha values still work on systems without per-pixel translucency:
            // the color is composited over black instead.
            g2.setColor(Color.BLACK);
            g2.fillRect(x, y, w, h);
            g2.setColor(background);
            g2.fillRect(x, y, w, h);
        }

        BufferedImage image = loaded.backgroundImage();
        if (image != null) {
            paintBackgroundImage(g2, image, width, height);
        }
    }

    /**
     * Draws the background image centered and scaled to cover the whole
     * window without changing its aspect ratio, over the background color.
     */
    private void paintBackgroundImage(Graphics2D g2, BufferedImage image,
                                      int width, int height) {
        double scale = Math.max(width / (double) image.getWidth(),
                height / (double) image.getHeight());
        int targetWidth = Math.max(width, (int) Math.ceil(image.getWidth() * scale));
        int targetHeight = Math.max(height, (int) Math.ceil(image.getHeight() * scale));
        int targetX = (width - targetWidth) / 2;
        int targetY = (height - targetHeight) / 2;

        double opacity = loaded.config().backgroundOpacity();
        g2.setComposite(AlphaComposite.SrcOver.derive((float) opacity));
        g2.drawImage(image, targetX, targetY, targetWidth, targetHeight, null);
        g2.setComposite(AlphaComposite.SrcOver);
    }

    private void paintTextItem(Graphics2D g2, LayoutItem item, int width, int height, ZonedDateTime now) {
        String text = TimeText.render(item.getText(), now,
                loaded.timeFormatter(item), loaded.dateFormatter(item), loaded.weekDef(item));
        paintText(g2, item, text, width, height);
    }

    /**
     * Text containing {@code {time}}, {@code {date}} or {@code {week}} changes
     * over time and is the only content that needs timed repaints.
     */
    private boolean isDynamic(LayoutItem item) {
        if (item == null || !item.isText() || item.getText() == null) {
            return false;
        }
        String text = item.getText();
        return text.contains(TimeText.PLACEHOLDER_TIME)
                || text.contains(TimeText.PLACEHOLDER_DATE)
                || text.contains(TimeText.PLACEHOLDER_WEEK);
    }

    /**
     * Approximates the rectangle that a text item occupies. Used to repaint
     * only the changing parts of a translucent window.
     */
    private Rectangle textBounds(LayoutItem item, ZonedDateTime now, int width, int height) {
        String text = TimeText.render(item.getText(), now,
                loaded.timeFormatter(item), loaded.dateFormatter(item), item.getWeekDef());

        Font font = fontFor(item);
        FontMetrics metrics = metricsFor(font);
        String[] lines = splitLines(text);
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, metrics.stringWidth(line));
        }
        int totalHeight = metrics.getHeight() * lines.length;

        Anchor anchor = item.resolveAnchor();
        int left = anchor.placeX(maxWidth, width, item.getX());
        int top = anchor.placeY(totalHeight, height, item.getY());
        return new Rectangle(left, top, maxWidth, totalHeight);
    }

    private void paintText(Graphics2D g2, LayoutItem item, String text, int width, int height) {
        Font font = fontFor(item);
        g2.setFont(font);
        FontMetrics metrics = metricsFor(font);

        String[] lines = splitLines(text);
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, metrics.stringWidth(line));
        }
        int lineHeight = metrics.getHeight();
        int totalHeight = lineHeight * lines.length;

        Anchor anchor = item.resolveAnchor();
        int left = anchor.placeX(maxWidth, width, item.getX());
        int top = anchor.placeY(totalHeight, height, item.getY());

        if (isDynamic(item)) {
            dynamicBounds.put(item, new Rectangle(left, top, maxWidth, totalHeight));
        }

        Rgba rgba = item.getColor() != null ? item.getColor() : DEFAULT_TEXT_COLOR;
        g2.setColor(rgba.toColor());

        Composite previous = g2.getComposite();
        double opacity = item.opacityOrOne();
        if (opacity < 1.0) {
            g2.setComposite(AlphaComposite.SrcOver.derive((float) opacity));
        }

        String align = item.textAlignOrCenter();
        int baseline = top + metrics.getAscent();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineWidth = metrics.stringWidth(line);
            int x = switch (align) {
                case "left" -> left;
                case "right" -> left + maxWidth - lineWidth;
                default -> left + (maxWidth - lineWidth) / 2;
            };
            g2.drawString(line, x, baseline + i * lineHeight);
        }

        g2.setComposite(previous);
    }

    private void paintImageItem(Graphics2D g2, LayoutItem item, int width, int height) {
        BufferedImage image = loaded.images().get(item);
        if (image == null) {
            return;
        }

        int sourceWidth = image.getWidth();
        int sourceHeight = image.getHeight();
        int targetWidth;
        int targetHeight;
        if (item.getWidth() != null && item.getHeight() != null) {
            targetWidth = item.getWidth();
            targetHeight = item.getHeight();
        } else if (item.getWidth() != null) {
            targetWidth = item.getWidth();
            targetHeight = Math.max(1, (int) Math.round(targetWidth * (double) sourceHeight / sourceWidth));
        } else if (item.getHeight() != null) {
            targetHeight = item.getHeight();
            targetWidth = Math.max(1, (int) Math.round(targetHeight * (double) sourceWidth / sourceHeight));
        } else {
            targetWidth = sourceWidth;
            targetHeight = sourceHeight;
        }

        Anchor anchor = item.resolveAnchor();
        int x = anchor.placeX(targetWidth, width, item.getX());
        int y = anchor.placeY(targetHeight, height, item.getY());

        Composite previous = g2.getComposite();
        double opacity = item.opacityOrOne();
        if (opacity < 1.0) {
            g2.setComposite(AlphaComposite.SrcOver.derive((float) opacity));
        }
        g2.drawImage(image, x, y, targetWidth, targetHeight, null);
        g2.setComposite(previous);
    }

    private static String[] splitLines(String text) {
        String normalized = text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n');
        return normalized.split("\n", -1);
    }

    private FontMetrics metricsFor(Font font) {
        return metricsCache.computeIfAbsent(font, key -> {
            Graphics2D g2 = METRICS_IMAGE.createGraphics();
            try {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                return g2.getFontMetrics(key);
            } finally {
                g2.dispose();
            }
        });
    }

    private Font fontFor(LayoutItem item) {
        String family = item.getFontFamily();
        String styleName = item.getFontStyle();
        int style = FontStyle.parse(styleName).awtStyle();
        int size = item.getFontSize();
        String key = family + '\u0000' + style + '\u0000' + size;
        return fontCache.computeIfAbsent(key, ignored -> new Font(family, style, size));
    }
}
