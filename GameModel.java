import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameModel {
	// GameModel: Holds game state and logic (no Swing/AWT/Swing imports here).
	// Tracks:
	// - player horizontal position and bounds
	// - alien formation (5 rows x 11 cols)
	// - single player bullet (one at a time)
	// - multiple alien bullets
	// - score and lives

	public static final int WIDTH = 800;
	public static final int HEIGHT = 600;

	// Player
	private double playerX;
	private final int playerWidth = 60;
	private final int playerHeight = 16;
	private final double playerY = HEIGHT - 50; // fixed vertical position
	private final double playerSpeed = 300; // px/sec

	// Player bullet (one at a time)
	public static class Bullet {
		public double x, y;
		public double dy; // pixels per second (positive = down)
		public boolean active;
		public boolean fromAlien;

		public Bullet(double x, double y, double dy, boolean fromAlien) {
			this.x = x;
			this.y = y;
			this.dy = dy;
			this.active = true;
			this.fromAlien = fromAlien;
		}
	}

	private Bullet playerBullet; // null or inactive when not in flight
	private final List<Bullet> alienBullets = new ArrayList<>();

	// Aliens formation
	private final int rows = 5;
	private final int cols = 11;
	private final boolean[][] aliensAlive = new boolean[rows][cols];
	private double formationX = 50;
	private double formationY = 50;
	private final int alienWidth = 36;
	private final int alienHeight = 24;
	private final int hSpacing = 8;
	private final int vSpacing = 12;
	private double alienSpeed = 40; // px/sec horizontally
	private boolean movingRight = true;
	private final int dropDistance = 20;

	// Gameplay
	private int score = 0;
	private int lives = 3;
	private boolean gameOver = false;

	private final Random rng = new Random();

	public GameModel() {
		resetAliens();
		playerX = WIDTH / 2.0;
		playerBullet = null;
	}

	private void resetAliens() {
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				aliensAlive[r][c] = true;
			}
		}
		formationX = 50;
		formationY = 50;
		movingRight = true;
		alienSpeed = 40;
	}

	// --- Player controls ---
	public void movePlayerLeft(double deltaSeconds) {
		playerX -= playerSpeed * deltaSeconds;
		clampPlayer();
	}

	public void movePlayerRight(double deltaSeconds) {
		playerX += playerSpeed * deltaSeconds;
		clampPlayer();
	}

	private void clampPlayer() {
		double half = playerWidth / 2.0;
		if (playerX - half < 0) playerX = half;
		if (playerX + half > WIDTH) playerX = WIDTH - half;
	}

	public void firePlayerBullet() {
		if (playerBullet == null || !playerBullet.active) {
			double bx = playerX;
			double by = playerY - playerHeight / 2.0 - 4;
			playerBullet = new Bullet(bx, by, -420.0, false);
		}
	}

	// --- Game update (tick) ---
	// Call regularly with time delta in seconds
	public void update(double deltaSeconds) {
		if (gameOver) return;

		// Move player bullet
		if (playerBullet != null && playerBullet.active) {
			playerBullet.y += playerBullet.dy * deltaSeconds;
			if (playerBullet.y < 0) playerBullet.active = false;
		}

		// Move alien bullets
		Iterator<Bullet> it = alienBullets.iterator();
		while (it.hasNext()) {
			Bullet b = it.next();
			b.y += b.dy * deltaSeconds;
			if (b.y > HEIGHT) {
				it.remove();
			} else if (checkBulletHitsPlayer(b)) {
				it.remove();
				lives--;
				if (lives <= 0) gameOver = true;
			}
		}

		// Move alien formation horizontally
		double formationWidth = cols * alienWidth + (cols - 1) * hSpacing;
		double dx = (movingRight ? alienSpeed : -alienSpeed) * deltaSeconds;
		formationX += dx;
		// Check edges
		if (movingRight && formationX + formationWidth >= WIDTH - 10) {
			// clamp, drop, reverse
			formationX = Math.min(formationX, WIDTH - 10 - formationWidth);
			formationY += dropDistance;
			movingRight = false;
		} else if (!movingRight && formationX <= 10) {
			formationX = Math.max(formationX, 10);
			formationY += dropDistance;
			movingRight = true;
		}

		// Random alien firing
		double fireRatePerSecond = 0.6; // average bullets per second
		if (rng.nextDouble() < fireRatePerSecond * deltaSeconds) {
			spawnAlienBullet();
		}

		// Collision detection: player bullet vs aliens
		if (playerBullet != null && playerBullet.active) {
			outer:
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					if (!aliensAlive[r][c]) continue;
					double ax = formationX + c * (alienWidth + hSpacing);
					double ay = formationY + r * (alienHeight + vSpacing);
					if (pointIntersectsRect(playerBullet.x, playerBullet.y, ax, ay, alienWidth, alienHeight)) {
						aliensAlive[r][c] = false;
						playerBullet.active = false;
						score += 10;
						break outer;
					}
				}
			}
		}

		// Simple cleanup: if player bullet inactive set to null
		if (playerBullet != null && !playerBullet.active) playerBullet = null;
	}

	private void spawnAlienBullet() {
		// choose a random alive alien (try a few times)
		int tries = 50;
		for (int t = 0; t < tries; t++) {
			int r = rng.nextInt(rows);
			int c = rng.nextInt(cols);
			if (!aliensAlive[r][c]) continue;
			double ax = formationX + c * (alienWidth + hSpacing) + alienWidth / 2.0;
			double ay = formationY + r * (alienHeight + vSpacing) + alienHeight;
			Bullet b = new Bullet(ax, ay, 220.0, true);
			alienBullets.add(b);
			return;
		}
		// If none found, do nothing
	}

	private boolean checkBulletHitsPlayer(Bullet b) {
		double half = playerWidth / 2.0;
		double left = playerX - half;
		double right = playerX + half;
		double top = playerY - playerHeight / 2.0;
		double bottom = playerY + playerHeight / 2.0;
		return rectangleContainsPoint(left, top, playerWidth, playerHeight, b.x, b.y);
	}

	// --- Helpers ---
	private static boolean pointIntersectsRect(double px, double py, double rx, double ry, double rw, double rh) {
		return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
	}

	private static boolean rectangleContainsPoint(double rx, double ry, double rw, double rh, double px, double py) {
		return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
	}

	// --- Getters for the view/controller ---
	public double getPlayerX() { return playerX; }
	public double getPlayerY() { return playerY; }
	public Bullet getPlayerBullet() { return playerBullet; }
	public List<Bullet> getAlienBullets() { return new ArrayList<>(alienBullets); }
	public boolean isAlienAlive(int row, int col) { return aliensAlive[row][col]; }
	public double getAlienX(int row, int col) { return formationX + col * (alienWidth + hSpacing); }
	public double getAlienY(int row, int col) { return formationY + row * (alienHeight + vSpacing); }
	public int getRows() { return rows; }
	public int getCols() { return cols; }
	public int getAlienWidth() { return alienWidth; }
	public int getAlienHeight() { return alienHeight; }
	public int getScore() { return score; }
	public int getLives() { return lives; }
	public boolean isGameOver() { return gameOver; }
}
