package com.jimmyadelaja;

import io.javalin.Javalin;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.sikuli.script.Button;
import org.sikuli.script.FindFailed;
import org.sikuli.script.Key;
import org.sikuli.script.Location;
import org.sikuli.script.Match;
import org.sikuli.script.Mouse;
import org.sikuli.script.Pattern;
import org.sikuli.script.Region;
import org.sikuli.script.Screen;

/**
 * REST Server wrapper around SikuliX automation scripts.
 *
 * <p>
 * This application spins up an embedded Javalin server on port 7000 to process
 * inbound HTTP
 * endpoints, translating incoming parameters into low-level screen automation
 * gestures.
 */
public class SikuliServer {

  /**
   * Screen abstraction instance representing the primary desktop monitor display.
   */
  private static final Screen screen = new Screen();

  /**
   * Main entry point to launch the Javalin server and attach API endpoints.
   *
   * @param args command-line arguments (not utilized)
   */
  @SuppressWarnings("unchecked")
  public static void main(String[] args) {
    int waitIntervalMillis = 50;
    Javalin.create(
        config -> {

          // --- Basic UI Actions ---
          config.routes.post(
              "/click-coords",
              ctx -> {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                int x = ((Number) body.get("x")).intValue();
                int y = ((Number) body.get("y")).intValue();
                screen.click(new Location(x, y));
                ctx.result("Clicked at " + x + ", " + y);
              });

          config.routes.post(
              "/type",
              ctx -> {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                String text = (String) body.get("text");
                screen.type("a", Key.CTRL);
                screen.type(text);
                ctx.result("Typed: " + text);
              });

          config.routes.post(
              "/clear",
              ctx -> {
                screen.type("a", Key.CTRL);
                screen.type(Key.BACKSPACE);
                ctx.result("Cleared");
              });

          config.routes.post(
              "/click",
              ctx -> {
                screen.click();
                ctx.result("Click");
              });

          config.routes.post(
              "/mouse-move-coords",
              ctx -> {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                int dx = ((Number) body.get("dx")).intValue();
                int dy = ((Number) body.get("dy")).intValue();
                screen.mouseMove(dx, dy);
                ctx.result("Clicked at " + dx + ", " + dy);
              });

          config.routes.post(
              "/click-image",
              ctx -> {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                String base64Image = (String) body.get("base64Image");
                int index = (int) body.get("index");
                float weight = (float) ((double) body.get("weight"));
                File tempFile = base64ToFile(base64Image);
                Pattern imagePattern = new Pattern(tempFile.getAbsolutePath()).similar(weight);

                // Auto wait for 5 seconds
                for (int wait = 0; getRegionFromBody(body).findAllList(imagePattern).isEmpty()
                    && wait < 100; wait++) {
                  Thread.sleep(waitIntervalMillis);
                }

                if (getRegionFromBody(body).findAllList(imagePattern).isEmpty()) {
                  ctx.status(404).result("Image not found on screen");
                } else {
                  screen.click(
                      getRegionFromBody(body).findAllList(imagePattern).get(index));
                  ctx.result("Image clicked successfully");
                }

                tempFile.delete();
              });

          config.routes.post(
              "/exists",
              ctx -> {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                String base64Image = (String) body.get("base64Image");
                float weight = (float) ((double) body.get("weight"));
                File tempFile = base64ToFile(base64Image);
                Pattern imagePattern = new Pattern(tempFile.getAbsolutePath()).similar(weight);
                boolean val = !getRegionFromBody(body).findAllList(imagePattern).isEmpty();
                System.out.printf("Existing %s\n", val);
                ctx.result(String.valueOf(val));
                tempFile.delete();
              });

          config.routes.get(
              "/capture",
              ctx -> {
                try {
                  BufferedImage image = screen.capture().getImage();
                  ByteArrayOutputStream baos = new ByteArrayOutputStream();
                  ImageIO.write(image, "png", baos);
                  String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                  ctx.result("data:image/png;base64," + base64);
                } catch (IOException e) {
                  ctx.status(500).result("Capture failed: " + e.getMessage());
                }
              });

          config.routes.post(
              "/double-click-coords",
              ctx -> {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                int x = ((Number) body.get("x")).intValue();
                int y = ((Number) body.get("y")).intValue();
                screen.doubleClick(new Location(x, y));
                ctx.result("Double-clicked at " + x + ", " + y);
              });

          config.routes.post(
              "/right-click-coords",
              ctx -> {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                int x = ((Number) body.get("x")).intValue();
                int y = ((Number) body.get("y")).intValue();
                screen.rightClick(new Location(x, y));
                ctx.result("Right-clicked at " + x + ", " + y);
              });

          config.routes.post(
              "/drag-drop",
              ctx -> {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                int fromX = ((Number) body.get("fromX")).intValue();
                int fromY = ((Number) body.get("fromY")).intValue();
                int toX = ((Number) body.get("toX")).intValue();
                int toY = ((Number) body.get("toY")).intValue();
                screen.dragDrop(new Location(fromX, fromY), new Location(toX, toY));
                ctx.result(
                    "Dragged from ("
                        + fromX
                        + ","
                        + fromY
                        + ") to ("
                        + toX
                        + ","
                        + toY
                        + ")");
              });

          config.routes.post(
              "/exists-text",
              ctx -> {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                String text = (String) body.get("text");
                Match match = findText(text, 0, getRegionFromBody(body));
                ctx.result(String.valueOf(match != null));
              });

          config.routes.post(
              "/find-text-click",
              ctx -> {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                String text = (String) body.get("text");
                int index = (int) body.get("index");
                Match match = findText(text, index, getRegionFromBody(body));

                // Auto wait for 5 seconds
                for (int wait = 0; match == null && wait < 100; wait++) {
                  Thread.sleep(waitIntervalMillis);
                  match = findText(text, index, getRegionFromBody(body));
                }

                if (match == null) {
                  ctx.status(404).result("Text '%s' not found on screen".formatted(text));
                  return;
                }
                screen.click(match);
                ctx.result("Text '" + text + "' found and clicked");
              });

          config.routes.post(
              "/hover-coords",
              ctx -> {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                int x = ((Number) body.get("x")).intValue();
                int y = ((Number) body.get("y")).intValue();
                screen.hover(new Location(x, y));
                ctx.result("Hovered at " + x + ", " + y);
              });

          config.routes.post(
              "/mouse-move",
              ctx -> {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                String fromBase64 = (String) body.get("fromBase64");
                String toBase64 = (String) body.get("toBase64");
                File fromTemp = base64ToFile(fromBase64);
                File toTemp = base64ToFile(toBase64);
                try {
                  Match fromMatch = screen.find(fromTemp.getAbsolutePath());
                  Match toMatch = screen.find(toTemp.getAbsolutePath());
                  Location fromLoc = fromMatch.getCenter();
                  Location toLoc = toMatch.getCenter();
                  screen.mouseMove(fromLoc.x, fromLoc.y);
                  screen.mouseMove(toLoc.x, toLoc.y);
                  ctx.result("Mouse moved from " + fromLoc + " to " + toLoc);
                } catch (FindFailed e) {
                  ctx.status(404).result("Image not found: " + e.getMessage());
                } finally {
                  fromTemp.delete();
                  toTemp.delete();
                }
              });

          config.routes.post(
              "/mouse-up",
              ctx -> {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                String button = (String) body.get("button"); // e.g., "left", "right", "middle"
                int buttonCode = button.equals("left")
                    ? Mouse.LEFT
                    : button.equals("right") ? Mouse.RIGHT : Mouse.MIDDLE;
                screen.mouseUp(buttonCode);
                ctx.result("Mouse " + button + " button up");
              });

          config.routes.post(
              "/mouse-down",
              ctx -> {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                String button = (String) body.get("button"); // e.g., "left", "right", "middle"
                int buttonCode = button.equals("left")
                    ? Mouse.LEFT
                    : button.equals("right") ? Mouse.RIGHT : Mouse.MIDDLE;
                screen.mouseDown(buttonCode);
                ctx.result("Mouse " + button + " button down");
              });

          config.routes.post(
              "/find-text-coords",
              ctx -> {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                String text = (String) body.get("text");
                int index = (int) body.get("index");
                Match match = findText(text, index, getRegionFromBody(body));

                // Auto wait for 5 seconds
                for (int wait = 0; match == null && wait < 100; wait++) {
                  Thread.sleep(waitIntervalMillis);
                  match = findText(text, index, getRegionFromBody(body));
                }

                if (match == null) {
                  ctx.status(404).result("Text '%s' not found on screen".formatted(text));
                  return;
                }

                Map<String, Integer> coords = new java.util.HashMap<>();
                coords.put("x", match.getX());
                coords.put("y", match.getY());
                coords.put("w", match.getW());
                coords.put("h", match.getH());
                ctx.json(coords);
              });

          config.routes.post(
              "/find-image-coords",
              ctx -> {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                String base64Image = (String) body.get("base64Image");
                int index = (int) body.get("index");
                float weight = (float) ((double) body.get("weight"));
                File tempFile = base64ToFile(base64Image);
                Pattern imagePattern = new Pattern(tempFile.getAbsolutePath()).similar(weight);

                // Auto wait for 5 seconds
                for (int wait = 0; getRegionFromBody(body).findAllList(imagePattern).isEmpty()
                    && wait < 100; wait++) {
                  Thread.sleep(waitIntervalMillis);
                }

                if (getRegionFromBody(body).findAllList(imagePattern).isEmpty()) {
                  ctx.status(404).result("Image not found on screen");
                  return;
                }

                Match match = getRegionFromBody(body).findAllList(imagePattern).get(index);
                Map<String, Integer> coords = new java.util.HashMap<>();
                coords.put("x", match.getX());
                coords.put("y", match.getY());
                coords.put("w", match.getW());
                coords.put("h", match.getH());
                tempFile.delete();
                ctx.json(coords);
              });

          config.routes.post(
              "/scroll",
              ctx -> {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                int dx = ((Number) body.get("dx")).intValue();
                int dy = ((Number) body.get("dy")).intValue();
                if (dy != 0) {
                  screen.wheel(dy > 0 ? Button.WHEEL_DOWN : Button.WHEEL_UP, Math.abs(dy));
                }
                if (dx != 0) {
                  screen.keyDown(Key.SHIFT);
                  screen.wheel(dy > 0 ? Button.WHEEL_DOWN : Button.WHEEL_UP, Math.abs(dy));
                  screen.keyUp(Key.SHIFT);
                }
                ctx.result("Scrolled dx=" + dx + ", dy=" + dy);
              });
        })
        .start(7000);
  }

