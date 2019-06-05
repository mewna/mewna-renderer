package com.mewna.renderer;

import com.mewna.renderer.TextureManager.Background;
import com.mewna.renderer.api.ProfileData;
import com.mewna.renderer.api.RankData;
import com.mewna.renderer.utils.CacheUtil;
import com.mewna.renderer.utils.CacheUtil.CachedImage;
import com.mewna.renderer.utils.LevelUtils;
import com.mewna.renderer.utils.Numbers;
import io.sentry.Sentry;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.AttributedString;
import java.util.List;
import java.util.*;

/**
 * @author amy
 * @since 4/10/19.
 */
@SuppressWarnings("unused")
public final class CardRenderer {
    // colours
    
    @SuppressWarnings("WeakerAccess")
    public static final String PRIMARY_COLOUR_STR = "0xdb325c";
    @SuppressWarnings("unused")
    public static final int PRIMARY_COLOUR = 0xdb325c;
    private static final Color NINETY_PERCENT_OPAQUE_BLACK = new Color(0, 0, 0, 180);
    private static final Color SIXTY_SEVEN_PERCENT_OPAQUE_BLACK = new Color(0, 0, 0, 127);
    private static final Color PRIMARY_THEME_COLOUR = Color.decode(PRIMARY_COLOUR_STR);
    
    // fonts
    
    private static final Font USERNAME_FONT;
    private static final Font STATS_FONT;
    private static final Font STATS_FONT_SMALLER;
    private static final Font ABOUT_ME_FONT;
    
    private static final Map<TextAttribute, Object> FONT_SETTINGS = new HashMap<>();
    
