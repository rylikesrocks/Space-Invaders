import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JPanel;

public class GameView extends JPanel {
	// GameView: Swing component responsible for rendering the GameModel.
	// This view only reads from the GameModel and never mutates it.

	public static final int WIDTH = 800;
	public static final int HEIGHT = 600;

	private GameModel model;

	public GameView() {
		this(null);
	}

	public GameView(GameModel model) {
		this.model = model;
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setBackground(Color.BLACK);
	}

	public void setModel(GameModel model) {
		this.model = model;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (model == null) {
			g2.setColor(Color.DARK_GRAY);
			g2.fillRect(0, 0, getWidth(), getHeight());
			g2.dispose();
			return;
		}

		// Draw background
		g2.setColor(getBackground());
		g2.fillRect(0, 0, getWidth(), getHeight());

		// Draw player
		double px = model.getPlayerX();
		double py = model.getPlayerY();
		int pw = 60;
		int ph = 16;
		int px0 = (int) Math.round(px - pw / 2.0);
		int py0 = (int) Math.round(py - ph / 2.0);
		g2.setColor(Color.GREEN);
		g2.fillRect(px0, py0, pw, ph);

		// Draw aliens
		int rows = model.getRows();
		int cols = model.getCols();
		int aw = model.getAlienWidth();
		int ah = model.getAlienHeight();
		g2.setColor(Color.CYAN);
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				if (!model.isAlienAlive(r, c)) continue;
				int ax = (int) Math.round(model.getAlienX(r, c));
				int ay = (int) Math.round(model.getAlienY(r, c));
				g2.fillRect(ax, ay, aw, ah);
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
