public class ModelTester {
    public static void main(String[] args) throws Exception {
        int failures = 0;

        // Test 1: Player cannot move past the left edge
        GameModel m1 = new GameModel();
        m1.movePlayerLeft(10.0); // large delta
        double px1 = m1.getPlayerX();
        boolean pass1 = px1 >= 0.0 && px1 <= GameModel.WIDTH;
        report(1, pass1);
        if (!pass1) failures++;

        // Test 2: Player cannot move past the right edge
        GameModel m2 = new GameModel();
        m2.movePlayerRight(10.0);
        double px2 = m2.getPlayerX();
        boolean pass2 = px2 >= 0.0 && px2 <= GameModel.WIDTH;
        report(2, pass2);
        if (!pass2) failures++;

        // Test 3: Firing while a bullet is already in flight does nothing
        GameModel m3 = new GameModel();
        m3.firePlayerBullet();
        GameModel.Bullet bBefore = m3.getPlayerBullet();
        m3.firePlayerBullet();
        GameModel.Bullet bAfter = m3.getPlayerBullet();
        boolean pass3 = bBefore == bAfter; // same bullet instance (second fire ignored)
        report(3, pass3);
        if (!pass3) failures++;

        // Test 4: A bullet that reaches the top is removed
        GameModel m4 = new GameModel();
        m4.firePlayerBullet();
        // Advance enough time for the bullet to exit the top (speeds are high)
        m4.update(2.0);
        boolean pass4 = m4.getPlayerBullet() == null;
        report(4, pass4);
        if (!pass4) failures++;

        // Test 5: Destroying an alien increases the score
        GameModel m5 = new GameModel();
        int initialScore = m5.getScore();
        // Aim roughly at an alive alien by moving the player beneath a living alien column
        boolean destroyed = false;
        double targetX = -1;
        for (int r = 0; r < m5.getRows() && targetX < 0; r++) {
            for (int c = 0; c < m5.getCols(); c++) {
                if (m5.isAlienAlive(r, c)) {
                    targetX = m5.getAlienX(r, c) + m5.getAlienWidth() / 2.0;
                    break;
                }
            }
        }
        if (targetX >= 0) {
            // Move player toward targetX in small steps until close
            for (int i = 0; i < 200 && Math.abs(m5.getPlayerX() - targetX) > 2.0; i++) {
                double dir = targetX > m5.getPlayerX() ? 1.0 : -1.0;
                if (dir > 0) m5.movePlayerRight(0.02);
                else m5.movePlayerLeft(0.02);
            }
            m5.firePlayerBullet();
            // Run updates up to a timeout until score increases
            for (int t = 0; t < 500; t++) {
                m5.update(0.02);
                if (m5.getScore() > initialScore) { destroyed = true; break; }
            }
        }
        boolean pass5 = destroyed;
        report(5, pass5);
        if (!pass5) failures++;

        // Test 6: Losing all lives triggers game-over
        GameModel m6 = new GameModel();
        int lives = m6.getLives();
        // Use reflection to add alien bullets that hit the player repeatedly
        java.lang.reflect.Field bulletsField = GameModel.class.getDeclaredField("alienBullets");
        bulletsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.List<GameModel.Bullet> list = (java.util.List<GameModel.Bullet>) bulletsField.get(m6);

        for (int i = 0; i < lives; i++) {
            // place a bullet exactly at the player's position so update() registers a hit
            GameModel.Bullet b = new GameModel.Bullet(m6.getPlayerX(), m6.getPlayerY(), 0.0, true);
            list.add(b);
            m6.update(0.01);
        }
        boolean pass6 = m6.isGameOver();
        report(6, pass6);
        if (!pass6) failures++;

        System.out.println((failures == 0) ? "ALL TESTS PASS" : (failures + " TESTS FAILED"));
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void report(int n, boolean pass) {
        System.out.println("Test " + n + ": " + (pass ? "PASS" : "FAIL"));
    }
}
