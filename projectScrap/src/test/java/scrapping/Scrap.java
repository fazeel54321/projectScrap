package scrapping;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.annotations.AfterMethod;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Scrap {

    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private static ThreadLocal<String> api = ThreadLocal.withInitial(() -> "https://google-translate113.p.rapidapi.com/api/v1/translator/text");
    private static ThreadLocal<String> apiKey = ThreadLocal.withInitial(() -> "b46c7f55c2mshd762fcd8a759c0ep1b9b9ajsna8c230bb30b7");
    private static ThreadLocal<List<String>> titles = ThreadLocal.withInitial(ArrayList::new);
    private static ThreadLocal<List<String>> translatedTitles = ThreadLocal.withInitial(ArrayList::new);

    @Parameters("os")
    @Test
    public void scrapping(@Optional("Windows") String os) {
        try {
            WebDriver localDriver = setupDriver(os);
            driver.set(localDriver);
            if(os.equalsIgnoreCase("Windows")) {
            	localDriver.manage().window().maximize();
            }
            localDriver.get("https://elpais.com/opinion/");

            WebElement acceptButton = new WebDriverWait(localDriver, Duration.ofSeconds(60)).until(ExpectedConditions.elementToBeClickable(By.cssSelector("button#didomi-notice-agree-button")));
            if (acceptButton != null) {
                acceptButton.click();
            }

            List<WebElement> articles = localDriver.findElements(By.cssSelector("article"));
            List<String> titlesList = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                WebElement article = articles.get(i);
                
                WebElement titleElement = article.findElement(By.tagName("h2"));
                WebElement contentElement = article.findElement(By.tagName("p"));
                
                
                    String title = titleElement.getText().trim();
                    String content = contentElement.getText().trim();
                    
                    
                        titlesList.add(title);
                        System.out.println("Title: " + title);
                   
                        System.out.println("Content: " + content);
                    
               

                WebElement imgElement = null;
                try {
                    imgElement = article.findElement(By.tagName("img"));
                    if (imgElement != null) {
                        String imgUrl = imgElement.getAttribute("src");
                        if (imgUrl != null && !imgUrl.isEmpty()) {
                            saveImage(imgUrl, "article_" + (i + 1) + ".jpg");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("No image found for article " + (i + 1));
                }
            }

            titles.set(titlesList);
            translator("auto", "en");
            analyzeWords();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            quitDriver();
        }
    }

    private WebDriver setupDriver(String os) throws Exception {
        MutableCapabilities capabilities = new MutableCapabilities();
        HashMap<String, Object> bstackOptions = new HashMap<>();

        if (os.equalsIgnoreCase("Windows")) {
            capabilities.setCapability("browserName", "Chrome");
            bstackOptions.put("os", "Windows");
            bstackOptions.put("osVersion", "10");
            bstackOptions.put("browserVersion", "latest");
        } else if (os.equalsIgnoreCase("Android")) {
            capabilities.setCapability("browserName", "Chrome");
            bstackOptions.put("deviceName", "Google Pixel 9");
            bstackOptions.put("osVersion", "15.0");
            bstackOptions.put("interactiveDebugging", true);
        }

        bstackOptions.put("userName", "fazeelahmed_ujYwK3");
        bstackOptions.put("accessKey", "yEBTRX5KRof5cLxdwct8");
        bstackOptions.put("consoleLogs", "info");
        bstackOptions.put("projectName", "Scrapping");
        bstackOptions.put("buildName", "Scrapping");
        capabilities.setCapability("bstack:options", bstackOptions);

        return new RemoteWebDriver(new URL("http://hub.browserstack.com/wd/hub"), capabilities);
    }



  

    public static void saveImage(String imageUrl, String fileName) throws IOException {
        URL url = new URL(imageUrl);
        InputStream in = url.openStream();
        FileOutputStream out = new FileOutputStream(new File("D:\\" + fileName));

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }

        in.close();
        out.close();
        System.out.println("Image saved: " + fileName);
    }

    public static void translator(String sourceLang, String targetLang) throws Exception {
        OkHttpClient client = new OkHttpClient();
        List<String> translated = new ArrayList<>();

        for (String s : titles.get()) {
            String jsonBody = String.format("{\"from\":\"auto\",\"to\":\"%s\",\"text\":\"%s\"}", targetLang, s);

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, jsonBody);

            Request request = new Request.Builder()
                    .url(api.get())
                    .post(body)
                    .addHeader("x-rapidapi-key", apiKey.get())
                    .addHeader("x-rapidapi-host", "google-translate113.p.rapidapi.com")
                    .addHeader("Content-Type", "application/json")
                    .build();

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                throw new Exception("Request Failed: " + response);
            }

            String responseBody = response.body().string();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String responseReceived = jsonNode.get("trans").asText();
            System.out.println("Translated Title: " + responseReceived);
            translated.add(responseReceived);
        }
        translatedTitles.set(translated);
    }

    public static void analyzeWords() {
        List<String> translated = translatedTitles.get();

        if (translated == null || translated.isEmpty()) {
            System.out.println("No translated titles found!");
            return;
        }

        Map<String, Integer> wordCount = new HashMap<>();

        System.out.println("Analyzing words from translated titles...");

        for (String title : translated) {
            String[] words = title.toLowerCase().split("[^a-zA-Z]+"); // Improved regex

            for (String word : words) {
                if (!word.isEmpty() && word.length() > 1) {  // Ignore single-letter words
                    wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
                }
            }
        }

        System.out.println(wordCount.toString());

    }

    @AfterMethod
    public void quitDriver() {
        if (driver.get() != null) {
            driver.get().quit();
            driver.remove();
        }
    }
}