    static {
        FONT_SETTINGS.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        
        try {
            try(final InputStream stream = Renderer.class.getResourceAsStream("/fonts/YanoneKaffeesatz-Regular.otf")) {
                final Font yanone = Font.createFont(Font.TRUETYPE_FONT, stream);
                final Map<TextAttribute, Object> settings = new HashMap<>(FONT_SETTINGS);
                settings.put(TextAttribute.SIZE, 42);
                USERNAME_FONT = yanone.deriveFont(settings);
            }
            try(final InputStream stream = Renderer.class.getResourceAsStream("/fonts/DroidSans.ttf")) {
                final Font droid = Font.createFont(Font.TRUETYPE_FONT, stream);
                {
                    final Map<TextAttribute, Object> settings = new HashMap<>(FONT_SETTINGS);
                    settings.put(TextAttribute.SIZE, 32);
                    STATS_FONT = droid.deriveFont(settings);
                }
                {
                    final Map<TextAttribute, Object> settings = new HashMap<>(FONT_SETTINGS);
                    settings.put(TextAttribute.SIZE, 30);
                    STATS_FONT_SMALLER = droid.deriveFont(settings);
                }
                {
                    final Map<TextAttribute, Object> settings = new HashMap<>(FONT_SETTINGS);
                    settings.put(TextAttribute.SIZE, 24);
                    ABOUT_ME_FONT = droid.deriveFont(settings);
                }
            }
        } catch(final FontFormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private CardRenderer() {
    }
    
    private static CachedImage getBackground(final String backgroundName) {
        final Optional<Background> background = TextureManager.getBackground(backgroundName);
        final Background bg = background.orElse(TextureManager.defaultBg);
        return CacheUtil.getImageResource(bg.getPath());
    }
    
    private static void setRenderHints(final Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    }
    
    private static List<String> wrap(final String txt, final FontMetrics fm, @SuppressWarnings("SameParameterValue") final int maxWidth) {
        final StringTokenizer st = new StringTokenizer(txt);
        
        final List<String> list = new ArrayList<>();
        String line = "";
        String lineBeforeAppend;
        while(st.hasMoreTokens()) {
            final String seg = st.nextToken();
            lineBeforeAppend = line;
            final int width = fm.stringWidth(line + seg + ' ');
            if(width >= maxWidth) {
                list.add(lineBeforeAppend);
                line = seg + ' ';
            } else {
                line += seg + ' ';
            }
        }
        //the remaining part.
        if(!line.isEmpty()) {
            list.add(line);
        }
        return list;
    }
    
    public static byte[] generateProfileCard(final Renderer renderer, final ProfileData profileData) {
        renderer.statsClient().count("renderer.cards", 1, "type:profile");
        final BufferedImage card = new BufferedImage(600, 600, BufferedImage.TYPE_INT_ARGB);
        try {
            final Graphics2D g2 = card.createGraphics();
            setRenderHints(g2);
            // Background
            final CachedImage bg = getBackground(profileData.background());
            final AffineTransform transform = new AffineTransform();
            // compute scale
            // this is a square, so we scale the smallest dimension (height) to reach it
            final int bgHeight = bg.getImage().getHeight();
            final double scale = 1 / (bgHeight / 600D);
            transform.scale(scale, scale);
            final BufferedImageOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC);
            g2.drawImage(bg.getImage(), op, 0, 0);
            
            // Main card panel
            g2.setColor(NINETY_PERCENT_OPAQUE_BLACK);
            g2.fillRect(10, 100, 580, 476);
            
            // Avatar panel
            g2.setColor(SIXTY_SEVEN_PERCENT_OPAQUE_BLACK);
            
            g2.fillRect(234, 34, 132, 132);
            
            // Avatar
            final BufferedImage avatar = TextureManager.getCachedAvatar(renderer, profileData.id(), profileData.avatarUrl());
            g2.drawImage(avatar, 236, 36, 128, 128, null);
            
            // Username
            // centered string at y=246
            setRenderHints(g2);
            drawCenteredString(g2, profileData.displayName().toUpperCase(),
                    new Rectangle(10, 202, 580, USERNAME_FONT.getSize()), USERNAME_FONT, Color.WHITE);
            
            g2.setFont(ABOUT_ME_FONT);
            g2.setColor(Color.WHITE);
            // About text
            final FontMetrics aboutMeFontMetrics = g2.getFontMetrics(ABOUT_ME_FONT);
            // 32, 268
            // 536x122
            final List<String> wrap = wrap(profileData.aboutText(), aboutMeFontMetrics, 536);
            int y = 268;
            for(final String line : wrap) {
                g2.drawString(line, 32, y + ABOUT_ME_FONT.getSize());
                y += ABOUT_ME_FONT.getSize();
            }
            
            // Stats
            final FontMetrics statsFontSmallerMetrics = g2.getFontMetrics(ABOUT_ME_FONT);
            
            final String money = Numbers.format(profileData.money());
            final String moneyLabel = "MONEY";
            final int moneyLabelWidth = statsFontSmallerMetrics.stringWidth(moneyLabel);
            
            final String tato = Numbers.formatBD(new BigDecimal(profileData.tato()));
            final String tatoLabel = "TATO";
            final int tatoLabelWidth = statsFontSmallerMetrics.stringWidth(tatoLabel);
            
            final String score = Numbers.format(profileData.score());
            final String scoreLabel = "OVERALL SCORE";
            final int scoreLabelWidth = statsFontSmallerMetrics.stringWidth(scoreLabel);
            
            // Ensure everything is roughly the same size
            final int allLabelSizes = Math.max(scoreLabelWidth, Math.max(tatoLabelWidth, moneyLabelWidth));
            
            // 32, 423
            // Money
            drawCenteredString(g2, money, new Rectangle(32, 423, allLabelSizes,
                    STATS_FONT.getSize()), STATS_FONT, PRIMARY_THEME_COLOUR);
            drawCenteredString(g2, moneyLabel, new Rectangle(32, 423, allLabelSizes,
                    STATS_FONT.getSize() * 2 + ABOUT_ME_FONT.getSize()), ABOUT_ME_FONT, Color.WHITE);
            
            // 234, 423
            // Tato
            drawCenteredString(g2, tato, new Rectangle(300 - allLabelSizes / 2, 423, allLabelSizes,
                    STATS_FONT.getSize()), STATS_FONT, PRIMARY_THEME_COLOUR);
            drawCenteredString(g2, tatoLabel, new Rectangle(300 - allLabelSizes / 2, 423, allLabelSizes,
                    STATS_FONT.getSize() * 2 + ABOUT_ME_FONT.getSize()), ABOUT_ME_FONT, Color.WHITE);
            
            // 435, 423
            // Score
            drawCenteredString(g2, score, new Rectangle(568 - allLabelSizes, 423, allLabelSizes,
                    STATS_FONT.getSize()), STATS_FONT, PRIMARY_THEME_COLOUR);
            drawCenteredString(g2, scoreLabel, new Rectangle(568 - allLabelSizes, 423, allLabelSizes,
                    STATS_FONT.getSize() * 2 + ABOUT_ME_FONT.getSize()), ABOUT_ME_FONT, Color.WHITE);
            
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(card, "png", baos);
            g2.dispose();
            final byte[] bytes = baos.toByteArray();
            baos.close();
            return bytes;
        } catch(final IOException e) {
            Sentry.capture(e);
            throw new IllegalStateException(e);
        }
    }
    
    public static byte[] generateRankCard(final Renderer renderer, final RankData rankData) {
        renderer.statsClient().count("renderer.cards", 1, "type:rank");
        final BufferedImage card = new BufferedImage(800, 200, BufferedImage.TYPE_INT_ARGB);
        try {
            final Graphics2D g2 = card.createGraphics();
            setRenderHints(g2);
            // Background
            final CachedImage bg = getBackground(rankData.background());
            final AffineTransform transform = new AffineTransform();
            // compute scale
            // this is a 800x200 rect, so we scale the smallest dimension (width) to reach it
            final int bgWidth = bg.getImage().getWidth();
            final double scale = 1 / (bgWidth / 800D);
            transform.scale(scale, scale);
            final BufferedImageOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC);
            g2.drawImage(bg.getImage(), op, 0, 0);
            
            // Main card panel
            g2.setColor(NINETY_PERCENT_OPAQUE_BLACK);
            g2.fillRect(100, 10, 676, 180);
            
            // Avatar panel
            g2.setColor(SIXTY_SEVEN_PERCENT_OPAQUE_BLACK);
            
            g2.fillRect(34, 34, 132, 132);
            
            // Avatar
            final BufferedImage avatar = TextureManager.getCachedAvatar(renderer, rankData.id(), rankData.avatarUrl());
            g2.drawImage(avatar, 36, 36, 128, 128, null);
            
            // Username
            g2.setPaint(Color.WHITE);
            g2.setFont(USERNAME_FONT);
            setRenderHints(g2);
            g2.drawString(rankData.username().toUpperCase(), 187, 70);
            
            // Stats
            
            final String lvl = "LVL ";
            final String rank = "RANK #";
            
            g2.setFont(STATS_FONT);
            setRenderHints(g2);
            
            // User stats
            final long userXp = rankData.exp();
            final long userLevel = LevelUtils.xpToLevel(userXp);
            final long userLevelXp = LevelUtils.fullLevelToXp(userLevel);
            final long nextLevel = userLevel + 1;
            final long playerRank = rankData.rank();
            // Text
            final String userLevelText = Numbers.format(userLevel) + "    ";
            final String playerRankText = Numbers.format(playerRank);
            // Font sizing util
            final FontMetrics metrics = g2.getFontMetrics(STATS_FONT);
            final int lvlWidth = metrics.stringWidth(lvl);
            final int rankWidth = metrics.stringWidth(rank);
            final int playerLevelWidth = metrics.stringWidth(userLevelText);
            
            g2.drawString(lvl, 187, 113); // LVL
            g2.setPaint(PRIMARY_THEME_COLOUR);
            g2.drawString(userLevelText, 187 + lvlWidth, 113); // 1234
            g2.setPaint(Color.WHITE);
            g2.drawString(rank, 187 + lvlWidth + playerLevelWidth, 113); // RANK #
            g2.setPaint(PRIMARY_THEME_COLOUR);
            g2.drawString(playerRankText, 187 + lvlWidth + playerLevelWidth + rankWidth, 113); // 123456
            
            // XP bar
            final long nextLevelXp = LevelUtils.fullLevelToXp(nextLevel);
            final long xpNeeded = LevelUtils.nextLevelXp(userXp);
            final long nextXpTotal = nextLevelXp - xpNeeded;
            g2.setColor(SIXTY_SEVEN_PERCENT_OPAQUE_BLACK);
            g2.fillRect(188, 123, 566, 42);
            // calc. bar size
            final int barWidth = (int) (562 * ((userXp - userLevelXp) / (double) (nextLevelXp - userLevelXp)));
            g2.setColor(PRIMARY_THEME_COLOUR);
            g2.fillRect(190, 125, barWidth, 38);
            // XP text
            drawCenteredString(g2, String.format("%s / %s EXP", Numbers.format(nextXpTotal), Numbers.format(nextLevelXp)),
                    new Rectangle(190, 125, 562, 38), STATS_FONT_SMALLER, Color.WHITE);
            
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(card, "png", baos);
            g2.dispose();
            final byte[] bytes = baos.toByteArray();
            baos.close();
            return bytes;
        } catch(final IOException e) {
            Sentry.capture(e);
            throw new IllegalStateException(e);
        }
    }
    
    private static void drawCenteredString(final Graphics2D g2, final String text, final Rectangle rect, final Font font,
                                           final Color color) {
        final FontMetrics metrics = g2.getFontMetrics(font);
        final int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        final int y = rect.y + (rect.height - metrics.getHeight()) / 2 + metrics.getAscent();
        g2.setFont(font);
        setRenderHints(g2);
        g2.setColor(color);
        g2.drawString(text, x, y);
    }
    
    private static void drawCenteredString(final Graphics2D g2, final String text, final AttributedString renderable,
                                           final Rectangle rect, @SuppressWarnings("SameParameterValue") final Font font) {
        final FontMetrics metrics = g2.getFontMetrics(font);
        final int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        final int y = rect.y + (rect.height - metrics.getHeight()) / 2 + metrics.getAscent();
        g2.setFont(font);
        renderable.addAttribute(TextAttribute.FONT, font);
        setRenderHints(g2);
        g2.drawString(renderable.getIterator(), x, y);
    }
}
