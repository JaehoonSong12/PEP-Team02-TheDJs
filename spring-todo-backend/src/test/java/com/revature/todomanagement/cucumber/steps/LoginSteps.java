package com.revature.todomanagement.cucumber.steps;

import com.revature.todomanagement.cucumber.CucumberRunner;
import com.revature.todomanagement.cucumber.poms.LoginPom;
import com.revature.todomanagement.cucumber.poms.RegistrationPom;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class LoginSteps {

    private static final String BASE_URL = "http://localhost:4200";

    private LoginPom loginPom;
    private String testUsername;
    private String testPassword;

    private WebDriver getDriver() {
        return CucumberRunner.driver;
    }

    /**
     * Registers a test user via the UI by navigating to the registration page,
     * filling in credentials, and waiting for redirect back to the login page.
     */
    private void registerTestUserViaUI() {
        WebDriver driver = getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        testUsername = "u" + (System.currentTimeMillis() % 100000000000L);
        testPassword = "TestPass1!";

        // Step 1: Navigate to login page
        driver.get(BASE_URL + "/login");

        // Step 2: Click the "Register" link to go to the registration page
        driver.findElement(By.linkText("Register")).click();

        // Step 3: Wait for URL to contain /register
        wait.until(ExpectedConditions.urlContains("/register"));

        // Step 4: Create RegistrationPom and enter credentials (fills username, password, confirmPassword)
        RegistrationPom registrationPom = new RegistrationPom(driver);
        registrationPom.enterCredentials(testUsername, testPassword);

        // Step 5: Click the submit button to register
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Step 6: Wait for redirect back to /login
        wait.until(ExpectedConditions.urlContains("/login"));
    }

    @When("The user enters valid login credentials")
    public void the_user_enters_valid_login_credentials() {
        // Register a test user first so we have valid credentials
        registerTestUserViaUI();

        // Now enter those credentials into the login form
        loginPom = new LoginPom(getDriver());
        loginPom.enterCredentials(testUsername, testPassword);
    }

    @When("The user enters invalid login credentials")
    public void the_user_enters_invalid_login_credentials() {
        loginPom = new LoginPom(getDriver());
        loginPom.enterCredentials("nonexistentuser" + System.currentTimeMillis(), "BadPass1!");
    }

    @When("The user clicks login button")
    public void the_user_clicks_login_button() {
        loginPom.clickSubmitButton();
    }

    @Then("The user should be redirected to the dashboard page")
    public void the_user_should_be_redirected_to_the_dashboard_page() {
        WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(5));
        wait.until(ExpectedConditions.urlContains("/dashboard"));
        assertTrue(getDriver().getCurrentUrl().contains("/dashboard"));
    }

    @Then("The user should remain on the login page")
    public void the_user_should_remain_on_the_login_page() {
        assertTrue(getDriver().getCurrentUrl().contains("/login"));
    }
}
