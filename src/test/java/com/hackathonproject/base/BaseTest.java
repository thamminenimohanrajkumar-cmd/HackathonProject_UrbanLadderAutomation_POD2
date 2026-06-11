package com.hackathonproject.base;

import com.hackathonproject.utils.ConfigReader;
import com.hackathonproject.utils.ScreenshotUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.time.Duration;

public class BaseTest {

    private static final ThreadLocal<WebDriver> threadDriver = new ThreadLocal<>();

    protected WebDriverWait wait;
    protected Logger log = LogManager.getLogger(this.getClass());

    public static WebDriver getDriver() {
        return threadDriver.get();
    }

    @BeforeTest
    @Parameters("browser")
    public void setUp(@Optional("chrome") String browser) {

        log.info("===== Opening browser: " + browser + " =====");

        // Selenium Manager (built into Selenium 4.6+) auto-resolves the driver
        // binary for the installed browser. No driver paths or WebDriverManager needed.
        WebDriver driver = switch (browser.toLowerCase()) {
            case "edge"    -> new EdgeDriver(buildEdgeOptions());
            case "firefox" -> new FirefoxDriver(buildFirefoxOptions());
            default        -> new ChromeDriver(buildChromeOptions());
        };

        threadDriver.set(driver);
        wait = new WebDriverWait(driver, Duration.ofSeconds(ConfigReader.getExplicitWait()));

        driver.get(ConfigReader.getBaseURL());
        log.info("Navigated to: " + ConfigReader.getBaseURL());
    }

    @AfterMethod(alwaysRun = true)
    public void afterEachTest(ITestResult result) {
        WebDriver driver = getDriver();
        if (driver == null) return;

        String status = result.isSuccess() ? "PASS" : "FAIL";
        String screenshotName = result.getMethod().getMethodName() + "_" + status;
        String path = ScreenshotUtil.takeScreenshot(driver, screenshotName);
        log.info("Screenshot saved: " + path);

        try {
            driver.get(ConfigReader.getBaseURL());
            log.info("Navigated back to homepage.");
        } catch (Exception e) {
            log.warn("Could not navigate to homepage: " + e.getMessage());
        }
    }

    @AfterTest(alwaysRun = true)
    public void tearDown() {
        WebDriver driver = getDriver();
        if (driver != null) {
            log.info("===== Closing browser =====");
            driver.quit();
            threadDriver.remove();
        }
    }

    /* ---------------- Browser options helpers ---------------- */

    private ChromeOptions buildChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1280,800");
        options.addArguments("--window-position=0,0");
        applyCommonArguments(options);
        return options;
    }

    private EdgeOptions buildEdgeOptions() {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--window-size=1280,800");
        applyCommonArguments(options);
        return options;
    }

    private FirefoxOptions buildFirefoxOptions() {
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--width=1280");
        options.addArguments("--height=800");
        return options;
    }

    /**
     * Chromium-family flags shared by Chrome and Edge.
     */
    private void applyCommonArguments(org.openqa.selenium.chromium.ChromiumOptions<?> options) {
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
    }
}