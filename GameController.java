import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class GameController {
	// GameController: Wires GameModel and GameView, handles input, and runs the game loop.

	private static final int FPS = 60;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			GameModel model = new GameModel();
			GameView view = new GameView(model);

			JFrame frame = new JFrame("Space Invaders");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.getContentPane().add(view);
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);

			// Input state
			final boolean[] leftPressed = {false};
			final boolean[] rightPressed = {false};

			// Key bindings on the view (so it works even if focused)
			InputMap im = view.getInputMap();
			ActionMap am = view.getActionMap();

			im.put(KeyStroke.getKeyStroke("pressed LEFT"), "left.press");
			im.put(KeyStroke.getKeyStroke("released LEFT"), "left.release");
			im.put(KeyStroke.getKeyStroke("pressed RIGHT"), "right.press");
			im.put(KeyStroke.getKeyStroke("released RIGHT"), "right.release");
			im.put(KeyStroke.getKeyStroke("pressed SPACE"), "space.press");

			am.put("left.press", new AbstractAction() { public void actionPerformed(ActionEvent e) { leftPressed[0] = true; } });
			am.put("left.release", new AbstractAction() { public void actionPerformed(ActionEvent e) { leftPressed[0] = false; } });
			am.put("right.press", new AbstractAction() { public void actionPerformed(ActionEvent e) { rightPressed[0] = true; } });
			am.put("right.release", new AbstractAction() { public void actionPerformed(ActionEvent e) { rightPressed[0] = false; } });
			am.put("space.press", new AbstractAction() { public void actionPerformed(ActionEvent e) { model.firePlayerBullet(); } });

			// Game loop using Swing Timer
			final long[] lastTime = {System.nanoTime()};
			int delayMs = 1000 / FPS;
			Timer timer = new Timer(delayMs, null);
			timer.addActionListener((ActionEvent e) -> {
				long now = System.nanoTime();
				double deltaSec = (now - lastTime[0]) / 1_000_000_000.0;
				lastTime[0] = now;

				// Process continuous input
				if (leftPressed[0]) model.movePlayerLeft(deltaSec);
				if (rightPressed[0]) model.movePlayerRight(deltaSec);

				// Update model
				model.update(deltaSec);

				// Repaint view
				view.repaint();

				// Stop when game over
				if (model.isGameOver()) {
					timer.stop();
				}
			});
			timer.setRepeats(true);
			timer.start();
		});
	}
}
