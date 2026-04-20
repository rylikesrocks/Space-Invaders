import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;

public class GameView extends JPanel {
	// GameView: Swing component responsible for rendering the GameModel.
	// This view only reads from the GameModel and never mutates it.

	public static final int WIDTH = 800;
	public static final int HEIGHT = 600;

	private GameModel model;
	private int lastLives = -1;
	private long damageStartNanos = 0L;
	private long damageEndNanos = 0L;
	private final long DAMAGE_DURATION_NANOS = 3_000_000_000L; // 3 seconds

	// Starfield
	private final List<int[]> stars = new ArrayList<>();
	private final Random rng = new Random(42);
	private final int STAR_COUNT = 120;

	public GameView() {
		this(null);
	}

	public GameView(GameModel model) {
		this.model = model;
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setBackground(Color.BLACK);
		// initialize stars
		for (int i = 0; i < STAR_COUNT; i++) {
			int sx = rng.nextInt(WIDTH);
			int sy = rng.nextInt(HEIGHT);
			int sz = 1 + rng.nextInt(3);
			stars.add(new int[]{sx, sy, sz});
		}
		if (model != null) lastLives = model.getLives();
	}

	public void setModel(GameModel model) {
		this.model = model;
		if (model != null) lastLives = model.getLives();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		long now = System.nanoTime();

		if (model == null) {
			g2.setColor(Color.DARK_GRAY);
			g2.fillRect(0, 0, getWidth(), getHeight());
			g2.dispose();
			return;
		}

		// Draw background (space) with stars behind everything
		g2.setColor(getBackground());
		g2.fillRect(0, 0, getWidth(), getHeight());
		g2.setColor(Color.WHITE);
		for (int[] s : stars) {
			int sx = s[0];
			int sy = s[1];
			int sz = s[2];
			g2.fillRect(sx, sy, sz, sz);
		}

		// Detect damage: if lives decreased, start/refresh damage flash
		int currentLives = model.getLives();
		if (lastLives >= 0 && currentLives < lastLives) {
			damageStartNanos = now;
			damageEndNanos = damageStartNanos + DAMAGE_DURATION_NANOS;
		}
		lastLives = currentLives;

		// Draw player as a ship (main body + wings). If in damage flash window, blink red.
		double px = model.getPlayerX();
		double py = model.getPlayerY();
		int bodyW = 48;
		int bodyH = 12;
		int wingW = 18;
		int wingH = 8;
		int bodyX = (int) Math.round(px - bodyW / 2.0);
		int bodyY = (int) Math.round(py - bodyH / 2.0);

		boolean inDamage = now < damageEndNanos;
		boolean flashOn = true;
		if (inDamage) {
			long elapsed = now - damageStartNanos;
			long period = 300_000_000L; // 300ms
			flashOn = ((elapsed / period) % 2) == 0;
		}

		// Choose ship colors (when flashing show red)
		Color bodyColor = (inDamage && flashOn) ? Color.RED : new Color(0xCCCCCC);
		Color wingColor = (inDamage && flashOn) ? Color.RED : new Color(0xCC6600); // darker orange

		g2.setColor(bodyColor);
		g2.fillRect(bodyX, bodyY, bodyW, bodyH);
		// left wing
		int lwx = bodyX - wingW + 6;
		int lwy = bodyY + bodyH - 2;
		g2.setColor(wingColor);
		g2.fillRect(lwx, lwy, wingW, wingH);
		// right wing
		int rwx = bodyX + bodyW - 6;
		int rwy = lwy;
		g2.fillRect(rwx, rwy, wingW, wingH);

		// Draw aliens: main body ovals, antennas, and eyes
		int rows = model.getRows();
		int cols = model.getCols();
		int aw = model.getAlienWidth();
		int ah = model.getAlienHeight();
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				if (!model.isAlienAlive(r, c)) continue;
				int ax = (int) Math.round(model.getAlienX(r, c));
				int ay = (int) Math.round(model.getAlienY(r, c));
				// body
				g2.setColor(new Color(0x00AA00)); // green
				g2.fillOval(ax, ay, aw, ah);
				// antennas (two short lines)
				int cx = ax + aw / 2;
				int top = ay;
				g2.setColor(new Color(0x66FF66));
				g2.drawLine(cx - 6, top + 2, cx - 6, top - 6);
				g2.drawLine(cx + 6, top + 2, cx + 6, top - 6);
				// eyes
				int eyeW = Math.max(2, aw / 8);
				int eyeH = Math.max(2, ah / 6);
				int leftEyeX = ax + aw / 3 - eyeW / 2;
				int rightEyeX = ax + 2 * aw / 3 - eyeW / 2;
				int eyeY = ay + ah / 3;
				g2.setColor(Color.WHITE);
				g2.fillOval(leftEyeX, eyeY, eyeW, eyeH);
				g2.fillOval(rightEyeX, eyeY, eyeW, eyeH);
				g2.setColor(Color.BLACK);
				g2.fillOval(leftEyeX + eyeW/4, eyeY + eyeH/4, Math.max(1, eyeW/2), Math.max(1, eyeH/2));
				g2.fillOval(rightEyeX + eyeW/4, eyeY + eyeH/4, Math.max(1, eyeW/2), Math.max(1, eyeH/2));
			}
		}

		// Draw player bullet
		GameModel.Bullet pb = model.getPlayerBullet();
		if (pb != null && pb.active) {
			g2.setColor(Color.YELLOW);
			int bw = 4;
			int bh = 10;
			g2.fillRect((int) Math.round(pb.x - bw / 2.0), (int) Math.round(pb.y - bh / 2.0), bw, bh);
		}

		// Draw alien bullets
		List<GameModel.Bullet> abs = model.getAlienBullets();
		g2.setColor(Color.ORANGE);
		for (GameModel.Bullet b : abs) {
			int bw = 4;
			int bh = 10;
			g2.fillRect((int) Math.round(b.x - bw / 2.0), (int) Math.round(b.y - bh / 2.0), bw, bh);
		}

		// Draw score and lives
		g2.setColor(Color.WHITE);
		g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
		g2.drawString("Score: " + model.getScore(), 12, 20);
		g2.drawString("Lives: " + model.getLives(), getWidth() - 100, 20);

		// Game over message
		if (model.isGameOver()) {
			String msg = "GAME OVER";
			g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
			int sw = g2.getFontMetrics().stringWidth(msg);
			int sh = g2.getFontMetrics().getHeight();
			int cx = (getWidth() - sw) / 2;
			int cy = (getHeight() - sh) / 2 + sh;
			g2.setColor(Color.RED);
			g2.drawString(msg, cx, cy);
		}

		g2.dispose();
	}
}
