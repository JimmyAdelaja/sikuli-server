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
 * <p>This application spins up an embedded Javalin server on port 7000 to process inbound HTTP
 * endpoints, translating incoming parameters into low-level screen automation gestures.
 */
public class SikuliServer {

  /** Screen abstraction instance representing the primary desktop monitor display. */
  private static final Screen screen = new Screen();

  /**
   * Main entry point to launch the Javalin server and attach API endpoints.
   *
   * @param args command-line arguments (not utilized)
   */
  @SuppressWarnings("unchecked")
  public static void main(String[] args) {
    Javalin app =
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
                        try {
                          Pattern imagePattern =
                              new Pattern(tempFile.getAbsolutePath()).similar(weight);
                          screen.click(
                              getRegionFromBody(body).findAllList(imagePattern).get(index));
                          ctx.result("Image clicked successfully");
                        } catch (FindFailed e) {
                          System.out.println("FindFailed: " + e.getMessage());
                          ctx.status(404).result("Image not found on screen");
                        } finally {
                          tempFile.delete();
                        }
                      });

                  config.routes.post(
                      "/exists",
                      ctx -> {
                        Map<String, Object> body = ctx.bodyAsClass(Map.class);
                        String base64Image = (String) body.get("base64Image");
                        float weight = (float) ((double) body.get("weight"));
                        File tempFile = base64ToFile(base64Image);
                        Pattern imagePattern =
                            new Pattern(tempFile.getAbsolutePath()).similar(weight);
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
                        screen.text();
                        Map<String, Object> body = ctx.bodyAsClass(Map.class);
                        String text = (String) body.get("text");
                        Match match = findText(text, 0, getRegionFromBody(body));
                        if (match == null) {
                          ctx.status(404).result("Text not found on screen");
                          return;
                        }
                        ctx.result(String.valueOf(match != null));
                      });

                  config.routes.post(
                      "/find-text-click",
                      ctx -> {
                        screen.text();
                        Map<String, Object> body = ctx.bodyAsClass(Map.class);
                        String text = (String) body.get("text");
                        int index = (int) body.get("index");
                        Match match = findText(text, index, getRegionFromBody(body));
                        if (match == null) {
                          ctx.status(404).result("Text not found on screen");
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
                        String button =
                            (String) body.get("button"); // e.g., "left", "right", "middle"
                        int buttonCode =
                            button.equals("left")
                                ? Mouse.LEFT
                                : button.equals("right") ? Mouse.RIGHT : Mouse.MIDDLE;
                        screen.mouseUp(buttonCode);
                        ctx.result("Mouse " + button + " button up");
                      });

                  config.routes.post(
                      "/mouse-down",
                      ctx -> {
                        Map<String, Object> body = ctx.bodyAsClass(Map.class);
                        String button =
                            (String) body.get("button"); // e.g., "left", "right", "middle"
                        int buttonCode =
                            button.equals("left")
                                ? Mouse.LEFT
                                : button.equals("right") ? Mouse.RIGHT : Mouse.MIDDLE;
                        screen.mouseDown(buttonCode);
                        ctx.result("Mouse " + button + " button down");
                      });

                  config.routes.post(
                      "/find-text-coords",
                      ctx -> {
                        Map<String, Object> body = ctx.bodyAsClass(Map.class);
                        System.out.println(screen.text());
                        String text = (String) body.get("text");
                        int index = (int) body.get("index");
                        Match match = findText(text, index, getRegionFromBody(body));
                        if (match == null) {
                          ctx.status(404).result("Text not found on screen");
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
                        Pattern imagePattern =
                            new Pattern(tempFile.getAbsolutePath()).similar(weight);
                        Match match = getRegionFromBody(body).findAllList(imagePattern).get(index);
                        // Location center = match.getCenter();
                        Map<String, Integer> coords = new java.util.HashMap<>();
                        coords.put("x", match.getX());
                        coords.put("y", match.getY());
                        coords.put("w", match.getW());
                        coords.put("h", match.getH());
                        ctx.json(coords);
                        tempFile.delete();
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

  private static Match findText(String text, int index, Region region) {
    List<Match> matches = new ArrayList<>();
    String[] tokenizedText = text.split(" ");
    if (tokenizedText.length == 1) {
      matches = region.findAllText(tokenizedText[0]);
    } else {
      Map<String, List<Match>> allTokenizedMatches = new HashMap<>();
      for (String token : tokenizedText) {
        allTokenizedMatches.put(token, region.findAllText(token));
      }
      int xWordTolerance = 60;
      int yWordTolerance = 8;

      System.out.printf(
          "[Log]: First matches: %s\n", allTokenizedMatches.get(tokenizedText[0]).size());
      for (Match first : allTokenizedMatches.get(tokenizedText[0])) {
        boolean potentialMatch = true;
        int xExpected = first.getX() + first.getW() + 2;
        for (int i = 1; i < tokenizedText.length; i++) {
          int pMatches = allTokenizedMatches.get(tokenizedText[i]).size();
          System.out.printf(
              "\t[Inner Next Log]: '%s', matches: %s, xExpected: %s, first.getY(): %s\n",
              tokenizedText[i],
              allTokenizedMatches.get(tokenizedText[i]).size(),
              xExpected,
              first.getY());
          for (Match next : allTokenizedMatches.get(tokenizedText[i])) {
            System.out.printf(
                "\t\t[Inner Next Log values]: next.getX(): %s, next.getY(): %s\n",
                next.getX(), next.getY());
            // ensure this next is within the expected x tolerance
            if (next.getX() >= xExpected - xWordTolerance
                && next.getX() <= xExpected + xWordTolerance
                // ensure this next are on the same line
                && next.getY() >= first.getY() - yWordTolerance
                && next.getY() <= first.getY() + yWordTolerance) {
              xExpected = next.getX() + next.getW() + 2;
            } else {
              pMatches--;
            }
          }

          if (pMatches == 0) { // None of the found next words were close
            System.out.printf("\t[Inner Next Log - Final]: no matches within tolerance\n");
            potentialMatch = false;
            break;
          }
        }

        if (potentialMatch) matches.add(first);
      }
    }
    return matches.isEmpty() ? null : matches.get(index);
  }
}
