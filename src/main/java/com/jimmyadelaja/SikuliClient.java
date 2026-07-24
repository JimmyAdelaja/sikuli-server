package com.jimmyadelaja;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Rectangle;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;

/**
 * Client for interacting with a remote SikuliServer instance over HTTP.
 *
 * <p>Provides an interface to execute GUI automation commands such as clicking, typing, capturing
 * screens, and finding text or images on a remote server.
 */
public class SikuliClient {
  private final String baseUrl;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  public static int waitInterval = 500; // In milliseconds

  /**
   * Constructs a new SikuliClient configured to connect to a specific server.
   *
   * @param host the hostname or IP address of the SikuliServer
   * @param port the port number the SikuliServer is listening on
   */
  public SikuliClient(String host, int port) {
    this.baseUrl = "http://" + host + ":" + port;
    this.httpClient = HttpClient.newHttpClient();
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Sends a JSON POST request to the specified endpoint on the server.
   *
   * @param endpoint the target API endpoint (e.g., "/click-coords")
   * @param data a map containing key-value pairs representing the JSON payload
   * @return the raw string response body from the server
   * @throws Exception if the network request fails or the server returns an error code (>= 400)
   */
  private String sendPost(String endpoint, Map<String, Object> data) throws Exception {
    String jsonBody = objectMapper.writeValueAsString(data);
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + endpoint))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() >= 400) {
      throw new RuntimeException(
          "Server error (" + response.statusCode() + "): " + response.body());
    }
    return response.body();
  }

  // --- Basic UI Actions ---

  /**
   * Clicks at the specified absolute pixel coordinates.
   *
   * @param x the target x-coordinate
   * @param y the target y-coordinate
   * @return a server execution confirmation message
   */
  @SneakyThrows
  public String clickCoords(int x, int y) {
    return sendPost("/click-coords", Map.of("x", x, "y", y));
  }

  /**
   * Inputs text by selecting all current content in focus (Ctrl+A) and typing the new string.
   *
   * @param text the sequence of characters to type
   * @return a server execution confirmation message
   */
  @SneakyThrows
  public String type(String text) {
    return sendPost("/type", Map.of("text", text));
  }

  /**
   * Clears text in the currently focused UI element by simulating Ctrl+A followed by Backspace.
   *
   * @return a server execution confirmation message
   */
  @SneakyThrows
  public String clear() {
    return sendPost("/clear", Map.of());
  }

  /**
   * Simulates a left-click at the mouse pointer's current location.
   *
   * @return a server execution confirmation message
   */
  @SneakyThrows
  public String click() {
    return sendPost("/click", Map.of());
  }

  /**
   * Moves the mouse pointer by a relative offset.
   *
   * @param dx horizontal displacement
   * @param dy vertical displacement
   * @return a server execution confirmation message
   */
  @SneakyThrows
  public String moveMouseCoords(int dx, int dy) {
    return sendPost("/mouse-move-coords", Map.of("dx", dx, "dy", dy));
  }

  /**
   * Finds and clicks a visual asset on the screen using a Base64-encoded image snippet.
   *
   * @param req Request Object
   * @return a server execution confirmation message
   */
  @SneakyThrows
  public String clickImage(Req req) {
    return sendPost("/click-image", req.toMap());
  }

  /**
   * Checks whether a visual asset is currently visible on the screen.
   *
   * @param req Request Object
   * @return true if the image matches a region on screen, false otherwise
   */
  @SneakyThrows
  public boolean exists(Req req) {
    String result = sendPost("/exists", req.toMap());
    return Boolean.parseBoolean(result);
  }

  /**
   * Captures the full screen of the host machine.
   *
   * @return a data URI string containing the Base64-encoded PNG screenshot
   */
  @SneakyThrows
  public String capture() {
    HttpRequest request =
        HttpRequest.newBuilder().uri(URI.create(baseUrl + "/capture")).GET().build();
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
  }

  /**
   * Double-clicks at the specified absolute pixel coordinates.
   *
   * @param x the target x-coordinate
   * @param y the target y-coordinate
   * @return a server execution confirmation message
   */
  @SneakyThrows
  public String doubleClickCoords(int x, int y) {
    return sendPost("/double-click-coords", Map.of("x", x, "y", y));
  }

  /**
   * Right-clicks at the specified absolute pixel coordinates.
   *
   * @param x the target x-coordinate
   * @param y the target y-coordinate
   * @return a server execution confirmation message
   */
  @SneakyThrows
  public String rightClickCoords(int x, int y) {
    return sendPost("/right-click-coords", Map.of("x", x, "y", y));
  }

  /**
   * Performs a drag-and-drop mouse gesture from one location to another.
   *
   * @param fromX starting x-coordinate
   * @param fromY starting y-coordinate
   * @param toX destination x-coordinate
   * @param toY destination y-coordinate
   * @return a server execution confirmation message
   */
  @SneakyThrows
  public String dragDrop(int fromX, int fromY, int toX, int toY) {
    return sendPost("/drag-drop", Map.of("fromX", fromX, "fromY", fromY, "toX", toX, "toY", toY));
  }

  // --- Text Actions ---

  /**
   * Checks whether the specified text string is visible anywhere on screen using OCR.
   *
   * @param req Request Object
   * @return true if the text matches an element on screen, false otherwise
   */
  @SneakyThrows
  public boolean existsText(Req req) {
    return Boolean.parseBoolean(sendPost("/exists-text", req.toMap()));
  }

  /**
   * Locates text matches via OCR on screen and clicks a specific match index.
   *
   * @param req Request Object
   * @return a server execution confirmation message
   */
  @SneakyThrows
  public String findTextClick(Req req) {
    return sendPost("/find-text-click", req.toMap());
  }

  // --- Mouse & Wait Actions ---

  /**
   * Hovers the mouse cursor over specific absolute coordinates.
   *
   * @param x target x-coordinate
   * @param y target y-coordinate
   * @return a server execution confirmation message
   */
  @SneakyThrows
  public String hoverCoords(int x, int y) {
    return sendPost("/hover-coords", Map.of("x", x, "y", y));
  }

  /**
   * Moves the mouse linearly from the center of one identified image to the center of another.
   *
   * @param fromBase64 the source image encoded as a Base64 string
   * @param toBase64 the destination image encoded as a Base64 string
   * @return a server execution confirmation message
   */
  @SneakyThrows
  public String mouseMove(String fromBase64, String toBase64) {
    return sendPost("/mouse-move", Map.of("fromBase64", fromBase64, "toBase64", toBase64));
  }

  /**
   * Changes the state of a specific mouse button to pressed or released.
   *
   * @param direction the action to take ("mouse-up" or "mouse-down")
   * @param button the target mouse button ("left", "right", or "middle")
   * @return a server execution confirmation message
   */
  @SneakyThrows
  public String mouseUpDown(String direction, String button) {
    // direction: "mouse-up" or "mouse-down"
    // button: "left", "right", or "middle"
    return sendPost("/" + direction, Map.of("button", button));
  }

  /**
   * Blocks execution until an image matches a section of the screen or a timeout occurs.
   *
   * @param req Request Object
   * @param timeout maximum wait time in seconds (0 means wait indefinitely)
   * @return a server execution confirmation message
   */
  @SneakyThrows
  public void waitImage(Req req, double timeout) {
    double waitTime = 0;
    while (!exists(req)) {
      System.out.printf("[waitImage] Still waiting for image...\n");
      Thread.sleep(waitInterval);
      waitTime += .5;
      if (waitTime > timeout)
        throw new java.util.concurrent.TimeoutException("Timed out waiting for image");
    }
  }

  /**
   * Blocks execution until the given text appears on screen or a timeout occurs.
   *
   * @param req Request Object
   * @param timeout maximum wait time in seconds (0 means wait indefinitely)
   * @return a server execution confirmation message
   */
  @SneakyThrows
  public void waitText(Req req, double timeout) {
    double waitTime = 0;
    while (!existsText(req)) {
      System.out.printf("[waitImage] Still waiting for text...\n");
      Thread.sleep(waitInterval);
      waitTime += .5;
      if (waitTime > timeout)
        throw new java.util.concurrent.TimeoutException(
            "Timed out waiting for text: " + req.getText());
    }
  }

  /**
   * Blocks execution until a specific image disappears from the screen or a timeout occurs.
   *
   * @param req Request Object
   * @param timeout maximum wait time in seconds (0 means wait indefinitely)
   * @return a server execution confirmation message
   */
  @SneakyThrows
  public void waitVanishImage(Req req, double timeout) {
    double waitTime = 0;
    while (exists(req)) {
      Thread.sleep(waitInterval);
      waitTime += .5;
      if (waitTime > timeout)
        throw new java.util.concurrent.TimeoutException("Timed out waiting for image to vanish");
    }
  }

  /**
   * Blocks execution until a given string disappears from the screen or a timeout occurs.
   *
   * @param req Request Object
   * @param timeout maximum wait time in seconds (0 means wait indefinitely)
   * @return a server execution confirmation message
   */
  @SneakyThrows
  public void waitVanishText(Req req, double timeout) {
    double waitTime = 0;
    while (existsText(req)) {
      Thread.sleep(waitInterval);
      waitTime += .5;
      if (waitTime > timeout)
        throw new java.util.concurrent.TimeoutException(
            "Timed out waiting for text: " + req.getText() + " to vanish");
    }
  }

  /**
   * Retrieves the absolute center coordinates of a text string on the screen.
   *
   * @param req Request Object
   * @return a Map containing keys "x" and "y" pointing to their respective pixel positions
   */
  @SneakyThrows
  public Rectangle findTextCoords(Req req) {
    return sendPostForCoords("/find-text-coords", req.toMap());
  }

  /**
   * Retrieves the absolute center coordinates of a visual asset matching the provided image.
   *
   * @param req Request Object
   * @return a Map containing keys "x" and "y" pointing to their respective pixel positions
   */
  @SneakyThrows
  public Rectangle findImageCoords(Req req) {
    return sendPostForCoords("/find-image-coords", req.toMap());
  }

  /**
   * Simulates mouse wheel scrolling along horizontal or vertical axes.
   *
   * @param dx horizontal scroll ticks/steps
   * @param dy vertical scroll ticks/steps
   * @return a server execution confirmation message
   */
  @SneakyThrows
  public String scroll(int dx, int dy) {
    return sendPost("/scroll", Map.of("dx", dx, "dy", dy));
  }

  /**
   * Helper method that unpacks coordinates from a server POST response into a Map container.
   *
   * @param endpoint target coordinate query endpoint
   * @param data request payload parameters
   * @return a parsed Map containing spatial mappings for "x" and "y"
   */
  @SneakyThrows
  private Rectangle sendPostForCoords(String endpoint, Map<String, Object> data) {
    String response = sendPost(endpoint, data);
    Map<String, Integer> coordsMap =
        objectMapper.readValue(
            response,
            objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Integer.class));

    return new Rectangle(
        coordsMap.get("x"), coordsMap.get("y"), coordsMap.get("w"), coordsMap.get("h"));
  }

  /**
   * Finds and clicks a visual asset on the screen using a Base64-encoded image snippet.
   *
   * @param base64Image the target image encoded as a Base64 string
   * @param text the target text to locate
   * @param index zero-based index targeting which match instance to click
   * @param rec the rectangle for a region
   * @return a server execution confirmation message
   */
  @Builder
  @Getter
  public static class Req {
    @Builder.Default private final String base64Image = "";
    @Builder.Default private final String text = "";
    @Builder.Default private final float weight = 0.7f;
    @Builder.Default private final int index = 0;
    @Builder.Default private final Rectangle rec = new Rectangle();

    private Map<String, Object> toMap() {
      return Map.of(
          "base64Image",
          this.base64Image,
          "text",
          this.text,
          "index",
          this.index,
          "weight",
          this.weight,
          "recX",
          this.rec.x,
          "recY",
          this.rec.y,
          "recW",
          this.rec.width,
          "recH",
          this.rec.height);
    }
  }
}