  private static File base64ToFile(String base64) throws Exception {
    // Remove header if present (e.g., "data:image/png;base64,")
    String cleanBase64 = base64.contains(",") ? base64.split(",")[1] : base64;
    byte[] bytes = Base64.getDecoder().decode(cleanBase64);
    File tempFile = File.createTempFile("sikuli-" + UUID.randomUUID(), ".png");
    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
      fos.write(bytes);
    }
    return tempFile;
  }

  private static Region getRegionFromBody(Map<String, Object> body) {
    int recX = (int) body.get("recX");
    int recY = (int) body.get("recY");
    int recW = (int) body.get("recW");
    int recH = (int) body.get("recH");
    System.out.printf("Region: %s, %s, %s, %s\n", recX, recY, recW, recH);
    return screen.newRegion(recX, recY, recW == 0 ? screen.w : recW, recH == 0 ? screen.h : recH);
  }

  private static final int X_WORD_TOLERANCE = 60;
  private static final int Y_WORD_TOLERANCE = 8;
  private static final int MAX_EDIT_DISTANCE = 1;

  /**
   * Locates {@code text} in {@code region} by first reading OCR once via {@link Region#text()},
   * fuzzy-matching the query against that string, then resolving the on-screen position of the
   * first word in each match (used as the click target). Spatial proximity between words is not
   * checked.
   */
  private static Match findText(String text, int index, Region region) {
    if (text == null || text.isBlank() || index < 0) {
      return null;
    }

    String ocrText = region.text();
    if (ocrText == null || ocrText.isBlank()) {
      return null;
    }

    String[] queryWords = text.trim().split("\\s+");
    String[] ocrWords = ocrText.trim().split("\\s+");
    List<String[]> matchedSequences = findFuzzySequences(queryWords, ocrWords);
    if (matchedSequences.isEmpty()) {
      return null;
    }

    Map<String, List<Match>> wordLocations = new HashMap<>();
    List<Match> phraseMatches = new ArrayList<>();

    for (String[] sequence : matchedSequences) {
      String firstWord = sequence[0];
      List<Match> locations =
          wordLocations.computeIfAbsent(firstWord, word -> locateWord(region, word));
      phraseMatches.addAll(locations);
    }

    List<Match> uniqueMatches = dedupeMatchesByPosition(phraseMatches);
    if (index >= uniqueMatches.size()) {
      return null;
    }
    return uniqueMatches.get(index);
  }

  /**
   * Finds consecutive OCR-word windows that fuzzy-match {@code queryWords} (exact containment when
   * edit distance is 0; OCR typos accepted up to {@link #MAX_EDIT_DISTANCE}).
   */
  private static List<String[]> findFuzzySequences(String[] queryWords, String[] ocrWords) {
    List<String[]> sequences = new ArrayList<>();
    if (queryWords.length == 0 || queryWords.length > ocrWords.length) {
      return sequences;
    }

    for (int start = 0; start <= ocrWords.length - queryWords.length; start++) {
      String[] captured = new String[queryWords.length];
      boolean matched = true;
      for (int i = 0; i < queryWords.length; i++) {
        String queryWord = normalizeWord(queryWords[i]);
        String ocrWord = normalizeWord(ocrWords[start + i]);
        if (queryWord.isEmpty()
            || ocrWord.isEmpty()
            || editDistance(queryWord, ocrWord) > MAX_EDIT_DISTANCE) {
          matched = false;
          break;
        }
        captured[i] = stripEdgePunctuation(ocrWords[start + i]);
      }
      if (matched) {
        sequences.add(captured);
      }
    }
    return sequences;
  }

  /** Returns on-screen matches for a single OCR word form. */
  private static List<Match> locateWord(Region region, String word) {
    if (word == null || word.isBlank()) {
      return List.of();
    }
    List<Match> found = region.findWords(word);
    if (found == null || found.isEmpty()) {
      return List.of();
    }
    return dedupeMatchesByPosition(found);
  }

  /**
   * Walks subsequent word locations left-to-right from {@code first}, requiring each next word to
   * land within {@link #X_WORD_TOLERANCE} / {@link #Y_WORD_TOLERANCE}.
   */
  private static boolean areWordsSpatiallyChained(Match first, List<List<Match>> locationsPerWord) {
    int xExpected = first.getX() + first.getW() + 2;
    int lineY = first.getY();

    for (int i = 1; i < locationsPerWord.size(); i++) {
      Match nearest = null;
      int nearestDelta = Integer.MAX_VALUE;
      for (Match next : locationsPerWord.get(i)) {
        boolean withinX =
            next.getX() >= xExpected - X_WORD_TOLERANCE
                && next.getX() <= xExpected + X_WORD_TOLERANCE;
        boolean withinY =
            next.getY() >= lineY - Y_WORD_TOLERANCE && next.getY() <= lineY + Y_WORD_TOLERANCE;
        if (!withinX || !withinY) {
          continue;
        }
        int delta = Math.abs(next.getX() - xExpected) + Math.abs(next.getY() - lineY);
        if (delta < nearestDelta) {
          nearestDelta = delta;
          nearest = next;
        }
      }
      if (nearest == null) {
        return false;
      }
      xExpected = nearest.getX() + nearest.getW() + 2;
    }
    return true;
  }

  /** Lowercases and strips leading/trailing punctuation for fuzzy comparisons. */
  private static String normalizeWord(String word) {
    return stripEdgePunctuation(word).toLowerCase();
  }

  private static String stripEdgePunctuation(String word) {
    String stripped = word.replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$", "");
    return stripped.isEmpty() ? word : stripped;
  }

  /** Drops duplicate matches that share the same screen rectangle. */
  private static List<Match> dedupeMatchesByPosition(List<Match> matches) {
    List<Match> unique = new ArrayList<>();
    for (Match match : matches) {
      boolean alreadyPresent = false;
      for (Match existing : unique) {
        if (existing.getX() == match.getX()
            && existing.getY() == match.getY()
            && existing.getW() == match.getW()
            && existing.getH() == match.getH()) {
          alreadyPresent = true;
          break;
        }
      }
      if (!alreadyPresent) {
        unique.add(match);
      }
    }
    return unique;
  }

  /** Levenshtein edit distance between two strings. */
  private static int editDistance(String a, String b) {
    int[][] dp = new int[a.length() + 1][b.length() + 1];
    for (int i = 0; i <= a.length(); i++) {
      dp[i][0] = i;
    }
    for (int j = 0; j <= b.length(); j++) {
      dp[0][j] = j;
    }
    for (int i = 1; i <= a.length(); i++) {
      for (int j = 1; j <= b.length(); j++) {
        int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
        dp[i][j] =
            Math.min(
                Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
      }
    }
    return dp[a.length()][b.length()];
  }
}
